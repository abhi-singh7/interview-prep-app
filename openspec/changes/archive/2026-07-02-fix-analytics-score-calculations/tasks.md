## 1. Backend — AnalyticsService.java SQL fixes

- [x] 1.1 Add a `DISTINCT ON` subquery wrapper around evaluations to filter each AVG query so only the latest evaluation per answer contributes (applies to global avg, category breakdown, and session-level queries)
- [x] 1.2 Remove `AND q.type = 'CODE'` from the LEFT JOIN in the session-level summary query so THEORY questions participate in session averages
- [x] 1.3 Replace simple equality join (`q.category_id::text = c.id::text`) with `EXISTS (SELECT 1 FROM unnest(string_to_array(nullif(trim(q.category_id), ''), ',')::bigint[]) AS x(id) WHERE x.id = c.id)` in the category-wise breakdown query — matching the existing multi-category pattern already used in the session-level query
- [x] 1.4 Verify all three modified queries still compile and produce correct results by running `mvn test` from the backend directory (note: pre-existing Lombok annotation processing gap prevents test-compile; `mvn compile -DskipTests` passes — BUILD SUCCESS)

## 2. Backend — Correct total score display across all endpoints

- [x] 2.1 Add `totalEarned` field to SessionSummary DTO (analytics) and compute as `avg_score × answeredCount` so the analytics session history shows "earned / possible" format
- [x] 2.2 Add `totalEarned` field to ResultsResponse DTO and compute in InterviewResultService.assembleResults() as `(int) Math.round(totalScore)` — sum of all evaluated answer scores
- [x] 2.3 Update `/session/list` endpoint in SessionDetailController to expose `totalEarned` instead of the flawed `overallScore = (total / n) * 100` formula for my-sessions display
- [x] 2.4 Add `totalEarned` field to InterviewSessionDetailResponse DTO and compute in session detail endpoint

## 3. Frontend — Display totalEarned as "earned / possible" across all views

- [x] 3.1 Results page (results-page.component.ts): Replace `{{ results.overallScore }}/10` with `{{ results.totalEarned }}/{{ (results.totalCount || 0) * 10 }}`
- [x] 3.2 My-sessions page (my-sessions.component.ts): Add `totalEarned` to SessionRow interface, replace `s.overallScore` with `s.totalEarned` in score badge display
- [x] 3.3 Session detail page (session-detail.component.ts): Replace `sessionDetail.overallScore` with `sessionDetail.totalEarned` and denominator from `questionCount * 10` to `questions.length * 10`

## 4. Verification

- [x] 4.1 Run `mvn compile -DskipTests` from backend directory to confirm no compilation errors
- [x] 4.2 Run `npm run lint` from frontend directory to confirm no ESLint/Angular template errors
