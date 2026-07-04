## 1. Backend — Interview routing and question loading

- [x] 1.1 Add `/interview/session/{sessionId}/questions` GET endpoint in InterviewController that fetches questions from `questionRepository.findBySessionId(sessionId)` for the resume scenario
- [x] 1.2 Update `InterviewPageComponent.loadCurrentQuestions()` to call the new backend endpoint and populate `currentQuestions` array, removing the stub `_sessionId` parameter prefix

## 2. Backend — Evaluation persistence

- [x] 2.1 Inject `EvaluationRepository` into `InterviewController` (add constructor parameter)
- [x] 2.2 In `finishInterview()`, add `evaluationRepository.save(evaluation)` inside the evaluation loop after each AI response is parsed, before building the EvaluationDetail for that answer
- [x] 2.3 Add `@Transactional` annotation to `finishInterview()` method so all evaluations are persisted atomically — if any save fails, roll back the entire operation

## 3. Backend — Auth status endpoint

- [x] 3.1 Add `GET /auth/status` endpoint in `AuthController` that checks if JWT cookie is present and valid (mirrors the logic from SecurityConfig's anonymous filter)
- [x] 3.2 Return `{ authenticated: true }` for valid token, or throw 401 for invalid/missing token

## 4. Backend — Fix duplicate auth filter

- [x] 4.1 Remove the inline anonymous `jakarta.servlet.Filter()` from `SecurityConfig.securityFilterChain()` (lines ~31-52)
- [x] 4.2 Move the request attribute setting logic (`currentUserId`, `currentEmail`) into `JwtAuthenticationFilter` if it's not already there — verify JwtAuthenticationFilter sets these attributes and remove duplication

## 5. Backend — Fix analytics null category crash

- [x] 5.1 In `AnalyticsService.getPerformanceData()`, add null check for `row[2]` (category_id) before casting to String in the session query
- [x] 5.2 Set a default value like `"N/A"` when category is null, or skip sessions with no category from the results list

## 6. Frontend — Add child routes under /interview

- [x] 6.1 Update `app.routes.ts` to add child routes under `/interview`:
    - `{ path: '', redirectTo: '/interview/setup', pathMatch: 'full' }` as default route
    - `{ path: 'setup', loadComponent: SetupPage }` for the setup page
- [x] 6.2 Remove the unused `<router-outlet>` from `InterviewPageComponent` template — replaced with "No Active Session" card that links to /interview/setup

## 7. Frontend — Fix interview page flow

- [x] 7.1 Update `InterviewPageComponent.ngOnInit()` to check if a child route exists (via activated route) and show resume option vs setup based on route, not just the `/interview/resume` API call
- [x] 7.2 Wire up the "Resume Interview" button in `resumeInterview()` to also load questions from backend (use the new endpoint created in task 1.1)

## 8. Frontend — Fix coding question component

- [x] 8.1 Add `@Output() submit = new EventEmitter<{ codeAnswer: string; language: string }>()` to `CodingQuestionComponent` so parent can react when user submits
- [x] 8.2 Wire up submit event — added "Submit Answer" button in CodingQuestionComponent template, bound (submit) in InterviewPageComponent, and added public getters currentCode/currentLanguage for parent access when Finish button is clicked

## 9. Frontend — Fix register password validation

- [x] 9.1 In `RegisterComponent`, add a return statement to the `PasswordMatchValidator` function so it properly returns an error object (currently missing `return { passwordsMismatch: true }`) and added `return null` for success case
