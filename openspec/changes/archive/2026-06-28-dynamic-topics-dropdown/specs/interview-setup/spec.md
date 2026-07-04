## ADDED Requirements

### Requirement: Setup page loads languages from backend API

When the interview setup form mounts, the system SHALL fetch available programming languages from `GET /interview/languages` to populate the language selection dropdown dynamically.

#### Scenario: Languages load on mount
- **WHEN** user navigates to `/interview` and the setup form renders
- **THEN** the system calls `GET /interview/languages` successfully (returns 200)
- **AND** displays the returned languages in a dropdown for selection

#### Scenario: Languages load fails on mount
- **WHEN** user navigates to `/interview` and the setup form renders
- **THEN** if `GET /interview/languages` returns an error, the system displays a snackbar notification indicating failed to load language options

### Requirement: Topics dropdown loads dynamically based on selected language

When a user selects a programming language from the dropdown, the system SHALL fetch sub-topics for that specific language via `GET /interview/topics?langId={id}` and populate the topics multi-select dropdown.

#### Scenario: Topics load after selecting a language
- **WHEN** user selects "Java" from the language dropdown
- **THEN** the system calls `GET /interview/topics?langId=2` (or whatever ID Java has)
- **AND** displays all sub-topics of Java in the topics multi-select dropdown

#### Scenario: Topics load fails after selecting a language
- **WHEN** user selects a language from the dropdown
- **THEN** if `GET /interview/topics?langId=X` returns an error, the system displays a snackbar notification indicating failed to load topic options

### Requirement: Topics dropdown supports debounced search filtering

When a user types into the topics multi-select input field, the system SHALL make a debounced API call (`GET /interview/topics?langId={id}&search={text}`) after 500ms of inactivity and filter the displayed topic options based on the search text.

#### Scenario: Search filters topics after typing
- **WHEN** user types "core" into the topics multi-select input field (after selecting Java as language)
- **THEN** after 500ms of no additional typing, the system calls `GET /interview/topics?langId=2&search=core`
- **AND** displays only topic options matching the search text "core"

#### Scenario: Search requires minimum characters before API call
- **WHEN** user types a single character into the topics multi-select input field
- **THEN** no API call is made until at least 2 characters are typed

### Requirement: Interview questions load from backend with topicIds array on start

When a user submits the setup form, the system SHALL call the `/api/interview/start` endpoint to generate questions, receiving them in the response and displaying the first question immediately. The request body uses `topicIds` (nullable array) instead of `categoryId`.

#### Scenario: Start interview with selected topics
- **WHEN** user fills all setup fields including selecting one or more topics and clicks "Start Interview"
- **THEN** the system calls `POST /interview/start` with topicIds (array), difficulty, count, and language
- **AND** the response contains sessionId and questions array
- **AND** the first question is displayed immediately

#### Scenario: Start interview without selecting specific topics — auto-select by LLM
- **WHEN** user fills all setup fields including a language but does not select any specific topics (empty topicIds array) and clicks "Start Interview"
- **THEN** the system calls `POST /interview/start` with an empty topicIds array, difficulty, count, and language
- **AND** the LLM auto-selects relevant topics based on the selected programming language
- **AND** after receiving results, the user sees a toast notification informing them which topics were auto-selected

#### Scenario: Start interview fails — show error to user
- **WHEN** `POST /interview/start` returns an error (400 or 500)
- **THEN** the system displays a snackbar notification with the error message and does not navigate away from the setup page

### Requirement: Results page displays topic names instead of IDs

When a user views interview results, the system SHALL resolve category/topic IDs to human-readable topic names before displaying them.

#### Scenario: Results show topic names
- **WHEN** user navigates to `/results/{sessionId}` after completing an interview
- **THEN** each evaluation detail displays the topic name (e.g., "Core Java") instead of a raw ID number

## MODIFIED Requirements

### Requirement: Setup page renders when no active session exists

When a user navigates to `/interview` and has no active interview session, the system SHALL display the interview setup form allowing the user to configure and start an interview.

#### Scenario: Navigate to /interview with no active session
- **WHEN** user navigates to `/interview` after login or registration
- **THEN** the system displays the interview setup page with fields for language selection, topic multi-select (with search), difficulty, and question count

### Requirement: Interview questions load from backend on start

When a user submits the setup form, the system SHALL call the `/api/interview/start` endpoint to generate questions, receive them in the response, and display the first question immediately.

#### Scenario: Start interview with valid configuration
- **WHEN** user fills all setup fields and clicks "Start Interview"
- **THEN** the system calls `POST /interview/start` with topicIds (array), difficulty, count, and language
- **AND** the response contains sessionId and questions array
- **AND** the first question is displayed immediately
