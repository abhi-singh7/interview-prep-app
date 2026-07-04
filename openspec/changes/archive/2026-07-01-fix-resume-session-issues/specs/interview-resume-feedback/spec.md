## ADDED Requirements

### Requirement: Resume errors are visible to the user on the setup page
The system SHALL display a user-readable error message when the resume status API (`GET /interview/resume`) fails with an HTTP error, instead of silently hiding the resume card. The error MUST include a retry button that re-issues the request.

#### Scenario: Resume API returns 401 Unauthorized
- **WHEN** the `getResumeStatus()` call receives a 401 response (e.g., expired JWT)
- **THEN** the setup page shows an "Session expired — please log in again" message with a "Sign In" link, and does NOT show either the resume card or the start form

#### Scenario: Resume API returns 500 Internal Server Error
- **WHEN** the `getResumeStatus()` call receives any non-success HTTP response (4xx or 5xx) other than 401/403
- **THEN** the setup page shows a "Something went wrong. Please try again." message with a "Retry" button

#### Scenario: Resume API succeeds but returns no active session
- **WHEN** `GET /interview/resume` returns HTTP 204 No Content (no active session)
- **THEN** the setup page shows the normal interview start form (existing behavior preserved)

### Requirement: Resume errors are visible to the user on the interview page
The system SHALL display a user-readable error message when `checkForActiveSession()` fails, and MUST show an "Ongoing Session" card when the backend reports an active session even if question loading subsequently fails.

#### Scenario: Active session exists but questions fail to load
- **WHEN** `getResumeStatus()` returns an active sessionId AND `loadCurrentQuestions(sessionId)` later fails or returns empty results for that session
- **THEN** the interview page displays a card showing "You have an ongoing interview" with the resume button, NOT the "No Active Session / Start Interview" fallback

#### Scenario: Resume status API call fails on interview page
- **WHEN** `getResumeStatus()` (called from `checkForActiveSession()`) returns any non-success HTTP response
- **THEN** the interview page shows a snack-bar/notification with "Unable to check session status. Please try again." and a retry button, instead of silently showing nothing

#### Scenario: Resume API succeeds on interview page
- **WHEN** `getResumeStatus()` returns an active sessionId with startedAt timestamp
- **THEN** the resume option is enabled and the user can click "Resume Interview" to navigate to `/interview/{sessionId}`

### Requirement: Question loading errors are retriable from the interview page
The system SHALL provide a retry mechanism for question loading failures so users can recover without restarting their session.

#### Scenario: Questions fail to load during resume
- **WHEN** `loadCurrentQuestions(sessionId)` returns an error (HTTP 4xx/5xx) while resuming
- **THEN** the page shows the question area with an "Unable to load questions" message and a "Retry" button, instead of blank or redirecting away

#### Scenario: Retry succeeds on second attempt
- **WHEN** the user clicks the retry button after a failed question load, AND the subsequent call succeeds
- **THEN** the normal interview view renders with the loaded questions

### Requirement: Timed-out sessions are consistently marked abandoned on the backend
The system SHALL correctly transition timed-out ACTIVE sessions to ABANDONED status so they do not appear as resumable.

#### Scenario: SessionTimeoutCleanupTask persists state changes
- **WHEN** a session's elapsed time exceeds `interview.session.timeout-hours` and `SessionTimeoutCleanupTask.cleanupExpiredSessions()` runs
- **THEN** the session is persistently updated to ABANDONED status in the database (not silently skipped due to readOnly transaction)

#### Scenario: Inline mutation in session list does not corrupt state
- **WHEN** `SessionDetailController.getRecentSessions()` iterates over sessions and detects a timed-out ACTIVE session
- **THEN** the timeout detection is performed purely for display purposes; entity state changes are NOT persisted inside this read endpoint

### Requirement: My-sessions page shows error states during resume
The system SHALL display an error message with retry capability when individual session data fails to load on the my-sessions page.

#### Scenario: Session detail fetch fails
- **WHEN** fetching details for a specific session (e.g., `GET /interview/session/{sessionId}/answers`) returns an HTTP error while rendering the sessions grid
- **THEN** that session card shows "Failed to load" with a retry option instead of breaking or showing blank content
