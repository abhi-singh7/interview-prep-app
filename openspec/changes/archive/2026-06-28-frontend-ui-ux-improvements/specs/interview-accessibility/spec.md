## ADDED Requirements

### Requirement: Arrow keys navigate between interview questions
While the user is focused inside the active interview page, pressing `ArrowRight` SHALL advance to the next question (equivalent to clicking "Next"), and `ArrowLeft` SHALL move back one question (equivalent to clicking "Previous").

#### Scenario: Right arrow advances
- **WHEN** the user presses `ArrowRight` while on any question except the last
- **THEN** the current answer is stored, the next question card renders, and focus moves to the new answer input

#### Scenario: Left arrow goes back
- **WHEN** the user presses `ArrowLeft` while on any question except the first
- **THEN** the previous question card renders with its previously-stored answer restored in the input

#### Scenario: Arrow keys respect boundaries
- **WHEN** the user is on the first question and presses `ArrowLeft`, or on the last question and presses `ArrowRight`
- **THEN** no navigation occurs (no error, no scroll, no request)

### Requirement: Escape key skips the current question
While focused inside the active interview page, pressing `Escape` SHALL skip the current question and advance to the next one without submitting an answer.

#### Scenario: Escape skips with stored partial answer
- **WHEN** the user has typed a partial answer and presses `Escape`
- **THEN** the partial answer is saved in session state (so it can be restored if they navigate back), the next question renders, and focus moves to its input

#### Scenario: Escape does nothing on last question
- **WHEN** the user is on the final question (where "Finish Interview" replaces "Next")
- **THEN** `Escape` has no effect — the Finish button remains the only action

### Requirement: Ctrl+Enter submits the current answer
While focused inside any text input on the interview page, pressing `Ctrl+Enter` (or `Cmd+Enter` on macOS) SHALL submit the current question's answer and advance (or finish) as appropriate.

#### Scenario: Submit from theory textarea
- **WHEN** the user is typing in the theory answer textarea and presses `Ctrl+Enter`
- **THEN** the answer is submitted to the backend, the next question renders on success, and a snackbar confirms submission

#### Scenario: Submit from coding textarea
- **WHEN** the user is typing code and presses `Ctrl+Enter`
- **THEN** the code submission is posted with the selected language, then advances or finishes; the button's `[disabled]` state still applies (e.g., in-flight request blocks duplicate submits)

#### Scenario: Shortcut is disabled during evaluation
- **WHEN** the "Evaluating your answers..." spinner is displayed after Finish is clicked
- **THEN** `Ctrl+Enter`, arrow keys, and Escape are all ignored until results load

### Requirement: Keyboard shortcuts have visible affordance
Users SHALL be able to discover that keyboard navigation exists. A small help indicator (e.g., a "?") in the interview page header SHALL reveal a tooltip or popover listing the available shortcuts.

#### Scenario: Help tooltip lists shortcuts
- **WHEN** the user hovers over or focuses the help icon on the interview page
- **THEN** a tooltip displays: "Arrow keys to navigate, Esc to skip, Ctrl+Enter to submit"

#### Scenario: Tooltips persist while focused
- **WHEN** the user tabs into the help icon via keyboard
- **THEN** the tooltip remains visible (not just on hover) so keyboard users can discover the shortcuts
