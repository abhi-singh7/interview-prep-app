## ADDED Requirements

### Requirement: Monaco Editor renders for coding questions
The system SHALL replace the plain `<textarea>` in `CodingQuestionComponent` with a Monaco Editor instance when rendering CODE-type questions.

#### Scenario: Editor displays on CODE question render
- **WHEN** a question has type `CODE` and the component initializes
- **THEN** a Monaco Editor instance is created in place of the previous textarea, showing syntax-highlighted code with line numbers

### Requirement: TypeScript/JavaScript provides full IntelliSense
The system SHALL configure Monaco's built-in TypeScript language worker (`ts.worker`) for JavaScript and TypeScript language modes to provide type-aware completions.

#### Scenario: TS completions activate on typed identifier
- **WHEN** the user types a variable name followed by a dot in TypeScript mode (e.g., `list.`)
- **THEN** Monaco displays completion suggestions based on inferred types, including method names and properties from visible code

#### Scenario: Signature help appears during function call
- **WHEN** the user types an opening parenthesis after a function name in TS/JS mode
- **THEN** Monaco shows signature help with parameter names and types

### Requirement: Java provides syntax highlighting and word-based suggestions
The system SHALL provide Monarch grammar-based syntax highlighting for Java code, along with word-based completion suggestions from identifiers visible in the current document.

#### Scenario: Java keywords are highlighted
- **WHEN** the user views or edits Java code in Monaco
- **THEN** Java keywords (e.g., `public`, `class`, `void`, `if`) are displayed with distinct coloring per Monarch grammar definition

#### Scenario: Word-based suggestions appear for visible identifiers
- **WHEN** the user types a dot after an identifier that is declared earlier in the same file (e.g., `myList.`)
- **THEN** Monaco suggests completion items drawn from method names and properties of that identifier as recognized by the editor

### Requirement: Dynamic language switching
The system SHALL switch the Monaco model's language ID when the user selects a different programming language from the dropdown.

#### Scenario: Switch from Java to TypeScript
- **WHEN** the user changes the language dropdown from "Java" to "Angular" (mapped to `typescript`)
- **THEN** the editor re-highlights existing content using TypeScript grammar and enables TS IntelliSense completions

#### Scenario: Switch from TypeScript to Java
- **WHEN** the user changes the language dropdown from "Angular" to "Java"
- **THEN** the editor switches back to Java grammar highlighting and disables TS-specific completions

### Requirement: Starter code as initial editor content (Option A)
The system SHALL populate the Monaco editor's initial model value with the question's `codePrompt` field when available.

#### Scenario: Editor loads starter code on mount
- **WHEN** a CODE question with a non-null `codePrompt` is loaded
- **THEN** the Monaco editor displays the starter code as its initial content, ready for direct editing by the user

#### Scenario: Empty editor when no starter code
- **WHEN** a CODE question has a null or empty `codePrompt`
- **THEN** the Monaco editor initializes with an empty model, allowing the user to write from scratch

### Requirement: Editor value syncs to parent component
The system SHALL capture the current editor content and emit it through an output mechanism so the parent `InterviewPageComponent` can submit it.

#### Scenario: Value emitted on change after debounce
- **WHEN** the user types in the Monaco editor
- **THEN** the updated code value is emitted to the parent component after a 300ms debounce, replacing the previous textarea-based capture mechanism

### Requirement: Editor disposes on navigation
The system SHALL properly dispose of the Monaco editor instance and its associated model when the component is destroyed or navigated away.

#### Scenario: No memory leak on question navigation
- **WHEN** the user navigates to a different question (changing from one CODE question to another)
- **THEN** the previous editor's model is disposed and a new model is created for the next question, with no dangling Monaco resources

### Requirement: Editor respects app theme
The system SHALL adapt to the application's current theme (light/dark mode) via Monaco's `setTheme` API.

#### Scenario: Theme change updates editor appearance
- **WHEN** the user toggles between light and dark themes using the existing theme service
- **THEN** the Monaco editor updates its color scheme accordingly without requiring a page reload
