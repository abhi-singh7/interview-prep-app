## Why

The analytics page displays incorrect score data to users: category-wise breakdown silently drops questions with multiple categories, session averages ignore theory question performance, all three AVG calculations include stale retry evaluation records, and the frontend session history table shows a per-question average as if it were a total. These bugs undermine user trust in their progress tracking — scores shown don't actually reflect how well they performed.

## What Changes

- **Session-level score query**: Remove `q.type = 'CODE'` filter so ALL question types (THEORY + CODE) contribute to the per-session average and category name aggregation.
- **Category-wise breakdown query**: Replace simple equality join with `string_to_array + unnest + EXISTS` pattern (matching session-level query) so multi-category questions are included in the breakdown.
- **Latest-evaluation-only filtering**: Add a subquery or window function to all three AVG calculations (global avg, category breakdown, session avg) so only the latest evaluation per answer contributes — stale retry records no longer pollute averages.
- **Frontend score display**: Fix `session.score` numerator in the session history table from average-per-question to total score (`avg × questionCount`).

## Capabilities

### Modified Capabilities
- `analytics-category-display`: Requirements around category names rendering remain; new requirement added that analytics scores must reflect only latest evaluations and all question types. Delta spec file required.

## Impact

- **Backend**: `AnalyticsService.java` — three SQL queries modified (global avg, category breakdown, session-level).
- **Frontend**: `analytics-page.component.ts` template line 87 — score numerator calculation.
- **Data integrity**: No schema changes; fix is purely query-filtering and display logic.
- **Backward compatibility**: Existing data unaffected. Only the aggregation window changes at read time (latest evaluation per answer).
