## 1. Backend — Inject dependency and resolve category IDs in question creation

- [x] 1.1 Add `InterviewSetupService` as a constructor-injected field of `QuestionGenerationService`, update constructor signature to accept it alongside existing dependencies
- [x] 1.2 In the question persistence loop, replace the unconditional `q.setCategoryId(null)` block with: call `interviewSetupService.resolveTopicNamesToIds(record.topics())` when topics are non-null/non-empty, and assign the returned string to `q.setCategoryId(...)`; keep null assignment for empty/null topics
- [x] 1.3 Verify backend compiles cleanly: `cd backend && mvn compile -DskipTests`

## 2. Frontend — Fix analytics template binding

- [x] 2.1 In `frontend/src/app/analytics/analytics-page/analytics-page.component.ts`, change the Category column cell binding from `{{ session.categoryId || 'N/A' }}` to `{{ session.categoryName || 'N/A' }}`
- [x] 2.2 Verify frontend builds: `cd frontend && npm run build`

## 3. Verification

- [x] 3.1 Run backend tests: `cd backend && mvn test` (pre-existing failures unrelated to this change)
- [x] 3.2 Run frontend lint: `cd frontend && npm run lint`
