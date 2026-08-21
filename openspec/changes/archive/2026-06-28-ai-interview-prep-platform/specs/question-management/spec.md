## ADDED Requirements

### Requirement: System supports theory questions with text answers
A theory question SHALL present its question text to the user along with a textarea for typing their answer. The question SHALL have an associated category, difficulty level, and point value.

#### Scenario: Display theory question
- **WHEN** user is in an active interview session and the current question is of type THEORY
- **THEN** system displays the question text and a textarea for the answer

### Requirement: System supports coding questions with code submissions
A coding question SHALL present its title, description, and a textarea for entering code. The user SHALL select a programming language (Java, Python, or JavaScript/Angular) before submitting their solution. The system SHALL evaluate the submitted logic via AI using a deterministic, language-specific prompt.

#### Scenario: Display coding question
- **WHEN** user is in an active interview session and the current question is of type CODE
- **THEN** system displays the question title, description, a language selector dropdown, and a textarea for code input

### Requirement: AI evaluation uses deterministic prompts for consistent scoring
All AI evaluation calls SHALL use temperature=0 to ensure deterministic output. Evaluation SHALL be performed after the user submits all answers by clicking "Finish Interview" — not in real-time per question. The evaluation prompt SHALL explicitly instruct the AI to evaluate algorithmic thinking over syntax.

#### Scenario: Deterministic theory evaluation
- **WHEN** an AI evaluates a theory answer submission
- **THEN** system sends the question, category, difficulty, and user's answer with temperature=0 and receives a consistent score each time

#### Scenario: Deterministic coding evaluation for Java
- **WHEN** an AI evaluates a Java code submission
- **THEN** system uses the language-specific Java prompt template (temperature=0) to evaluate algorithmic correctness and complexity

#### Scenario: Deterministic coding evaluation for Python
- **WHEN** an AI evaluates a Python code submission
- **THEN** system uses the language-specific Python prompt template (temperature=0) to evaluate algorithmic correctness and complexity

### Requirement: AI failures during evaluation are retried with exponential backoff
If an AI evaluation call fails, the system SHALL retry up to 3 times with exponential backoff delays of 5 seconds, 10 seconds, and 20 seconds respectively. If all retries fail for a question, that question's evaluation shall be marked as FAILED and visible to the user.

#### Scenario: Successful retry
- **WHEN** an AI evaluation call fails on first attempt
- **THEN** system waits 5 seconds and retries; if successful, stores the evaluation result

#### Scenario: All retries exhausted
- **WHEN** all 3 retry attempts for a question's evaluation fail due to API issues
- **THEN** system marks the evaluation as FAILED with status EVAL_FAILED and allows user to retry later
