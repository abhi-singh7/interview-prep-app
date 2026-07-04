## ADDED Requirements

### Requirement: Completed session detail view displays all interview data

When a user views the details of their own completed interview session, the system SHALL display all questions asked during that session along with each answer provided and its evaluation (if one was generated). The view must be read-only — no editing or modification is possible from this page.

#### Scenario: View shows question list with status indicators
- **WHEN** user navigates to `/interview/detail/{sessionId}` for their own completed session
- **THEN** the system displays all questions in a scrollable list
- **AND** each question shows its title (or first line of text if no title), type indicator (THEORY/CODE), and status: answered (with score if evaluated) or unanswered

#### Scenario: View shows answer details per question
- **WHEN** user expands or clicks on an answered question in the detail view
- **THEN** the system displays the user's submitted answer text (for THEORY) or code (for CODE, rendered with syntax highlighting via existing HighlightBlockComponent)
- **AND** for evaluated answers: shows score (out of 10), strengths list, weaknesses list, improved answer suggestion, time/space complexity, and correctness explanation

#### Scenario: View supports retrying failed evaluations
- **WHEN** a question's evaluation has status FAILED in the detail view
- **THEN** the system provides a "Retry Evaluation" button that calls `POST /evaluation/retry/{answerId}` (existing endpoint)
- **AND** on success, the evaluation status updates to SUCCESS and the full feedback is displayed

#### Scenario: View shows session metadata header
- **WHEN** user opens the detail view
- **THEN** the page header displays: language name, topic names (comma-separated), difficulty level, total questions count, date started, date completed
- **AND** the overall score (average of all evaluation scores) is displayed if at least one question was evaluated

### Requirement: Retake button initiates fresh interview with same topics

When a user clicks the "Retake" button on a completed session detail page, the system SHALL navigate to `/interview/setup` pre-populated with the original session's topic selection (and language), allowing the user to start a new interview with those same topics. The existing session remains untouched in the database.

#### Scenario: Retake navigates to setup with populated fields
- **WHEN** user clicks "Retake" on `/interview/detail/{sessionId}` for their own completed session
- **THEN** the system navigates to `/interview/setup`
- **AND** the language dropdown is pre-selected to the original session's languageId
- **AND** topic checkboxes reflect the original session's topicIdsJson (resolved from JSON set)

#### Scenario: User can modify topics before starting new interview
- **WHEN** user arrives at setup page via Retake
- **THEN** they can change any field (language, difficulty, question count, topic selection) before clicking "Start Interview"
- **AND** the original session's data is not affected by any changes on this new setup form
