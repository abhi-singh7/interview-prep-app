## 1. Backend — Fix session timeout cleanup persistence

- [x] 1.1 Read `config/SessionTimeoutCleanupTask.java` to confirm current annotation and write logic
- [x] 1.2 Remove `readOnly = true` from `@Transactional(readOnly = true)` on `cleanupExpiredSessions()` so status changes persist to the database
- [x] 1.3 Verify by reading `InterviewSetupService.getResumeStatus()` that it still correctly finds ACTIVE sessions after this change

## 2. Backend — Remove inline mutation from session list query

- [x] 2.1 Read `interview/SessionDetailController.java` and identify the timed-out detection + state mutation code in `getRecentSessions()` (around lines 279–286)
- [x] 2.2 Replace inline `session.setStatus(ABANDONED)` with a display-only flag: add a boolean `timedOut` field to whatever DTO/response shape is used, computed via the same elapsed-time calculation but without calling `setStatus()` or `save()` on the entity
- [x] 2.3 Confirm the timeout detection logic (elapsed hours >= timeoutHours) is preserved in the display-only path

## 3. Frontend — Add error handling to setup-page resume card

- [x] 3.1 Read `interview/setup-page/interview-setup.component.ts` lines 194–205 and identify the silent `error: () => {}` handler
- [x] 3.2 Replace the empty error handler with logic that: (a) shows an inline error message card when HTTP status is 4xx/5xx, (b) hides the resume card AND start form on auth errors (401/403), and (c) includes a "Retry" button that re-subscribes to `getResumeStatus()`
- [x] 3.3 Verify the template `resume-card` block (`*ngIf="resumeSession && !isRetakeFlow"`) still renders correctly when resumeSession is null but an error state is set — use separate boolean flags so the card and start form don't render during errors

## 4. Frontend — Add error handling to interview-page active session check

- [x] 4.1 Read `interview/interview-page/interview-page.component.ts` lines 354–401 (`checkForActiveSession()`) and identify the silent error handler
- [x] 4.2 Replace the empty error handler with a `MatSnackBar` notification showing "Unable to check session status" plus an optional retry action
- [x] 4.3 Add logic so that when `getResumeStatus()` returns an active sessionId, the resume card is shown even if the subsequent `loadCurrentQuestions()` call fails — use separate state flags for `hasActiveSession` and `questionsLoaded`

## 5. Frontend — Make question loading failures retriable from interview page

- [x] 5.1 Read `interview/interview-page/interview-page.component.ts` lines 46–55 to understand the "No Active Session" fallback card
- [x] 5.2 Modify the template so that when questions fail to load AND an active session was previously identified, show a "Unable to load questions — retry?" inline message with a Retry button instead of the generic "No Active Session" fallback
- [x] 5.3 Wire the Retry button to call `loadCurrentQuestions(sessionId)` again (extract into a dedicated method if needed)

## 6. Frontend — Add error states to my-sessions page

- [x] 6.1 Read `interview/my-sessions/my-sessions.component.ts` and identify where session detail fetching happens
- [x] 6.2 Add an error state per-session card (e.g., "Failed to load" with retry) when individual session fetches return HTTP errors — do NOT break the rest of the grid
- [x] 6.3 Wire a per-card retry button that re-fetches just that session's data

## 7. Verification

- [x] 7.1 Run `npm run lint` in frontend/ to confirm no new lint violations (especially NG0203 effect-placement, OnPush CD rules)
- [x] 7.2 Run `mvn compile -DskipTests` in backend/ to confirm Java compilation succeeds after annotation and mutation fixes
- [x] 7.3 Verify the Resume button now shows an error state by simulating: (a) a network failure on `/interview/resume`, (b) a 500 response, (c) an active session where question loading is forced to fail
