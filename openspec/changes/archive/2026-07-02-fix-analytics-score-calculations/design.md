## Context

`AnalyticsService.getPerformanceData()` runs three SQL queries against the analytics data model:
1. Global average score — `AVG(e.score)` over all evaluations for completed sessions (line 29-32)
2. Category-wise breakdown — per-category AVG from JOIN chain evaluations → answers → questions → sessions (line 39-50)
3. Session-level summary — per-session AVG across multiple LEFT JOINs including a `q.type = 'CODE'` filter, plus `string_to_array + unnest` for multi-category resolution (line 64-77)

Meanwhile `EvaluationController.retryEvaluation()` creates new evaluation records via `entityManager.persist(newEvaluation)` on retry instead of updating the existing one. This means stale failed evaluations remain in the database alongside fresh success evaluations, but no query filters to "latest per answer". The frontend template displays session.score (a per-question average) as a numerator against `(questionCount * 10)` denominator.

## Goals / Non-Goals

**Goals:**
- All three AVG calculations consider only the latest evaluation per answer.
- Session-level score includes ALL question types (THEORY + CODE).
- Category-wise breakdown joins correctly on comma-separated `category_id` values using `string_to_array + unnest`.
- Frontend session history table shows total score (`avg × count`) instead of average alone.

**Non-Goals:**
- No schema changes — fix is read-time filtering only.
- No changes to evaluation creation/retry flow (keep persist-new, don't delete old).
- No new endpoints or API surface changes.

## Decisions

### Decision 1: Use `DISTINCT ON` subquery instead of window functions for latest-evaluation filtering

**Choice:** Filter evaluations with a derived table using PostgreSQL `DISTINCT ON (answer_id) ... ORDER BY answer_id, id DESC`.

```sql
-- Pattern applied to each AVG query
SELECT c.name AS category_name,
       AVG(sub.score)::float AS avg_score
FROM (
  SELECT DISTINCT ON (answer_id) id, score, answer_id
  FROM evaluations
  WHERE score IS NOT NULL
  ORDER BY answer_id, id DESC
) latest_e
JOIN answers a ON a.id = latest_e.answer_id
...
```

**Alternatives considered:**
| Approach | Pros | Cons |
|----------|------|------|
| `DISTINCT ON` subquery | Simple, standard PostgreSQL, indexes on (answer_id) help | Requires re-joining to other tables |
| Window function `ROW_NUMBER()` | Flexible, single pass | Slightly more verbose SQL |
| Mark old evaluations as superseded in DB | Cleanest model | Requires write-side change to retry flow; not in scope |

`DISTINCT ON` is chosen for simplicity and because the evaluation table already has an index-friendly access pattern (queries filter by answer/session relationships).

### Decision 2: Remove `q.type = 'CODE'` from session-level LEFT JOIN

**Choice:** Join ALL questions regardless of type, then aggregate across all answers/evaluations.

This is a one-line removal:
```sql
-- Before
LEFT JOIN questions q ON q.session_id = s.id AND q.type = 'CODE'
-- After
LEFT JOIN questions q ON q.session_id = s.id
```

The `string_to_array + unnest` multi-category resolution already works for all question types, so removing the type filter is safe. The GROUP BY on session-level columns ensures no cartesian blow-up from joining multiple questions per session.

### Decision 3: Apply same `string_to_array + EXISTS` pattern to category-wise breakdown

**Choice:** Replace simple equality join with multi-value-aware join matching the session-level query's approach.

```sql
-- Before (line 44)
LEFT JOIN categories c ON q.category_id::text = c.id::text
-- After
LEFT JOIN categories c ON EXISTS (
  SELECT 1 FROM unnest(string_to_array(nullif(trim(q.category_id), ''), ',')::bigint[]) AS x(id) WHERE x.id = c.id
)
```

**Why not a lateral join?** A `CROSS JOIN LATERAL unnest(...)` would produce multiple rows per question (one per category), which could inflate the AVG if not aggregated carefully. The EXISTS approach preserves one-row-per-question semantics while correctly matching categories — identical to the session-level query's proven pattern.

### Decision 4: Frontend score numerator = `avg × count` instead of raw avg

**Choice:** Multiply the backend-returned average by questionCount client-side before rendering.

```html
<!-- Before -->
{{ session.score }}/{{ (session.questionCount || 0) * 10 }}
<!-- After -->
{{ (session.score * (session.questionCount || 0)) }}/{{ (session.questionCount || 0) * 10 }}
```

**Alternatives considered:**
| Approach | Pros | Cons |
|----------|------|------|
| Backend returns totalScore field | Single source of truth, no client math | New response shape change; minimal benefit vs client-side multiply |
| Client-side multiply (chosen) | No backend change, simple template fix | Requires understanding that score is avg not total |

Client-side multiply chosen for minimal scope — one template line.

## Risks / Trade-offs

- **[Risk]** `DISTINCT ON` subquery adds a derived-table join in every query → slight performance cost on large datasets.
  [Mitigation] The evaluations table is small per user (one per answered question, retries are rare). Index `(answer_id)` already exists via the FK constraint. Negligible impact.

- **[Risk]** Removing `q.type = 'CODE'` could inflate session rows if a session has many questions with multiple answers — but GROUP BY on `s.id` keeps one row per session.
  [Mitigation] The existing query already joins multiple question rows per session (one CODE question → multiple joined rows); removing the type filter just adds THEORY rows, which is the intended behavior.

- **[Risk]** Multi-category questions now appear in breakdown under each category — their score contributes to every matching category's average.
  [Mitigation] This is correct behavior: a question tagged "Core Java + Streams" should contribute to both categories' performance tracking. The session-level query uses identical logic (string_to_array), so this is consistent across analytics views.

- **[Risk]** Frontend change exposes that `session.score` was an average, not total — existing users may notice the displayed numbers shift.
  [Mitigation] Not a breaking API change (response shape unchanged). The new display is numerically correct and matches user expectation of "score / possible".

## Migration Plan

No migration needed. All changes are query-only or template-only:
1. Deploy backend fix (`AnalyticsService.java` SQL queries) — existing data unaffected, read-time filter change takes effect immediately.
2. Deploy frontend fix (`analytics-page.component.ts`) — no API contract change, safe to deploy independently.
3. Rollback: revert the two files; no schema or seed-data changes to undo.

## Open Questions

None identified. The evaluation retry flow (persist-new, don't delete) is preserved — if future work wants a cleaner model where only one evaluation per answer exists, that's a separate change.
