## ADDED Requirements

### Requirement: Setup page renders when no active session exists

When a user navigates to `/interview` and has no active interview session, the system SHALL display the interview setup form allowing the user to configure and start an interview.

#### Scenario: Navigate to /interview with no active session
- **WHEN** user navigates to `/interview` after login or registration
- **THEN** the system displays the interview setup page with fields for category, difficulty, question count, and language selection

### Requirement: Interview questions load from backend on start

When a user submits the setup form, the system SHALL call the `/api/interview/start` endpoint to generate questions, receive them in the response, and display the first question immediately.

#### Scenario: Start interview with valid configuration
- **WHEN** user fills all setup fields and clicks "Start Interview"
- **THEN** the system calls `POST /interview/start` with categoryId, difficulty, count, and language
- **AND** the response contains sessionId and questions array
- **AND** the first question is displayed immediately

### Requirement: Active session shown on page load

When a user navigates to `/interview` and has an active interview session, the system SHALL display a resume option.

#### Scenario: Navigate with active session
- **WHEN** user navigates to `/interview` while an ACTIVE session exists for their account
- **THEN** the system calls `GET /interview/resume` successfully (returns 200)
- **AND** displays a message showing the ongoing interview category and progress
- **AND** provides a "Resume Interview" button

### Requirement: Resume loads questions from backend

When a user resumes an active session, the system SHALL load all previously generated questions for that session.

#### Scenario: Resume active interview
- **WHEN** user clicks "Resume Interview" on the resume screen
- **THEN** the system fetches questions via `GET /interview/session/{sessionId}/questions`
- **AND** displays the last answered question or first unanswered question
- **AND** shows the correct progress indicator

### Requirement: All question types display correctly

The system SHALL render THEORY questions with a text area for answers and CODE questions with a code editor, language selector, and starter code view.

#### Scenario: Display theory question
- **WHEN** current question type is "THEORY"
- **THEN** the TheoryQuestionComponent renders showing the question text and an answer textarea
- **AND** the user can enter their answer as text

#### Scenario: Display coding question
- **WHEN** current question type is "CODE"
- **THEN** the CodingQuestionComponent renders showing the title, description, starter code, language selector, and a code editor
- **AND** the user can select a programming language and write code

### Requirement: Navigation between questions works

The system SHALL allow users to navigate forward and backward through their interview questions.

#### Scenario: Navigate to next question
- **WHEN** user clicks "Next" button while not on last question
- **THEN** the current answer is submitted (if one exists)
- **AND** the next question is displayed

#### Scenario: Navigate to previous question
- **WHEN** user clicks "Previous" button while not on first question
- **THEN** the previous question is displayed with its previously entered answer restored

### Requirement: Finish interview loads results page

When a user finishes an interview, the system SHALL call `POST /interview/finish` to evaluate all answers and then navigate to `/results/{sessionId}`.

#### Scenario: Finish last question
- **WHEN** user clicks "Finish Interview" on the last question
- **THEN** any answer for that question is submitted first
- **AND** `POST /interview/finish` is called with the sessionId
- **AND** after receiving results, the user navigates to `/results/{sessionId}`
