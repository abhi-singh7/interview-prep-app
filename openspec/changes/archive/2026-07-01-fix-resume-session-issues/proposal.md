## Why

Users clicking "Resume Interview" on an ongoing session see no response — the button does nothing and the UI shows either a blank state or "No Active Session." This is caused by three compounding bugs: resume API error handlers silently swallow all failures (`error: () => {}`), questions fail to load for active sessions with no visible feedback, and no user-facing error display exists anywhere in the resume flow.

## What Changes

- Add user-visible error handling throughout the session resume flow (setup page, interview page, my-sessions page)
- Replace silent `error: () => {}` handlers with descriptive error states shown to the user
- Show "Resume Interview" card even when question loading fails for an active session — the session state is authoritative on the backend
- Add a retry mechanism for failed resume operations
- Fix backend `SessionTimeoutCleanupTask` annotation (`@Transactional(readOnly = true)` on a write operation) so timed-out sessions are properly marked ABANDONED and don't appear as resumable after timeout
- Remove inline entity state mutation from session list query in `SessionDetailController.getRecentSessions()`

## Capabilities

### New Capabilities
- `interview-resume-feedback`: User-visible error states, retry actions, and loading indicators for all resume-related flows (setup page banner, interview page no-session fallback, my-sessions grid)

### Modified Capabilities
- None — this is a new capability adding requirements that did not exist before. The existing `interview-setup` spec does not cover error handling or user feedback during resumption.

## Impact

**Frontend:**
- `setup-page/interview-setup.component.ts` — resume card error handling, retry button
- `interview-page/interview-page.component.ts` — active session check, question loading errors, no-session fallback
- `my-sessions/my-sessions.component.ts` — error display for failed session loads
- New: shared error component or snack-bar pattern for consistent error messaging

**Backend:**
- `config/SessionTimeoutCleanupTask.java` — fix `@Transactional(readOnly = true)` to allow writes
- `interview/SessionDetailController.java` — remove inline entity mutation from query loop (move to dedicated service method)

**No API changes required** — the issue is entirely in error handling and state consistency, not contract.
