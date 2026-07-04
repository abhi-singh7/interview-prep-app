## ADDED Requirements

### Requirement: User can configure interview session parameters
A logged-in user SHALL be able to set the following options before starting an interview: topic (category), difficulty level, number of questions, and programming language for coding challenges. The system SHALL validate that all fields are selected before allowing the interview to start.

#### Scenario: Valid configuration submitted
- **WHEN** user selects topic, difficulty, question count, and language then clicks Start Interview
- **THEN** system generates a new interview session with AI-generated questions based on these parameters

#### Scenario: Missing required field
- **WHEN** user attempts to start without selecting all fields
- **THEN** system displays error "Please select all options"

### Requirement: Questions are generated dynamically by AI at session start
When an interview is started, the backend SHALL call the AI endpoint with the session parameters and receive a dynamic set of questions. The questions SHALL be persisted server-side as part of the interview session before being sent to the frontend.

#### Scenario: Successful question generation
- **WHEN** user starts an interview with valid configuration
- **THEN** system calls AI to generate questions, stores them in the database, and returns the question list to the frontend

### Requirement: Interview session persists server-side
An active interview session SHALL be stored in the database on creation. The session SHALL include its status (ACTIVE), user ID, configured parameters, generated questions, and timestamps (startedAt). If a user loses connection or navigates away, their session SHALL remain active for later resumption.

#### Scenario: Session created and persisted
- **WHEN** user starts an interview
- **THEN** system creates InterviewSession record with status ACTIVE in the database

### Requirement: Active sessions can be resumed
A logged-in user navigating to the interview page SHALL have the option to resume any active session associated with their account. The system SHALL check for and display pending active sessions before allowing a new interview setup.

#### Scenario: Resume existing session
- **WHEN** user navigates to Interview page while an ACTIVE session exists for them
- **THEN** system displays "You have an ongoing session — [topic], [X/Y questions done]" with a Resume button

#### Scenario: No active session found
- **WHEN** user navigates to Interview page and no ACTIVE session exists for their account
- **THEN** system shows the standard interview setup screen

### Requirement: Sessions auto-expire after configurable timeout
An active session SHALL be automatically terminated if it remains inactive beyond a configurable timeout period (default: 2 hours, set via application.properties). Expired sessions SHALL have their status changed to ABANDONED.

#### Scenario: Session expires due to inactivity
- **WHEN** the configured timeout duration passes without any activity on an active session
- **THEN** system marks the session as ABANDONED and removes it from resume options

### Requirement: User can end an interview session early
A user SHALL be able to terminate their current interview session before completing all questions. The system SHALL mark the session status as COMPLETED and proceed with evaluation of submitted answers.

#### Scenario: Early termination
- **WHEN** user clicks "Finish Interview" while some questions are unanswered
- **THEN** system marks the session as COMPLETED, evaluates all submitted answers, and shows results
