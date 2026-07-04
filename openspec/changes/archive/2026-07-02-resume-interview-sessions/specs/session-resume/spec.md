## ADDED Requirements

### Requirement: Resume loads all questions from database for active session

When a user resumes an ACTIVE interview session by navigating to `/interview/{sessionId}`, the system SHALL load all previously generated questions for that session from the database (not just inline from the start response) and display them with correct answered/unanswered state.

#### Scenario: User navigates to active session URL
- **WHEN** user has an ACTIVE session (id=42) and navigates to `/interview/42`
- **THEN** the system calls `GET /interview/session/42/questions` successfully
- **AND** all questions originally generated for that session are returned with their full text, type, title, description, and code prompt
- **AND** answered questions are marked as complete in the frontend state (derived from existing Answer entities)

#### Scenario: Resume shows correct progress indicator
- **WHEN** user resumes an active session where 3 of 5 questions have been answered
- **THEN** the UI displays a progress indicator showing "3/5 answered" or equivalent
- **AND** navigation defaults to the first unanswered question (or last answered, per existing behavior)

### Requirement: Active session detail endpoint enforces ownership

When a user requests details of an interview session, the system SHALL verify that the requesting user owns the session before returning any data.

#### Scenario: Owner accesses their own session
- **WHEN** authenticated user A requests `GET /interview/session/42/detail` and session 42 belongs to user A
- **THEN** the system returns a 200 response with full session detail (questions, answers, evaluations)

#### Scenario: Non-owner attempts access
- **WHEN** authenticated user B requests `GET /interview/session/42/detail` where session 42 belongs to user A
- **THEN** the system returns a 403 Forbidden response

#### Scenario: Unauthenticated request
- **WHEN** an unauthenticated request is made to `GET /interview/session/42/detail`
- **THEN** the system returns a 401 Unauthorized response (handled by existing JWT filter)

#### Scenario: ABANDONED session not accessible for detail view
- **WHEN** any user requests `GET /interview/session/{abandonedId}/detail` where the session status is ABANDONED
- **THEN** the system returns a 404 Not Found response (no recovery path for abandoned sessions)

### Requirement: Session detail view returns full question-answer-evaluation picture

When returning details for a completed interview session, the system SHALL include all questions asked, each user answer, and corresponding evaluation (if one exists), along with session metadata (language name, topic names, difficulty).

#### Scenario: Detail response includes all data
- **WHEN** `GET /interview/session/42/detail` returns successfully for a completed session
- **THEN** the response contains:
  - Session metadata: languageId, languageName, topicIdsJson (resolved to topicNames), categoryId, difficulty, startedAt, endedAt
  - All questions with their text, type, title, description, codePrompt
  - Each user answer linked to its question with answerText and languageSubmitted
  - Evaluations for answered questions with score, strengths, weaknesses, improvedAnswer, time/space complexity, correctness explanation
- **AND** unanswered questions appear in the list with no associated answer or evaluation

#### Scenario: Detail response uses human-readable names
- **WHEN** session detail is returned to the frontend
- **THEN** languageId resolves to a human-readable language name (e.g., "Java" not "15")
- **AND** topicIdsJson resolves to an array of human-readable topic names

### Requirement: Retake starts new interview with same topics

When a user clicks "Retake" on a completed session detail page, the system SHALL create a brand-new interview session using the same topic selection from the original session. Prior answers and evaluations are discarded (the old session remains COMPLETED in database; a new ACTIVE session is created).

#### Scenario: Retake initiates fresh interview
- **WHEN** user clicks "Retake" on `/interview/detail/42` where session 42 has topicIdsJson = {1,3,5}
- **THEN** the system calls `POST /interview/start` with languageId and topicIds ["1","3","5"] from the original session's metadata
- **AND** a new InterviewSession is created with status ACTIVE
- **AND** new questions are generated via AI using those same topics (questions will differ from originals since generation is non-deterministic)
- **AND** the user is navigated to `/interview/{newSessionId}`

#### Scenario: Retake does not modify original session
- **WHEN** user clicks "Retake" on a completed session
- **THEN** the original session (id=42) retains status COMPLETED with all its prior answers and evaluations intact in the database
- **AND** only a new session is created — no data mutation on the old session
