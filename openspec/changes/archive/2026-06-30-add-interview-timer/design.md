## Context

The interview system tracks session timing in the database (`InterviewSession.startedAt`, `timeoutHours`) but provides no visual feedback to users during active sessions. Users cannot see how long they've been working or when their 2-hour timeout expires, creating uncertainty about time management and risk of lost work.

**Current state:**
- Backend: `InterviewSession` entity has `startedAt` (LocalDateTime) and `timeoutHours` (int, default 2) fields
- `/api/interview/resume-status` returns session data including these timestamps
- Frontend: `InterviewPageComponent`, `TheoryQuestionComponent`, and `CodingQuestionComponent` have no timer UI
- No existing interval/timer infrastructure in the Angular app

**Constraints:**
- Must work with OnPush change detection strategy (already used throughout)
- Timer must persist across question type transitions (THEORY ↔ CODE)
- Must not break keyboard shortcuts or navigation flows
- TypeScript strict mode enabled — no `any` types

## Goals / Non-Goals

**Goals:**
- Display real-time elapsed time since session started
- Show countdown to 2-hour timeout threshold
- Update every second during active interview
- Persist across question navigation within same session
- Auto-hide when navigating away from interview page or session ends

**Non-Goals:**
- No auto-expiration logic (backend cleanup task handles abandoned sessions)
- No pause/resume functionality for the timer
- No notification system when timeout is approaching (e.g., "5 minutes remaining")

## Decisions

**1. Use Angular `setInterval` in `InterviewPageComponent` instead of a service**

Rationale: Timer state is scoped to the interview session lifecycle, which maps directly to the component's lifecycle. A shared service would require additional cleanup logic and doesn't provide value since only one timer exists per user at a time.

**2. Add `startedAt` and `timeoutHours` fields to `InterviewSummary` DTO**

Rationale: The backend entity has these fields (`InterviewSession.startedAt`, `InterviewSession.timeoutHours`) but they're not currently exposed in the `/api/interview/resume-status` response. Adding them as minimal fields to the existing record enables frontend timer calculations without creating a new endpoint or breaking existing API consumers.

**3. Display format: "01:23:45 elapsed / 01:59:59 remaining"**

Rationale: Clear, compact format that fits in the interview header without dominating screen space. Uses 2-digit hours/minutes/seconds with leading zeros for consistent width.

**4. Use `Date.now()` deltas instead of interval counter for accuracy**

Rationale: `setInterval` can drift if user switches browser tabs (browsers throttle background timers). Calculate elapsed time as `(now - startedAt)` on each tick, not by incrementing a counter. This ensures accuracy even after tab inactivity.

**5. Place timer in interview header area above question display**

Rationale: Existing `interview-header` section already contains the progress bar and question type badge. Adding timer here keeps it visible without cluttering the question area or requiring new layout sections.

## Risks / Trade-offs

[Risk] Browser tab throttling may cause timer to appear stuck after extended inactivity → Mitigation: Use `Date.now()` deltas (Decision 4) so time jumps forward correctly when user returns to tab

[Risk] OnPush change detection won't update timer automatically → Mitigation: Call `cdr.markForCheck()` inside the interval callback to trigger CD manually

[Risk] Mobile battery saver may kill intervals more aggressively → Trade-off: Acceptable for interview use case; users are actively using the page, not leaving it open in background

[Risk] Component destroyed while interval is active causes memory leak → Mitigation: Subscribe/unsubscribe pattern with `ngOnDestroy` cleanup (already required by Angular best practices)
