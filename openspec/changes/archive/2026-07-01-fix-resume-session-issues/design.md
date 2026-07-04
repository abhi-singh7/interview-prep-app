## Context

The resume flow is used when a user returns to an existing active interview session. Three entry points exist: (1) Setup page banner showing "You have an ongoing interview" with a Resume button, (2) Interview page `/interview` without a sessionId param which checks for active sessions via `checkForActiveSession()`, and (3) My Sessions grid.

Currently every error path in these flows is swallowed silently (`error: () => {}`). When the resume API fails for any reason — expired JWT, network blip, 500 from backend — the user sees nothing: not a resume card, not an error message, just silence. Similarly, if questions fail to load after identifying an active session, the "No Active Session" fallback renders even though the session IS active per the backend.

The backend has two additional correctness issues: `SessionTimeoutCleanupTask` is annotated `@Transactional(readOnly = true)` but writes entity state inside it, and `SessionDetailController.getRecentSessions()` mutates entity state inline during a read loop.

## Goals / Non-Goals

**Goals:**
- Every error in the resume flow produces a visible, actionable message to the user
- Active sessions remain discoverable even when question loading fails (session state is authoritative)
- Timed-out sessions are consistently marked ABANDONED on the backend so they don't appear resumable after the timeout window
- Read-only endpoints do not persist entity state changes

**Non-Goals:**
- No new API endpoints — this change works with existing `GET /interview/resume` and `GET /interview/session/{id}/questions` contracts
- No backend logic changes to resume detection itself (`InterviewSetupService.getResumeStatus()`)
- No changes to the session timeout configuration or threshold values

## Decisions

### D1: Use Angular Material snack-bar for error notifications (not inline error cards)

**Decision:** For transient errors (network blips, 500s), show a Material `MatSnackBar` notification with an "Undo" action. For persistent errors requiring user intervention (401/403), render an inline message card in the affected component.

**Rationale:** Snack-bars are non-blocking and dismissible — appropriate for recoverable errors like temporary 500s where a retry button may not be needed. Inline cards with explicit Retry buttons are used when the user must take action (e.g., re-authenticate). This avoids cluttering the resume card UI with error states that should be transient.

**Alternatives considered:**
- *Toast-only everywhere:* Loses the retry capability for persistent failures like 401s where the user needs to explicitly sign in again.
- *Inline cards only:* Would clutter the already-busy setup page and interview page with multiple error card states.

### D2: Session state authority — backend response wins over question load results

**Decision:** When `getResumeStatus()` returns an active sessionId, do NOT suppress the resume UI even if subsequent `loadCurrentQuestions()` calls fail or return empty. The resume button navigates to `/interview/{sessionId}`; that route's own error handling covers loading failures independently.

**Rationale:** The two operations are independent — session existence vs. question availability. Coupling them (hiding resume because questions failed) creates the reported bug where active sessions become undiscoverable after any transient question-load failure.

**Alternatives considered:**
- *Tight coupling:* Show no resume UI if questions can't load. This is what's currently broken — a single error blocks the entire resume path.
- *Retry before showing resume:* Wait for question loading to succeed before showing resume card. Adds latency and still blocks on failure.

### D3: Fix `SessionTimeoutCleanupTask` by removing `readOnly` annotation

**Decision:** Change `@Transactional(readOnly = true)` to plain `@Transactional` on `cleanupExpiredSessions()`. The method legitimately writes entity state — it's not a read-only operation despite the name suggesting cleanup is observational.

**Rationale:** Hibernate respects `readOnly=true` by disabling dirty-checking and flush, which means the `session.setStatus(ABANDONED)` calls have no effect on the database. Removing `readOnly` restores proper write behavior without changing any other logic.

### D4: Extract inline mutation from `SessionDetailController.getRecentSessions()` into a separate method

**Decision:** Move the timed-out session detection + state mutation out of the query iteration loop in `getRecentSessions()`. Use it only for display purposes (flagging sessions as timed-out in the response DTO) without persisting changes. Persist state transitions via a dedicated `SessionStatusService.markTimedOut(Long)` method used by the cleanup task.

**Rationale:** Read endpoints must not mutate entity state — this violates the CQRS principle and causes subtle bugs when combined with readOnly transactions or concurrent access. The inline mutation in `getRecentSessions()` is effectively dead code (it sets status but never flushes due to the controller's read-only transaction boundary), and removing it eliminates a class of potential inconsistency.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Snack-bar spam if errors are frequent (e.g., backend is down) | Debounce error snack-bars: only show one per 5 seconds, coalesce identical messages |
| User might click Resume on a session whose questions haven't loaded yet after a server restart | The `/interview/{sessionId}` route has its own loading spinner and "Retry" button — this is acceptable UX for a transient state |
| Removing `readOnly` from cleanup task could cause unexpected flush behavior during concurrent reads | This was already the intended behavior; the annotation was simply incorrect. Add an integration test to verify persistence. |
| Inline mutation removal in controller might leave timed-out sessions visible as ACTIVE briefly until next cleanup run | The frontend also checks elapsed time client-side (my-sessions: `isSessionTimedOut()`). Combined with the 5-minute scheduled cleanup, stale active status is at most ~5 min old. Acceptable. |

## Migration Plan

This change is deploy-ready in a single release — no data migration needed. Backend fixes (`@Transactional`, inline mutation removal) are backward-compatible. Frontend changes add new error-state UI but don't break existing flows for users with healthy sessions.

Rollback: revert the four changed files; resume behavior returns to current (silent errors, broken resume). No user-visible database state is changed by this deployment.

## Open Questions

- Should the frontend `InterviewSignalService` expose a single observable that combines session status + question loading state for easier error handling in components? Currently these are two separate subscriptions with independent error swallowing — consolidating them would reduce code duplication but adds complexity.
- The `SESSION_TIMEOUT_HOURS` constant (2 hours) is duplicated between backend (`application.properties`) and frontend (`my-sessions.component.ts`). Should this be fetched from a config endpoint on app init?
