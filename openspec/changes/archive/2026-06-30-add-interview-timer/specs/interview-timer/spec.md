## ADDED Requirements

### Requirement: Timer displays elapsed time and countdown during active sessions
The system SHALL display real-time elapsed time since session started and countdown to 2-hour timeout threshold in the interview page header, updating every second while the user is actively on the interview page.

#### Scenario: Timer shows elapsed time for active session
- **WHEN** a user navigates to an active interview session with `startedAt` timestamp set
- **THEN** the timer displays "00:00:00 elapsed" initially and increments each second

#### Scenario: Timer updates correctly after tab inactivity
- **WHEN** the user switches away from the browser tab for 5 minutes, then returns to the interview page
- **THEN** the timer reflects the actual elapsed time (e.g., "00:05:12 elapsed"), not counting only active seconds

#### Scenario: Timer shows countdown to timeout threshold
- **WHEN** a session has `timeoutHours` set (default 2 hours) and the user is on the interview page
- **THEN** the timer displays remaining time as "01:59:47 remaining" and decrements each second

#### Scenario: Timer auto-hides when navigating away from interview
- **WHEN** the user navigates to a different route (e.g., results, setup) or the session ends
- **THEN** the timer component is destroyed and no longer visible on the page

### Requirement: Timer persists across question type transitions
The system SHALL maintain the same timer instance when navigating between THEORY and CODE questions within the same interview session, ensuring elapsed time continues counting without interruption.

#### Scenario: Timer continues during theory-to-code transition
- **WHEN** a user submits an answer to a THEORY question and the next question is CODE type
- **THEN** the timer does not reset or pause; it continues incrementing seamlessly

#### Scenario: Timer persists when navigating back to previous questions
- **WHEN** a user clicks "Previous" button to return to a previously answered question
- **THEN** the elapsed time reflects total session duration, not time since first viewing current question

### Requirement: Timer respects OnPush change detection strategy
The system SHALL manually trigger Angular's change detection cycle when updating timer display values to ensure the UI updates correctly with OnPush components.

#### Scenario: Timer updates trigger change detection in parent component
- **WHEN** the timer interval fires and elapsed time changes
- **THEN** `ChangeDetectorRef.markForCheck()` is called, causing Angular to update the DOM

#### Scenario: Timer cleanup prevents memory leaks on destroy
- **WHEN** the interview page component is destroyed (user navigates away)
- **THEN** the interval subscription is unsubscribed and no timer callbacks execute after destruction

### Requirement: Timer displays in consistent format with leading zeros
The system SHALL format elapsed time and remaining time using HH:MM:SS format with leading zeros for hours, minutes, and seconds to maintain consistent display width.

#### Scenario: Single-digit values display with leading zero
- **WHEN** elapsed time is 5 minutes and 3 seconds
- **THEN** the timer displays "00:05:03" not "0:5:3" or "00:5:3"

#### Scenario: Hours display correctly for long sessions
- **WHEN** a session has been active for 1 hour, 23 minutes, and 45 seconds
- **THEN** the timer displays "01:23:45" with proper hour formatting
