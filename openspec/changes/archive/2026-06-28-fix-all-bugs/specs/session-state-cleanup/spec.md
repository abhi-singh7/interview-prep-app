## ADDED Requirements

### Requirement: Session state is fully cleared when starting new interview

When a user starts a new interview, the system SHALL clear all session-related state (currentSessionId signal, sessionStorage entry, answers map, and codingSubmissions map) before saving the new session ID.

#### Scenario: Clear old state before starting new interview

- **WHEN** user submits the setup form to start an interview
- **THEN** `InterviewSignalService.clearCurrentSession()` is called first (clears signal and sessionStorage)
- **AND** `answers` Map is cleared (all previous answers are discarded)
- **AND** `codingSubmissions` Map is cleared (all previous submissions are discarded)
- **AND** then the new session ID, questions, and auth state are saved

#### Scenario: Resume preserves old state before loading new data

- **WHEN** user resumes an existing interview session via `/interview/:sessionId` route
- **THEN** the currentSessionId signal is updated with the resumed sessionId
- **AND** answers and codingSubmissions maps retain their previous values (answers from earlier questions are not lost)

### Requirement: Session state is fully cleared after finishing interview

When a user finishes an interview, the system SHALL clear all session-related state including the answers map and coding submissions map.

#### Scenario: Clear all state after interview completion

- **WHEN** user clicks "Finish Interview" on the last question
- **THEN** `InterviewSignalService.clearCurrentSession()` is called (clears signal and sessionStorage)
- **AND** `answers` Map is cleared
- **AND** `codingSubmissions` Map is cleared
