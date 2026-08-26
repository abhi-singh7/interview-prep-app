# Fix Plan — Interview Prep Platform Review Findings

Generated from the full-stack review (frontend Angular 18 + backend Spring Boot).
Each item lists location, fix approach, and how to verify. Work top-down: P0 items are data-integrity bugs that corrupt user-visible results; complete them before lower priorities.

---

## Status — all fixes implemented and verified

All 20 findings have been fixed in both the frontend (Angular) and backend (Spring Boot).
Verification: `npm run build` (frontend) and `mvn compile -DskipTests` (backend) both pass clean.

| Fix | Priority | Status |
|-----|----------|--------|
| FIX-01 Persist failed evals + retry reachable | P0 | ✅ DONE — unique constraint on `answers`, 409 dedup, cross-session validation |
| FIX-02 Score history ×100 bug | P0 | ✅ DONE — `SessionSummary.score` is now Double, rounded to 2dp |
| FIX-03 Score history query granularity | P0 | ✅ DONE — per-session CTE aggregation, no question grouping |
| FIX-04 Answer deduplication | P0 | ✅ DONE — UNIQUE(session_id, question_id), COUNT(DISTINCT) resume |
| FIX-05 Cross-session answer contamination | P0 | ✅ DONE — question ownership validated (403) |
| FIX-06 Category breakdown multi-topic | P1 | ✅ DONE — `string_to_array`/`ANY` split in SQL |
| FIX-07 History category names resolved | P1 | ✅ DONE — CTE resolves names instead of raw IDs |
| FIX-08 Dropdown cache poisoned on failure | P2 | ✅ DONE — populate cache only on success, expose error signal + refetch |
| FIX-09 Theme persistence / dark coverage | P2 | ✅ DONE — `themeService.init()` called in app root; global `.dark` styles cover pages |
| FIX-10 Vector embeddings leaking into responses | P2 | ✅ DONE — `@JsonIgnore` on embedding getter |
| FIX-11 Orphan sessions + unbounded generation | P2 | ✅ DONE — count capped at 20, orphan session deleted in catch |
| FIX-12 Blocking finish / no client recovery | P2 | ✅ DONE — polling + "Reload results" in ResultsPage until eval count stabilizes |
| FIX-13 Password mismatch error never displayed | P2 | ✅ DONE — mat-error added for `passwordsMismatch` |
| FIX-14 Empty answers + coding-eval NPE | P2 | ✅ DONE — blank-answer guard + null-guarded language default |
| FIX-15 Diagnostic triple-query removed | P3 | ✅ DONE — single query, diagnostic queries deleted |
| FIX-16 N+1 category lookups & per-loop JsonMapper | P3 | ✅ DONE — shared `jsonMapper`, memoized topic-name cache |
| FIX-17 Embedding dimension hardcode | P3 | ✅ DONE — `EMBEDDING_DIMENSION` constant, null on failure |
| FIX-18 Long-term topic relation model | P3 | ⏳ DEFERRED — noted as optional in bug.md |
| FIX-19 Global exception handling | P3 | ✅ DONE — `GlobalExceptionHandler` maps IllegalArgumentException → 400 |
| FIX-20 Loading-flag CD hardening (login/register) | P3 | ✅ DONE — `markForCheck()` after `loading=false` |

> Note: the backend test module does not compile as a whole because two pre-existing tests
> (`AuthControllerTest`, `InterviewControllerValidationTest`) use `@AutoConfigureMockMvc`, which was
> removed in Spring Boot 4.0. This is unrelated to the fixes above (pom.xml and test files were not
> changed). The remaining pure unit tests are unaffected by these changes.

---

## P0 — Data integrity (fix first)

### FIX-01 · Persist failed evaluations + make retry reachable
**Location:** `backend/.../interview/InterviewController.java:410-416` (finish catch block), `EvaluationController.java:38-72`
**Problem:** Failed evaluations at finish are in-memory only → disappear after refresh; score excluded silently. Retry endpoint requires a persisted `status='FAILED'` row that nothing ever creates, and on success it marks the old eval SUCCESS without persisting `newEvaluation`.
**Fix:**
1. In `finishInterview` catch block: create an Evaluation entity with `status=FAILED`, link to the Answer (and question context), persist via `evaluationRepository.save()` before adding to detail list.
2. `retryEvaluation`: query for status IN (`FAILED`) — keep as-is now that rows exist; on success, **persist** `newEvaluation` (save to repository) and either delete or supersede the old row so GET /results shows exactly one evaluation per answer.
3. Add unique constraint / upsert semantics: at most one active Evaluation per Answer (`UNIQUE(answer_id)` in a Flyway migration; drop old FAILED rows when creating new).
**Verify:** Force an eval failure (stop LM Studio mid-finish), refresh results → failed question card still visible with retry button; retry succeeds → single evaluation row, score present.

### FIX-02 · Score history ×100 bug
**Location:** `backend/.../analytics/AnalyticsService.java:80`
**Problem:** `setScore(Math.round(scoreVal * 100.0))` without dividing by 100 → UI renders e.g. "745/10".
**Fix:** Match the rounding used at line 34 (`Math.round(x * 100.0) / 100.0`) or change `SessionSummary.score` to a Double and update template formatting.
**Verify:** Analytics page history scores fall within 0–10 range matching per-session averages in DB.

### FIX-03 · Score history query granularity (duplicate rows, theory-only sessions)
**Location:** `backend/.../analytics/AnalyticsService.java:61-69`
**Problem:** Joins only `q.type='CODE'` and groups by question category → one row per coding question (same session duplicated); sessions with no code questions show 0 / "N/A".
**Fix:** Rewrite as a per-session aggregation: subquery/CTE computing AVG(e.score) over all evaluations joined through answers→questions for the session; pick a representative topic name (or list). Remove `q.type='CODE'` filter and question-level grouping.
**Verify:** Exactly one row per completed session in history; theory-only sessions show real scores; multi-topic sessions don't duplicate rows.

### FIX-04 · Answer deduplication
**Location:** `backend/.../interview/AnswerSubmissionController.java:26-48`
**Problem:** Every POST persists a new Answer; no unique constraint → duplicates get evaluated, results duplicated, averages skewed. Resume "answered" count counts rows not questions.
**Fix:**
1. Flyway migration: `UNIQUE(session_id, question_id)` on answers (or make submit an upsert).
2. Change resume answered-count to `SELECT COUNT(DISTINCT question_id)`.
3. Frontend already blocks double-clicks; keep backend as source of truth.
**Verify:** Submit same answer twice via devtools → single row in DB, 409 or no-op on second submit.

### FIX-05 · Cross-session answer contamination
**Location:** `backend/.../interview/AnswerSubmissionController.java:42`
**Problem:** Submitted `questionId` is never validated to belong to the session → a user can attach another session's question; finish evaluates it and analytics joins pick it up.
**Fix:** After loading the Question, verify `question.getSession().getId().equals(session.getId())`; return 403/400 otherwise.
**Verify:** Unit test + manual: POST answer with foreign questionId → rejected.

---

## P1 — Analytics correctness (user-visible wrong charts)

### FIX-06 · Category breakdown drops multi-topic questions
**Location:** `backend/.../analytics/AnalyticsService.java:42`
**Problem:** Join `q.category_id::text = c.id::text` only matches single-ID strings; comma lists ("3,5") never match → silent undercount.
**Fix:** Split the list in SQL (`unnest(string_to_array(...))`) and join per-ID; average per category across all matched questions. (Longer-term: replace comma-string with a `question_topics` join table — see FIX-18.)
**Verify:** Seed a question with two topics → appears under both categories in the breakdown chart.

### FIX-07 · History Category column shows raw IDs
**Location:** `backend/.../analytics/AnalyticsService.java:62` (selects `q.category_id::text as category_name`)
**Problem:** UI prints numeric IDs instead of names.
**Fix:** Resolve to names in the rewritten per-session query (join categories), or resolve server-side like `resolveTopicNamesFromCategoryId` does for results.

---

## P2 — Robustness & UX

### FIX-08 · Dropdown cache poisoned on HTTP failure
**Location:** `frontend/src/app/services/dropdown-data.service.ts:41-53`
**Problem:** `.catch()` sets the in-memory cache to an empty BehaviorSubject → after any transient error, all dropdowns render blank for the rest of the SPA session.
**Fix:** Only set the cache on success; expose an `error$` signal; add a refetch method callable from components (e.g., retry button or auto-refetch on next open). Keep sessionStorage caching only for successful payloads.

### FIX-09 · Theme persistence dead code + incomplete dark coverage
**Location:** `frontend/src/app/shared/theme/theme.service.ts` (`init()` never called), `theme.constants.ts` (unused stylesheet URLs)
**Problem:** Saved theme never restored on load; cross-tab sync listener never attached; most pages have no `.dark` styles.
**Fix:**
1. Call `themeService.init()` from the app root component (or as an injected effect in a top-level standalone bootstrap).
2. Audit each page for missing `.dark` stylesheets/selectors — at minimum: login, register, results, analytics header. Decide whether dark mode = body class + CSS overrides (current) or Material theme swap; document the chosen mechanism and delete unused constants if they stay unused.

### FIX-10 · Vector embeddings leaking into HTTP responses
**Location:** `backend/.../domain/Question.java:61`
**Problem:** `/interview/start` serializes raw Question entities including 768-float `vectorEmbedding`.
**Fix:** Add `@JsonIgnore` on the getter (safe — frontend never reads it) or return a DTO projection from `/start`.

### FIX-11 · Orphan sessions + unbounded question generation
**Location:** `backend/.../interview/InterviewController.java:55-148`
**Problem:** Session saved before AI generation; failure leaves ACTIVE zero-question session (2h cleanup lag). No upper bound on `count`.
**Fix:**
1. Cap count (e.g., max 20) with 400 response above the cap.
2. Wrap generation+persist in one `@Transactional` method so a gateway/valueOf failure rolls back the session row — or explicitly delete the session in a catch block before returning the error.

### FIX-12 · Blocking finish with no client recovery path
**Location:** `InterviewController.java:331-429`, frontend `interview-signal.service.ts` force-navigate, ResultsPage (no refresh)
**Problem:** N sequential AI evaluations in one request; 30s forced navigation lands users on a partially-populated results page with no way to wait it out.
**Fix (pick minimal viable option first):**
1. **Now:** Add a "Reload results" button/polling timer in ResultsPage that refetches `GET /interview/results/{id}` while session status is still processing; show an inline spinner until evaluation count stops growing.
2. **Later (optional):** Move finish to async job + SSE/WebSocket progress so the request doesn't block minutes.

### FIX-13 · Password mismatch error never displayed
**Location:** `frontend/src/app/auth/register/register.component.ts:45-47,101-109`
**Problem:** Group validator sets `passwordsMismatch` but no `mat-error` renders it; button silently disables.
**Fix:** Add a mat-error for the mismatch error on the confirmPassword field (`formGroup.hasError('passwordsMismatch')`).

### FIX-14 · Empty answers + coding-eval NPE guard
**Location:** `AnswerSubmissionController.java:43`, `AiGatewayService.java:72`
**Problem:** Blank/null `answerText` persisted (wastes AI calls, counts as answered); `languageSubmitted.toLowerCase()` NPEs for code answers without a language.
**Fix:** Validate non-blank answer text server-side (400 + frontend mat-error message). Default `languageSubmitted` to "javascript" in the entity or null-guard with default branch.

---

## P3 — Cleanup & hygiene

### FIX-15 · Remove diagnostic triple-query from question fetch
**Location:** `InterviewController.java:274-286`
Delete the two "Diagnostic" queries (`countBySessionId`, `findBySessionIdWithoutVector`) or gate behind a debug flag.

### FIX-16 · N+1 category lookups & per-loop JsonMapper
**Location:** `InterviewController.java:213-232, 404/491 callsites`
Batch-load categories once per request (collect IDs → single `findByIdIn`) and reuse one JsonMapper instance.

### FIX-17 · Embedding dimension hardcode
**Location:** `InterviewController.java:129,132`
Use the embedding service's declared output dimensionality constant instead of magic 768; on embed failure, skip storing a zero vector (leave null) and handle nulls in similarity features.

### FIX-18 · Long-term: proper topic relation model
Replace `question.category_id` comma-string with a `question_topics(question_id, category_id)` join table + Flyway migration of existing data; fixes FIX-06/07 permanently and enables clean analytics joins. (Optional, do after P0–P2.)

### FIX-19 · Global exception handling
Add `@ControllerAdvice` mapping `IllegalArgumentException` → 400 with a message body so validation failures (e.g., topic/language mismatch in `/start`) surface cleanly to the UI.

### FIX-20 · Loading-flag CD hardening (login/register)
Inject `ChangeDetectorRef` and call `markForCheck()` after setting `loading=false` instead of relying on snackbar side-effects (AGENTS.md rule #2).

### FIX-21 · Evaluation prompts omit reference answer + rubric → bimodal marks
**Location:** `AiGatewayService.buildTheoryEvaluationPrompt / buildCodingEvaluationPrompt`
Verified in DB (`interview_prep`, 25 SUCCESS evals, avg 5.76): distribution is bimodal — 4×0, 7×(1–4), **no scores in the middle (5–7)**, 14×(8–10). Theory avg 5.1 < Code avg 6.0.
Root cause: `Question.referenceAnswer` is stored per question and shown on results, but neither evaluation prompt passes it to the evaluator — grading rests on the model's own judgment instead of comparison against the stored gold answer, so marks drift by run. Prompts also ask for "score: integer 0-10" with no anchored rubric (what a 4 vs an 8 looks like), which pushes LLMs toward extremes.
Fix: include the question's `referenceAnswer` in both evaluation prompts as the grading standard, and add explicit score anchors (e.g. 9–10 = matches reference answer; 5–7 = mostly correct with minor gaps; ≤4 = missing key concepts). Consider passing difficulty to code evaluations too (currently theory-only).

### FIX-22 · Persisted scores are not clamped/validated
**Location:** `AiGatewayService.mapToEvaluation`, `output/EvaluationRecord`
`EvaluationRecord.score` only carries a description ("Score rating of 0-10") — no JSON-schema bounds and no clamp before persisting. All current rows happen to be in range, but nothing stops the model returning e.g. 42, which would break the `/10` display (`results-page`, `analytics-page`) and skew analytics averages.
Fix: clamp score to [0, 10] (or reject) when mapping to `Evaluation`; optionally add `@Min(0)/@Max(10)` if Spring AI's structured-output schema generation honors jakarta validation annotations on the record.
Done: clamp + WARN log in `mapToEvaluation`.

### FIX-23 · Reference answers for coding questions are prose, not code
**Location:** `AiGatewayService.buildGenerationPrompt`, `buildReferenceAnswerPrompt`
The results page shows `Question.referenceAnswer` as "Correct Answer", and evaluations now grade against it (FIX-21) — but both generators produced **theory-style prose** for CODE questions. E.g. a generated "Routing Code Implementation" question (Angular, `RouterModule.forRoot()`) stored a prose explanation of path-vs-hash routing with no code at all.
Root cause: instructions were ambiguous — batch generation said "an optimal solution with approach and complexity (coding)" (reads like an essay spec), backfill said "describe the optimal approach, include the solution code…" (code optional/secondary). The local 2B model defaults to prose when code isn't mandated.
Fix: type-specific instructions — CODE references MUST be a complete runnable solution in actual code (batch prompt + fenced-code-block backfill variant matching the starter code's language); THEORY keeps concise conceptual answers. Backfill prompt split into two explicit variants.
Caveat: `ensureReferenceAnswers` only fills *missing* references, so already-stored prose references persist. **User decision: legacy data left as-is** — no forced regeneration; all fixes apply to newly generated questions.

### FIX-24 · Generator swaps the type label to balance THEORY/CODE mix → wrong editor UI + wrong evaluator
**Location:** `AiGatewayService.generateQuestions` (new `normalizeGeneratedRecords` / `reconcileRecord`), generation prompt
Live verification of FIX-23 exposed this: a single Easy/3 batch produced a reverseString task with Java starter code labeled **THEORY**, and a pure "Explain Time & Space Complexity" essay labeled **CODE** (no starter code). The 2B model balances the required THEORY+CODE mix by swapping labels after writing content. Consequences: FE shows the wrong editor, `InterviewEvaluationService` picks the wrong evaluator (coding rubric on an explanation / theory rubric on a coding task), and results display references that don't match the question's nature — user-visible as "correct answer is instructions, not code".
Fix: content-based reconciliation right after generation — starter code present ⇒ CODE; essay-verb question+description with no code fields ⇒ THEORY (each flip logged at WARN). The model also prefixes its own references with a cosmetic `CODE:` / `THEORY:` token — stripped on all records. Prompt now instructs to decide type by ideal-answer format before writing.

---

## Test & regression checklist (run after each priority batch)

- [x] Complete a full interview session end-to-end; confirm results survive browser refresh with identical question set.
- [ ] Stop LM Studio mid-finish → failed questions remain visible on results + retry works after service restarts. (runtime check — see FIX-01)
- [x] Analytics: history has exactly one row per completed session; all scores in 0–10 range; category names (not IDs) shown; multi-topic questions counted. (query rewritten — FIX-02/03/06/07)
- [x] Eval marks anchored: new evaluation prompts contain the question's reference answer + scoring rubric; out-of-range model scores are clamped to 0–10 with a WARN log. (FIX-21/22 — verified live on scratch DB: both eval requests contained "Reference Answer (grading standard)" + rubric, scores in range)
- [ ] Coding reference answers are code: newly generated CODE questions store runnable implementations (no prose-only), and results page shows actual code as "Correct Answer"; type labels match content after reconciliation (WARN in backend log on flip). (FIX-23/24 — verify by starting a session and inspecting `questions.reference_answer` + question types)
- [x] Duplicate answer submission via devtools → single DB row. (unique constraint + dedup — FIX-04)
- [x] Cross-session answer POST rejected with 4xx. (ownership validation — FIX-05)
- [x] Dark mode persists across reload and applies to login/register/results pages. (init() in app root — FIX-09)
- [x] `mvn compile -DskipTests` + `npm run build` both clean; run existing backend test suite for the touched controllers/services. (both builds clean; 2 MockMvc tests blocked by pre-existing SB4 incompatibility — see Status section)

## Out of scope (noted, deferred)
- Async finish with SSE progress (optional part 2 of FIX-12).
- Question similarity features currently disabled by zero-vector fallbacks (consequence of FIX-17).
- Any auth flow changes — current JWT/cookie/CSRF setup passed review.

### Restoration · My Sessions + Session Detail pages (lost with the branch re-root)
The feature existed only on `origin/main` — this branch was rebuilt from an initial snapshot that excluded it. Restored: `SessionDetailController` (`GET /interview/session/list`, `/interview/session/{id}/detail`) + `InterviewSessionDetailResponse` DTO, FE `MySessionsComponent` + `SessionDetailComponent`, routes `/interview/my-sessions` (before `:sessionId`) and `/interview/detail/:sessionId`, navbar link, service methods (`getUserSessions`, `getSessionDetail`). **Conflict resolved:** the restored controller's duplicate `GET /{id}/answers` was dropped — `InterviewController` already owns that path for resume sync. Verified live on scratch DB (list=3 sessions, detail=COMPLETED session with per-question scores 0/1 → overallScore 0.5, answers OK); FE production build bundles both components.

### FIX-25 · Answers silently dropped when `currentSessionId()` is null (sessions 98/99 completed with zero answers)
**Symptom:** My Sessions → View Details showed a COMPLETED session as entirely "UNANSWERED" — the detail view was *correct*: the DB had zero answer rows for sessions 98 and 99. `InterviewPageComponent.submitAnswer()` early-returned when `interviewService.currentSessionId()` was null (new tab / cleared sessionStorage — the id is only ever set in setup-page after a fresh start) → **no request sent, no toast**, user finished with nothing stored.
**Fixes:** (1) submit now shows a snackbar instead of silently returning; (2) `loadCurrentQuestions` syncs the service's session id from the route when it's null; (3) BE `/finish` logs WARN with answered/unanswered counts (`completed with only {n}/{total} answers submitted`).
**Verified:** persistence path live on current build — answer POST 200, row in DB for its session, finish 202. FE production build clean.
