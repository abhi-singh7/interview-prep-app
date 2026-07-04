## ADDED Requirements

### Requirement: All answers are evaluated after interview completion
When a user clicks "Finish Interview", the backend SHALL batch-evaluate all submitted answers (both theory and coding) by calling the AI evaluation endpoint for each answer. The system SHALL collect all evaluation results before returning them to the frontend.

#### Scenario: Batch evaluation of mixed question types
- **WHEN** user finishes interview with both theory and coding questions answered
- **THEN** system evaluates all answers sequentially, storing each evaluation result in the database

### Requirement: Evaluation response includes structured feedback
Each AI evaluation SHALL return a JSON object containing: score (0-10), strengths (array of strings), weaknesses (array of strings), and an improved answer or solution. The backend SHALL validate this structure before storing the result.

#### Scenario: Valid evaluation received
- **WHEN** AI returns a well-formed evaluation response
- **THEN** system stores the structured feedback in the Evaluation database record

### Requirement: Partial results are viewable when some evaluations fail
If any question's evaluation fails after all retries, the user SHALL still be able to view results for questions that were successfully evaluated. Failed evaluations SHALL display a retry button allowing the user to attempt re-evaluation manually after resolving the API issue.

#### Scenario: View partial results on failure
- **WHEN** some evaluations fail and others succeed
- **THEN** system displays all available results with an option to retry failed evaluations individually

### Requirement: User can retry individual failed evaluations
A user SHALL be able to retry any evaluation that previously failed due to API issues. The retry SHALL use the same deterministic prompt as the original attempt.

#### Scenario: Manual retry of failed evaluation
- **WHEN** user clicks "Retry" on a FAILED evaluation record
- **THEN** system re-sends the question and answer to the AI evaluator using the same deterministic prompt
