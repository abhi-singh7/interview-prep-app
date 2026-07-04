## Context

The interview session lifecycle currently has a critical data integrity issue: every persisted `Question` entity has `session_id = NULL` because `QuestionGenerationService.generateAndPersistQuestions()` never calls `q.setSession(session)`. The FK exists in the schema via `@ManyToOne` on the Question entity, but no code populates it. This means:

- `/interview/session/{id}/questions` always returns empty for real sessions
- Resume navigation to `/interview/{id}` shows nothing
- There is no way to retrieve "all questions asked in session X" from the database

Answer entities DO have correct `session_id`, and Evaluation entities link through `answer_id`. The Question→InterviewSession link is purely broken at runtime despite being correct in the schema.

The system currently has three session states (ACTIVE, COMPLETED, ABANDONED) with no transitions out of terminal states. Completed sessions can show results but cannot be reviewed for retake.

## Goals / Non-Goals

**Goals:**
1. Fix Question entity orphaning so questions are queryable by session ID — both future inserts and existing data via migration backfill through the Answer chain
2. Enable proper resume of ACTIVE sessions: navigate to `/interview/{id}`, load all original questions from DB, sync answered-question state to frontend UI
3. Enable viewing completed session details (all questions + answers + evaluations) for the owning user only
4. Enable retaking a completed interview with the same topic selection — regenerates questions via AI fresh but uses original topics

**Non-Goals:**
- ABANDONED sessions are NOT recoverable (terminal, no resume or view)
- No change to SessionStatus enum (no RESUMABLE intermediate state)
- Retake does NOT reuse original question text — generates fresh questions per topic selection via AI (current behavior)
- No analytics/dashboard changes beyond the detail view itself

## Decisions

### D1: Fix Question FK, don't work around it

**Decision:** Set `q.setSession(session)` in `QuestionGenerationService` and run a Flyway migration to backfill existing orphans.

```sql
-- V4__fix_question_session_fk.sql
UPDATE questions q
SET session_id = a.session_id
FROM answers a
WHERE a.question_id = q.id;
```

**Rationale:** Using the answer chain as a bridge works because `Answer.session_id` is always set correctly in `AnswerSubmissionController`. This makes Question queryable on its own for any future feature, not just these two. A workaround (derive from answers only) would require repeating this join pattern everywhere questions are needed and still couldn't surface unanswered questions to the user during resume.

**Alternatives considered:**
- *Work around via Answer-derived queries*: Smaller code change but every feature needing "questions for session" repeats the join; can't show unanswered questions during resume.
- *Add a separate question_session mapping table*: Overkill for what is essentially the same FK relationship, just redundant.

### D2: Single new controller for session detail endpoint

**Decision:** Add `GET /interview/session/{id}/detail` to an existing or new controller (recommend new `SessionDetailController`) that returns a structured DTO with questions, answers, evaluations, and metadata.

**Rationale:** Keeps interview-related endpoints co-located. New controller avoids cluttering `InterviewController` which already handles start/finish/resume-check/results. Separation also makes ownership check consistent across all session-detail access paths.

**Alternatives considered:**
- *Extend InterviewResultService.assembleResults()*: Already returns evaluations but not full question list with answer state for non-evaluated questions. Would require significant restructuring of that service.
- *Add to InterviewSetupService*: Mixes concerns — setup service handles creation, not retrieval of past data.

### D3: Retake regenerates questions (does NOT reuse originals)

**Decision:** When user clicks "Retake" on a completed session detail page, the system calls `POST /interview/start` with the same `topicIdsJson` from the original session, creating a completely new session. Prior answers and evaluations are discarded.

**Rationale:** Simpler implementation (reuses existing start flow), generates fresh AI questions which is more valuable for practice than re-answering identical questions, avoids complexity of "which question was asked originally" tracking across sessions. The retake is essentially a new interview with the same topic selection — matching user intent of "practice this again."

**Alternatives considered:**
- *Reuse original question text*: More complex (need to surface old questions per answer), but gives direct comparison. Defer if users request it later.
- *Keep session alive and add more answers*: Requires new status state, complicates evaluation flow for partially-evaluated sessions.

### D4: Ownership check on all detail access paths

**Decision:** Both resume-check (`GET /interview/resume`) and detail view (`GET /interview/session/{id}/detail`) enforce that the requesting user owns the session (matches `InterviewSession.user_id`).

**Rationale:** The current `getResults()` endpoint has no ownership check — any authenticated user can fetch another's results by guessing an ID. This change fixes that gap for the new detail path and reinforces it on existing resume-check. ABANDONED sessions are excluded from both checks (no recovery, no viewing).

### D5: Ship data fix + features together in one release

**Decision:** The Flyway migration (V4) and code changes land atomically — no staggered rollout.

**Rationale:** This is an internal tool; atomic deployment avoids the confusing state where resume works inconsistently before the migration runs. The backfill migration is a single UPDATE statement with no risk of data corruption (each question maps to exactly one session via its answers).

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|---|---|---|
| Migration backfills questions that had multiple answers across sessions — wait, can't happen since FK is on question→session not answer→question. Each Question has ONE session_id once backfilled. | Low | Migration uses `a.question_id = q.id` which maps each question to exactly one session (questions are only created in one session context) |
| Existing data: some questions may have NO answers yet (edge case if user started but never answered). These would NOT be backfilled by the migration, remaining orphaned. | Medium for resume completeness | For these orphans, we can either: (a) accept gap — unanswered questions won't appear in detail view; (b) add fallback to regenerate question list from topicIdsJson on session if no answers exist. Option (a) is acceptable since users who never answered have nothing meaningful to review anyway. |
| Retake starts a fresh interview with potentially different AI-generated questions than the original. User may be confused by "retake" implying same questions. | Low for UX clarity | Label the button as "Start New Interview" rather than "Retake" in UI text, or add explanatory subtitle: "Same topics, new questions generated." Actually we decided on "Retake" so let's use that and accept the semantic stretch — it matches user mental model better than "start new interview." |
| Frontend state leak: `answers` Map in InterviewSignalService is not cleared on session end. | Low-Medium | Feature A (resume) will need to clear/rebuild this map from DB answers when resuming, which is an inherent part of the resume flow anyway — no separate fix needed. |

## Migration Plan

1. **V4 migration**: Run `UPDATE questions q SET session_id = a.session_id FROM answers a WHERE a.question_id = q.id`
2. **Code change**: Add `q.setSession(session)` in `QuestionGenerationService.generateAndPersistQuestions()`
3. **New backend endpoint**: `GET /interview/session/{id}/detail` with ownership check and structured response DTO
4. **Frontend**: New route `/interview/detail/:sessionId`, detail component, retake button wired to existing start flow
5. **Resume wiring**: Update `InterviewPageComponent` to correctly load questions from working endpoint on resume
