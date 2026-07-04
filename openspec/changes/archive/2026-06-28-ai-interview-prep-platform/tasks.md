## 1. Project Setup — Backend

- [x] 1.1 Create Spring Boot 4 project with Java 25, dependencies: Spring Web, Spring Security, Spring Data JPA, Lombok
- [x] 1.2 Add PostgreSQL driver dependency and pgvector extension support
- [x] 1.3 Configure application.properties with database connection (PostgreSQL), JWT secret, AI API endpoint URL, and session timeout constant
- [x] 1.4 Create package structure: auth, interview, question, evaluation, analytics under com.interviewprep
- [x] 1.5 Set up Spring Security configuration for httpOnly cookie-based authentication

## 2. Project Setup — Frontend

- [x] 2.1 Scaffold Angular 18+ project with Angular Material dependency
- [x] 2.2 Add ng2-charts dependency and Chart.js peer dependency
- [x] 2.3 Configure Angular routing with guards for authenticated routes
- [x] 2.4 Set up httpOnly cookie interceptor for JWT token transmission on every request
- [x] 2.5 Create shared services: AuthService (httpOnly cookie management), InterviewSignalService (state via Signals + RxJS)

## 3. Database Layer — Entities and Repositories

- [x] 3.1 Create User entity with id, name, email, password, createdAt fields; UserRepository extends JpaRepository
- [x] 3.2 Create Category entity with id, name fields; CategoryRepository
- [x] 3.3 Create InterviewSession entity with id, userId (FK), categoryId, difficulty, language, status (ACTIVE/COMPLETED/ABANDONED), startedAt, endedAt, timeoutHours fields; InterviewSessionRepository
- [x] 3.4 Create Question entity with id, sessionId (FK), categoryId, type (THEORY/CODE), questionText, title, description, codePrompt fields; QuestionRepository
- [x] 3.5 Create Answer entity with id, sessionId (FK), questionId, answerText, languageSubmitted fields; AnswerRepository
- [x] 3.6 Create Evaluation entity with id, answerId (FK), score, strengths (JSONB array), weaknesses (JSONB array), improvedAnswer, correctedness (boolean + explanation), timeComplexity, spaceComplexity, status (SUCCESS/FAILED) fields; EvaluationRepository
- [x] 3.7 Create UserPerformance entity with id, userId (FK), totalSessions, avgScore, categoryBreakdown (JSONB), updatedAt fields; UserPerformanceRepository

## 4. Database Layer — PostgreSQL and pgvector Setup

- [x] 4.1 Write SQL migration to create pgvector extension in PostgreSQL
- [x] 4.2 Add pgvector column type to Question entity for semantic search capability
- [x] 4.3 Create initial seed data script: default categories (System Design, Java, Spring Boot, Angular, SQL, Behavioral)

## 5. Authentication Feature — Backend

- [x] 5.1 Implement AuthService with register and login methods; password hashing via BCryptPasswordEncoder
- [x] 5.2 Implement JWT token generation and httpOnly cookie setting logic in AuthController
- [x] 5.3 Create /auth/register endpoint that validates unique email before creating user
- [x] 5.4 Create /auth/login endpoint that authenticates credentials and sets httpOnly JWT cookie on success
- [x] 5.5 Create /auth/logout endpoint that clears the httpOnly JWT cookie
- [x] 5.6 Add CSRF protection for cross-origin requests (Spring Security CSRF with sameSite=Strict)

## 6. Authentication Feature — Frontend

- [x] 6.1 Create Login page component with email/password form and submit handling
- [x] 6.2 Create Register page component with name/email/password/confirmPassword form and validation
- [x] 6.3 Implement AuthService in Angular: register(), login(), logout() methods calling backend endpoints; httpOnly cookie management via httpOnlyCookie service
- [x] 6.4 Create AuthGuard for protecting authenticated routes (Interview, Analytics)

## 7. Interview Session Feature — Backend

- [x] 7.1 Implement AiGatewayService interface with generateQuestions(interviewParams) method for dynamic question generation
- [x] 7.2 Create AI Gateway implementation using OpenAI-compatible endpoint; configure temperature=0.7 for generation, temperature=0 for evaluation
- [x] 7.3 Implement InterviewService.startInterview(userId, params) — calls AI gateway, creates InterviewSession record with status ACTIVE, persists generated questions
- [x] 7.4 Create /interview/start POST endpoint that validates all parameters (topic, difficulty, count, language), starts interview via service
- [x] 7.5 Implement InterviewService.getResumeStatus(userId) — returns active session if exists for user
- [x] 7.6 Create /interview/resume GET endpoint returning active session data for resume option

## 8. Interview Session Feature — Frontend

- [x] 8.1 Create InterviewSetup page component with form: topic dropdown, difficulty dropdown (Easy/Medium/Hard), question count input, language dropdown (Java/Python/Angular)
- [x] 8.2 Implement InterviewSignalService in Angular: signal-based state for currentSessionId, questions[], answers Map, codingSubmissions Map, evaluations[]
- [x] 8.3 Create Interview page component that checks for active session on load — shows resume option if found, otherwise shows setup screen
- [x] 8.4 Implement Interview page flow: show generated questions one by one with answer textarea (theory) or code textarea + language selector (coding), progress indicator showing X/Y

## 9. Question Management Feature — Backend

- [x] 9.1 Implement AI prompt templates for question generation — include topic, difficulty, count, and question type distribution in the prompt
- [x] 9.2 Create AiEvaluationService with evaluateTheory(Answer) method using deterministic temperature=0 and theory evaluation prompt template
- [x] 9.3 Create language-specific coding evaluation prompt templates (Java, Python, JavaScript/Angular) — each explicitly instructing AI to evaluate algorithmic thinking over syntax
- [x] 9.4 Implement retry logic with exponential backoff: 3 attempts at 5s, 10s, 20s delays using Spring Retry or custom implementation

## 10. Question Management Feature — Frontend

- [x] 10.1 Create TheoryQuestion component displaying question text and textarea for answer submission
- [x] 10.2 Create CodingQuestion component displaying title, description, language dropdown selector, and textarea for code input
- [x] 10.3 Implement Interview page state management: submit answer per question to backend on "Next" button click (store in SignalService answers Map), submit coding solution with selected language

## 11. Evaluation Feature — Backend

- [x] 11.1 Implement InterviewService.finishInterview(sessionId) — marks session as COMPLETED, triggers batch evaluation of all submitted answers
- [x] 11.2 Implement batch evaluation: iterate over all unanswered questions in the session, call AiEvaluationService for each, store results in EvaluationRepository
- [x] 11.3 Create /interview/finish POST endpoint that validates user owns the session, finishes it and returns evaluation results (or partial results if some evaluations failed)
- [x] 11.4 Implement retry endpoint: /evaluation/retry/{answerId} — re-evaluates a specific FAILED answer using the same deterministic prompt

## 12. Evaluation Feature — Frontend

- [x] 12.1 Create "Finish Interview" button on Interview page that calls POST /interview/finish
- [x] 12.2 Implement loading state during batch evaluation (show spinner with message: "Evaluating your answers...")
- [x] 12.3 Handle partial results response — display available evaluations, show retry buttons for FAILED ones

## 13. Results Display Feature — Backend

- [x] 13.1 Calculate overall score as weighted average of all individual question scores in finishInterview service method
- [x] 13.2 Implement InterviewService.getResults(sessionId) — returns session status, all evaluations with their feedback, and overall score
- [x] 13.3 Create /interview/results/{sessionId} GET endpoint returning full results data for display

## 14. Results Display Feature — Frontend

- [x] 14.1 Create Results page component displaying overall score prominently
- [x] 14.2 For each THEORY question result: display user's answer text, AI's score, strengths array, weaknesses array, improved answer
- [x] 14.3 For each CODE question result: display submitted code block, correctness assessment, time/space complexity analysis, score, strengths, weaknesses, improved solution in same language
- [x] 14.4 Implement retry button for any FAILED evaluation items on results page

## 15. Analytics Feature — Backend

- [x] 15.1 Create AnalyticsService with getPerformanceData(userId) method — aggregates completed sessions from InterviewSession and Evaluation repositories
- [x] 15.2 Calculate totalSessions, avgScore across all sessions for the user
- [x] 15.3 Calculate categoryBreakdown: group evaluations by categoryId, compute average score per category
- [x] 15.4 Query completed sessions ordered by endedAt descending (most recent first)

## 16. Analytics Feature — Frontend

- [x] 16.1 Create Analytics page component with analytics service call to fetch performance data
- [x] 16.2 Display basic metrics: total sessions completed, average score across all sessions
- [x] 16.3 Display category breakdown using ng2-charts bar chart — each category as a bar showing its average score
- [x] 16.4 Display score history list/table with columns: Date, Topic, Difficulty, Score (sorted by date descending)

## Remaining Work — Completed

### Fix getCurrentUserId() method across all controllers
- [x] Created AuthenticationContext service to extract current user from request attributes
- [x] Created JwtRequestInterceptor filter that validates JWT cookie and sets userId/email as request attributes on each request
- [x] Updated SecurityConfig to use the authentication interceptor instead of @AuthenticationPrincipal annotations
- [x] Updated InterviewController to use authContext.getCurrentUserId() instead of getCurrentUser() placeholder methods
- [x] Updated AnswerSubmissionController to use authContext.getCurrentUserId() instead of getCurrentUser() placeholder methods
- [x] Updated EvaluationController to use authContext.getCurrentUserId() instead of getCurrentUser() placeholder methods
- [x] Updated AnalyticsController to use authContext.getCurrentUserId() instead of getCurrentUser() placeholder methods

### Refactor InterviewPageComponent for proper question navigation and Finish button
- [x] Rewrote InterviewPageComponent with proper Next/Previous/Skip/Finish button flow
- [x] Added @ViewChild references to TheoryQuestionComponent and CodingQuestionComponent to read answer text from children
- [x] Added finishInterview() method that submits the last answer then navigates to results page after evaluation
- [x] Updated TheoryQuestionComponent with Output event emitter for submit events and currentAnswer getter via ViewChild
- [x] Updated CodingQuestionComponent with Output event emitter for coding submission events

### CSRF token interceptor in Angular frontend
- [x] Created CsrfTokenInterceptor that reads _csrf cookie and adds X-XSRF-TOKEN header on non-GET requests
- [x] Registered interceptor via withInterceptorsFromDi() (already configured in app.config.ts)
- [x] Added 403 retry logic to re-read CSRF token if first attempt fails
