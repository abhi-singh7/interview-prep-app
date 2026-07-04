## ADDED Requirements

### Requirement: Overall score is displayed after interview completion
After all evaluations are complete (or partial results are available), the system SHALL display an overall score for the interview session calculated as the weighted average of all individual question scores.

#### Scenario: Display overall score with all evaluations successful
- **WHEN** user completes interview and all evaluations succeed
- **THEN** system displays overall score as the average of all question scores out of 10

### Requirement: Per-question answers and feedback are displayed after completion
Each submitted answer SHALL be displayed along with its evaluation results — including score, strengths, weaknesses, and improved version. Coding questions SHALL additionally display the time complexity, space complexity, and improved solution if applicable.

#### Scenario: Display theory question result
- **WHEN** user views interview results for a THEORY type question
- **THEN** system displays their answer text, the AI's score, strengths, weaknesses, and the improved answer

### Requirement: Coding challenge evaluations display algorithmic feedback
For CODE type questions, the result display SHALL show the submitted code, the AI's assessment of correctness, time complexity, space complexity, score, strengths, weaknesses, and an improved solution if applicable.

#### Scenario: Display coding question result
- **WHEN** user views interview results for a CODE type question
- **THEN** system displays their submitted code, evaluation scores (correctness, score out of 10), time/space complexity analysis, strengths, weaknesses, and the improved solution in the same language

### Requirement: Results are displayed at end of session only
Interview results SHALL be shown only after all evaluations have been processed. The user SHALL not see per-question feedback during the interview — only at the end when viewing their complete results page.

#### Scenario: Results visible only after completion
- **WHEN** a user is actively in an interview before clicking "Finish Interview"
- **THEN** system does not display any evaluation feedback until all evaluations are complete and the results page is rendered
