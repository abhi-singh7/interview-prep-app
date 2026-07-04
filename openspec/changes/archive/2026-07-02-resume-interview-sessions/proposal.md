## Why

Users who leave an interview mid-session lose access to their questions and cannot resume — navigating back to `/interview/{id}` shows nothing because Question entities are persisted without a session_id foreign key, leaving every question orphaned in the database. Additionally, users who have completed an interview have no way to review what was asked or retake it with the same topics; the only path is starting a brand-new interview from scratch.

This change fixes the data integrity issue and adds two user-facing capabilities: resuming active sessions properly, and reviewing + retaking completed sessions.

## What Changes

- **Fix orphaned Question entities** — `QuestionGenerationService` will set `q.setSession(session)` before persisting; a Flyway migration backfills `session_id` on all existing orphans via the Answer chain
- **Feature A: Resume active session** — when an ACTIVE session exists, navigating to `/interview/{id}` correctly reloads questions from DB (not just inline from start response), and marks already-answered questions as complete in the UI state
- **Feature B: Session detail view + retake** — new `GET /interview/session/{id}/detail` endpoint returns full session picture (questions, answers, evaluations); new frontend page at `/interview/detail/{id}` displays it with a "Retake" button that starts a fresh interview using the same topic IDs from `topicIdsJson`
- Completed sessions remain visible only to their owning user — ownership check enforced on detail endpoint
- ABANDONED sessions are NOT resumable or viewable for retake (terminal state, no recovery)

## Capabilities

### New Capabilities
- `session-resume`: Properly resume an ACTIVE interview session by loading questions from DB and syncing answered-question state to the frontend
- `session-retake`: View full details of a completed interview session and retake it with the same topic selection (regenerating questions, discarding prior answers/evaluations)

### Modified Capabilities
- None — existing `interview-setup` spec requirements remain satisfied once the data fix is in place; no behavioral requirement changes, only implementation corrections

## Impact

| Area | Change |
|---|---|
| **Backend** | `QuestionGenerationService.generateAndPersistQuestions()` sets session FK; new `InterviewSessionDetailController` (or endpoint on existing controller) with `/interview/session/{id}/detail`; Flyway V4 migration to backfill question.session_id from answer chain |
| **Frontend** | New route `/interview/detail/:sessionId` with detail display component and retake button; resume flow in `InterviewPageComponent` re-fetches questions via new working endpoint and syncs answered state |
| **Database** | Flyway V4 migration: `UPDATE questions q SET session_id = a.session_id FROM answers a WHERE a.question_id = q.id` — bridges existing orphans to their correct sessions |
| **Security** | Detail endpoint enforces ownership (user must own the session) |
| **Existing bug** | `/interview/session/{id}/questions` currently returns empty array for real sessions due to orphaned questions; this change makes it work correctly |
