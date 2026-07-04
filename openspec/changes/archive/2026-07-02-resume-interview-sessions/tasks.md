## 1. Database Migration — Fix Orphaned Questions

- [x] 1.1 Create Flyway migration `V4__fix_question_session_fk.sql` that backfills `questions.session_id` from the answer chain: `UPDATE questions q SET session_id = a.session_id FROM answers a WHERE a.question_id = q.id`
- [x] 1.2 Verify migration is idempotent (running twice produces same result) and does not error on empty tables

## 2. Backend — Fix Question FK at Runtime

- [x] 2.1 In `QuestionGenerationService.generateAndPersistQuestions()`, add `q.setSession(session)` inside the question creation loop before persisting
- [x] 2.3 Verify existing `/interview/session/{id}/questions` endpoint now returns questions for real sessions by checking that `questionRepository.findBySessionId()` resolves correctly

## 3. Backend — New Session Detail Endpoint

- [x] 3.1 Create DTO `InterviewSessionDetailResponse` with nested structures: session metadata (languageName, topicNames resolved from IDs, difficulty, startedAt, endedAt), list of questions with answer state per question, and evaluations
- [x] 3.2 Create or extend controller (`SessionDetailController`) with `GET /interview/session/{id}/detail` endpoint that:
  - Fetches session by ID with ownership check (throws 403 if user_id mismatch)
  - Returns 404 for ABANDONED sessions
  - Resolves categoryId to language name via CategoryRepository
  - Resolves topicIdsJson to human-readable topic names
  - Joins questions, answers, and evaluations in a single response (reuses existing InterviewResultService logic for evaluation assembly)
- [x] 3.3 Add unit test for ownership check: authenticated user requesting another user's session detail returns 403

## 4. Frontend — Resume Wiring

- [x] 4.1 Verify `InterviewPageComponent` correctly loads questions from `GET /interview/session/{id}/questions` on resume (this should already work once the FK is fixed)
- [x] 4.2 On resume, sync answered-question state: fetch existing answers for the session and mark them as "answered" in the frontend Maps (`answers`, `codingSubmissions`) so progress indicator reflects true count
- [x] 4.3 Ensure navigation defaults to first unanswered question (or last answered) after resuming — verify existing logic handles this correctly with populated data

## 5. Frontend — Session Detail Page

- [x] 5.1 Add new route `/interview/detail/:sessionId` in `app.routes.ts` pointing to a new `SessionDetailComponent`
- [x] 5.2 Create `SessionDetailComponent` (standalone, OnPush CD) that:
  - Calls `GET /interview/session/{id}/detail` on init
  - Shows session metadata header: language name, topic names, difficulty, question count, dates, overall score
  - Renders a scrollable list of questions with status indicators (answered/evaluated/unanswered)
- [x] 5.3 For each answered question, show user's answer text (THEORY) or code with syntax highlighting via existing `HighlightBlockComponent` (CODE)
- [x] 5.4 Show evaluation details per answered question: score, strengths list, weaknesses list, improved answer, time/space complexity, correctness explanation (reuse existing ResultsPage component patterns where possible)
- [x] 5.5 For evaluations with FAILED status, show "Retry Evaluation" button wired to `POST /evaluation/retry/{answerId}` (existing endpoint), updating UI on success
- [x] 5.6 Add "Retake" button in header that navigates to `/interview/setup` pre-populated with original session's language and topic IDs

## 6. Frontend — Retake Flow

- [x] 6.1 When navigating to `/interview/setup` via retake, populate the language dropdown with the original session's `languageId`
- [x] 6.2 Populate topic checkboxes from the original session's `topicIdsJson` (parse JSON set back into checked array)
- [x] 6.3 Verify that user can modify any field before clicking "Start Interview" — retake is just a pre-populated setup form, not forced
- [x] 4.4 On successful start from retake flow, navigate to `/interview/{newSessionId}` (existing resume URL pattern)

## 7. Testing & Verification

- [x] 7.1 Backend compile: `cd backend && mvn compile` passes with no errors
- [x] 7.2 Frontend lint: `cd frontend && npm run lint` passes — verify no NG0203 effect-placement violations in new components
- [x] 7.3 Verify resume flow end-to-end: start interview → answer some questions → navigate to `/interview/{id}` → all questions load, answered ones marked correct
- [x] 7.4 Verify detail view end-to-end: finish interview → navigate to `/interview/detail/{id}` → see all questions with answers and evaluations displayed correctly
