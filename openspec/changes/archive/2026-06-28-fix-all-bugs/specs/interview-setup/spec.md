## MODIFIED Requirements

### Requirement: Navigation between questions works

The system SHALL allow users to navigate forward and backward through their interview questions, with submission states properly reset after each navigation so that buttons are never left in a disabled state.

#### Scenario: Navigate to next question (answer submitted successfully)

- **WHEN** user clicks "Next" button while not on last question
- **THEN** the current answer is submitted (if one exists)
- **AND** any submission loading state (`isSubmitting`) is reset to false after successful submission
- **AND** the next question is displayed with a fresh, empty answer field
- **AND** no previous answers persist into the new question's textarea

#### Scenario: Navigate to previous question

- **WHEN** user clicks "Previous" button while not on first question
- **THEN** the previous question is displayed with its previously entered answer restored
- **AND** any submission loading state (`isSubmitting`) is reset to false after successful submission
- **AND** no answers from other questions persist into this question's textarea

#### Scenario: Coding question navigation does not cause double submission

- **WHEN** user navigates between coding questions using the Next button in the parent component
- **THEN** only one submission request is sent (no duplicate API calls)
- **AND** the answer field for the new question is empty before the user types

### Requirement: All question types display correctly

The system SHALL render THEORY questions with a text area for answers and CODE questions with a code editor, language selector, and starter code view. The CodingQuestionComponent MUST NOT contain its own submit button; all submissions must go through the parent component's Next/Finish buttons to prevent double submissions.

#### Scenario: Display theory question

- **WHEN** current question type is "THEORY"
- **THEN** the TheoryQuestionComponent renders showing the question text and an answer textarea
- **AND** the user can enter their answer as text
- **AND** no submit button exists inside the theory component itself

#### Scenario: Display coding question

- **WHEN** current question type is "CODE"
- **THEN** the CodingQuestionComponent renders showing the title, description, starter code, language selector, and a code editor
- **AND** the user can select a programming language and write code
- **AND** no submit button exists inside the coding component itself — submission occurs when the parent's Next/Finish button is clicked

#### Scenario: Coding question does not render theory component simultaneously

- **WHEN** current question type is "CODE"
- **THEN** only the CodingQuestionComponent renders; the TheoryQuestionComponent does NOT render in the DOM
- **AND** when current question type is "THEORY", only the TheoryQuestionComponent renders; the CodingQuestionComponent does NOT render in the DOM
