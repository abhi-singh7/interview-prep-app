## ADDED Requirements

### Requirement: Code blocks render with syntax highlighting
All code displays in the application — starter code in coding questions, submitted code and improved solutions on results pages — SHALL be rendered with language-appropriate syntax highlighting (keywords, strings, comments, numbers, operators).

#### Scenario: Starter code is highlighted on coding question page
- **WHEN** the user views a coding question that includes `question.codePrompt`
- **THEN** the starter-code block renders with colorized tokens for the selected programming language

#### Scenario: Submitted and improved answers are highlighted on results
- **WHEN** the user views a coding-question evaluation card on `/results/:sessionId`
- **THEN** both `eval.submittedCode` and `eval.improvedAnswer` render syntax-highlighted, using distinct background shades to differentiate user code from the model's improved answer

### Requirement: Line numbers are displayed alongside every code block
Every rendered code block SHALL show line numbers in a narrow left gutter.

#### Scenario: Line gutter is visible and correctly numbered
- **WHEN** any `<pre><code>` region is rendered via the syntax-highlighting service
- **THEN** a sequential line-number column (1, 2, 3...) appears to the left of the code, right-aligned, in muted text

### Requirement: User can copy code to clipboard with one click
Every code block SHALL include a visible "Copy" button that copies the raw (unformatted) source text to the system clipboard.

#### Scenario: Copy button copies raw source
- **WHEN** the user clicks the copy button on any code block
- **THEN** `navigator.clipboard.writeText(...)` is called with the original plain-text source and the button briefly shows a "Copied" confirmation

#### Scenario: Copy fails gracefully
- **WHEN** the clipboard API throws (e.g., insecure context)
- **THEN** the button falls back to a transient snackbar message explaining that clipboard access requires HTTPS or a secure context, without blocking the user

### Requirement: Syntax highlighting runs client-side with no server round-trip
Highlighting SHALL be performed entirely in the browser using `highlight.js` (or an equivalent bundle-able library). No API call is required to render code.

#### Scenario: Offline rendering works
- **WHEN** the network is unavailable after the initial page load
- **THEN** previously-rendered code blocks remain highlighted; newly loaded code blocks highlight client-side using the bundled `highlight.js` language definitions

### Requirement: Language auto-detection falls back gracefully
When a question does not explicitly specify a programming language, the highlighting library SHALL attempt to detect it. If detection fails or returns an unrecognized result, the block SHALL render with plain text and no styling error.

#### Scenario: Unknown language renders safely
- **WHEN** `question.codePrompt` is present but no matching `highlight.js` definition exists
- **THEN** the code renders as plain monospaced text without throwing or producing broken HTML
