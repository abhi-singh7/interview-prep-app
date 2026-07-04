## Why

Users have no visibility into session duration during active interviews — they can't see elapsed time or when their 2-hour session will expire, leading to uncertainty about time management and potential lost work if a session times out unexpectedly.

## What Changes

- Add real-time timer display showing elapsed time since session started
- Add countdown indicator showing remaining time before 2-hour timeout
- Timer updates every second during active interview sessions
- Display persists across question navigation (theory → coding transitions)
- Auto-hide timer when session ends or navigates away from interview page

## Capabilities

### New Capabilities
- `interview-timer`: Real-time elapsed and remaining time display during active interview sessions, showing duration since `startedAt` timestamp and countdown to 2-hour timeout threshold.

### Modified Capabilities
- None

## Impact

**Frontend:**
- `InterviewPageComponent` — inject timer logic, add timer UI template section
- `TheoryQuestionComponent` / `CodingQuestionComponent` — ensure timer persists across question transitions
- `InterviewSignalService` — expose session timing data (startedAt, timeoutHours) to components

**Backend:**
- `InterviewSummary.java` — add `startedAt` (LocalDateTime) and `timeoutHours` (int) fields to DTO
- `/api/interview/resume-status` endpoint — now returns timing metadata for frontend timer calculations
