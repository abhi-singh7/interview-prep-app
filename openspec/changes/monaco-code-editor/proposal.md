## Why

Coding questions currently use a plain `<textarea>` with no syntax highlighting or IntelliSense, making it difficult for users to read and write code during interviews. Replacing it with Monaco Editor (VS Code's engine) provides professional-grade editing UX — syntax highlighting, line numbers, bracket matching, and type-aware autocomplete for TypeScript/JavaScript — without adding code execution complexity.

## What Changes

- Replace the `<textarea>` in `CodingQuestionComponent` with a Monaco Editor instance
- Add `monaco-editor` as an npm dependency; configure ESM worker bundling for TypeScript/JavaScript IntelliSense
- Support two languages: **Java** (syntax highlighting + word-based suggestions) and **TypeScript/JavaScript** (full type-aware IntelliSense via built-in `ts.worker`)
- Starter code (`codePrompt`) becomes the initial content inside the editor (Option A — user edits directly on top of it)
- Dynamically switch Monaco model language based on the selected language dropdown value
- Remove the separate `HighlightBlockComponent` rendering for starter code (no longer needed)
- No code execution, no LSP bridge, no multi-file support

## Capabilities

### New Capabilities
- `code-editor-monaco`: Monaco Editor integration for Java and TypeScript/JavaScript coding questions with syntax highlighting and IntelliSense

### Modified Capabilities

<!-- None yet — this is a new capability. Existing capabilities are not changing at the spec level. -->

## Impact

- **Frontend**: `CodingQuestionComponent`, `HighlightBlockComponent` (removal), `package.json`, Angular build config for Monaco workers
- **Backend**: No changes — code submission flow unchanged, AI evaluation unchanged
- **Database**: No schema or migration changes
- **New dependency**: `monaco-editor` (~600KB gzipped core + ~2MB TS worker)
