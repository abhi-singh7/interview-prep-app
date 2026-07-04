## ADDED Requirements

### Requirement: Evaluations are persisted to database during interview finish

When `POST /interview/finish` is called, the system SHALL save each Evaluation entity created by AI evaluation into the `evaluations` table before returning the results.

#### Scenario: Finish interview with successful evaluations
- **WHEN** user finishes an interview via `POST /interview/finish`
- **THEN** the backend creates Evaluation entities for all answers
- **AND** each Evaluation is persisted via `evaluationRepository.save()`
- **AND** the response includes all evaluation details

### Requirement: Results page displays evaluations after navigation away and back

When a user navigates to `/results/{sessionId}` after finishing an interview (or reloads the page), the system SHALL display all past evaluations fetched from the database.

#### Scenario: View results on page load
- **WHEN** user navigates to `/results/{sessionId}` where session status is COMPLETED
- **THEN** the `GET /interview/results/{sessionId}` endpoint queries the `evaluations` table for that session's answers
- **AND** returns all evaluation details including scores, strengths, weaknesses, and improved answers

### Requirement: Failed evaluations are persisted with FAILED status

When an AI evaluation fails during interview finish, the system SHALL save the Evaluation entity with status = FAILED so users can see which evaluations failed and retry them.

#### Scenario: Finish with a failed evaluation
- **WHEN** AI evaluation throws an exception for one answer
- **THEN** the catch block creates an EvaluationDetail with `status = FAILED`
- **AND** the Evaluation entity is persisted with status FAILED in the database
- **AND** the results page shows a "Retry Evaluation" button for that question

### Requirement: Retry evaluation persists updated result

When a user retries a failed evaluation, the system SHALL save the new successful evaluation and update the old one.

#### Scenario: Retry a failed evaluation
- **WHEN** user clicks "Retry Evaluation" on a FAILED evaluation card
- **THEN** `POST /evaluation/retry/{answerId}` re-runs AI evaluation
- **AND** if successful, updates the existing FAILED Evaluation to status SUCCESS
- **AND** returns the new evaluation data in the response
