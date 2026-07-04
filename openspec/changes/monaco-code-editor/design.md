## Context

Coding questions currently render via a plain `<textarea>` inside `CodingQuestionComponent` (`frontend/src/app/interview/components/coding-question.component.ts:54-57`) with starter code displayed in a separate `HighlightBlockComponent`. The language dropdown (populated from `/interview/languages` API) only controls which language label is submitted — it does not affect the editing experience.

The app uses Angular 18 standalone components with OnPush change detection, Material UI, and `highlight.js` for rendering code blocks. Monaco Editor integration requires worker-based language services, which adds bundle size but delivers VS Code-quality editing in the browser.

## Goals / Non-Goals

**Goals:**
- Replace `<textarea>` with Monaco Editor for a professional coding experience
- Provide syntax highlighting + line numbers + bracket matching for Java and TypeScript/JavaScript
- Deliver type-aware IntelliSense (completions, signature help, hover) for TypeScript/JavaScript via Monaco's built-in `ts.worker`
- Provide word-based suggestions for Java (identifiers visible in the current document)
- Render starter code (`codePrompt`) as initial editor content — user edits directly on top of it (Option A)
- Dynamically switch Monaco model language when the user changes the language dropdown

**Non-Goals:**
- Code execution or test case running
- Java LSP bridge or classpath-aware IntelliSense
- Angular template (`*.html`) editing
- Multi-file support within a question
- Server-side compilation or runtime feedback

## Decisions

### 1. Use `monaco-editor` ESM directly, not the Angular wrapper

**Choice:** Import `monaco-editor` via ESM and create the editor instance imperatively in the component lifecycle. Avoid `@monaco-editor/angular`.

**Rationale:** The Angular wrapper adds an abstraction layer that can interfere with OnPush change detection and makes worker configuration harder to control. Direct ESM usage gives full control over:
- Worker bundling via `MonacoEnvironment.getWorker`
- Editor instance lifecycle (create/dispose) in `ngAfterViewInit`/`ngOnDestroy`
- Model API for syncing value changes back to Angular signals

### 2. Bundle TypeScript worker explicitly; skip Java language service

**Choice:** Configure `self.MonacoEnvironment.getWorker` to route `typescript` and `javascript` labels to the bundled `ts.worker.bundle.js`. For Java, only the Monarch grammar is needed (no worker).

**Rationale:** The TS worker (~2MB gzipped) enables real IntelliSense. Java's built-in support is Monarch-only (syntax highlighting + word completions), which requires no worker. This keeps the bundle lean — we don't pay for a Java language service that doesn't exist in Monaco.

### 3. Editor lifecycle: imperative creation in `ngAfterViewInit`

**Choice:** Create the Monaco editor instance in `ngAfterViewInit`, store it as a component field, and dispose in `ngOnDestroy`. Sync value changes via `editor.onDidChangeModelContent` with debouncing.

```
Component lifecycle:
  constructor    → inject dependencies, declare fields
  ngAfterViewInit→ monaco.editor.create(containerEl, options)
                   editor.onDidChangeModelContent(debounced sync)
  ngOnDestroy  → editor.dispose()
```

**Rationale:** Monaco's `monaco.editor.create()` requires a DOM element that exists in the view. Angular's OnPush CD doesn't track Monaco internals — we manage sync explicitly via the model API rather than relying on property bindings.

### 4. Starter code as initial model value (Option A)

**Choice:** When the question loads, set the Monaco model's value to `question.codePrompt` (if present). User edits directly in the same editor instance.

```
Model lifecycle per question:
  1. Create model with language = selectedLanguage
     value = codePrompt || ''
     uri = URI.parse(`inmemory://model/${questionId}`)
  2. Assign model to editor via editor.setModel(model)
  3. On language change → setModelLanguage(model, newLang)
  4. On question navigation → dispose old model, create new one
```

**Rationale:** Option A is simpler (one editor instance vs two), matches LeetCode/HackerRank UX, and avoids the visual complexity of a split panel. Users can delete starter code if they want — that's their choice during an interview.

### 5. Language mapping: dropdown name → Monaco language ID

**Choice:** Map backend language names to Monaco language identifiers:
- `Java` → `java`
- `Angular` → `typescript` (Angular is TypeScript under the hood)
- `Spring Boot` → `java` (Java-based framework)
- `Python` → `python`
- `SQL` → `sql`

**Rationale:** Monaco's language IDs are lowercase identifiers. Angular code IS TypeScript — Monaco's TS language service provides full IntelliSense for Angular decorators, components, and services without any extra setup. Spring Boot questions use Java syntax, so map to `java`.

### 6. Debounced sync to parent component

**Choice:** Listen to `onDidChangeModelContent` with a 300ms debounce via RxJS `debounceTime`, then emit the current editor value through an `@Output()` or directly update the `InterviewSignalService`.

**Rationale:** Every keystroke triggers a change event. Debouncing prevents excessive signal updates and backend submissions during typing. The existing `submitCodingSubmission` flow already handles final code capture — we just need to keep the in-memory value current.

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| **Bundle size increase** (~2.5MB total: 600KB core + 2MB TS worker) | Monaco is tree-shakeable; only load languages actually used. TS worker is the dominant cost but delivers real IntelliSense — acceptable trade for interview UX. |
| **OnPush change detection incompatibility** | Editor manages its own DOM updates internally. We sync values via model API, not Angular bindings. No `cdr.markForCheck()` needed for editor content changes. |
| **Java autocomplete limited to word-based suggestions** | Acceptable — the AI evaluation layer handles correctness assessment. Word-based suggestions (picking up identifiers from the current file) are sufficient for interview prep. |
| **Angular decorators not fully recognized by TS language service** | Standard TypeScript completions work. `@Component`, `@Input()` etc. appear as identifiers but won't trigger framework-aware hints. Acceptable — users know Angular APIs. |
| **Worker cross-origin issues in production** | Configure `MonacoEnvironment.getWorker` to return relative paths that resolve correctly after Angular build copies assets. The Angular build system handles this via the asset configuration. |

## Migration Plan

1. Add `monaco-editor` to `package.json` and run `npm install`
2. Create a dedicated Monaco worker bundling config (separate from existing `highlight.js`)
3. Build new `CodeEditorComponent` as standalone component with OnPush strategy
4. Wire into `InterviewPageComponent` replacing `<app-coding-question>` template usage
5. Remove `HighlightBlockComponent` if no longer used elsewhere
6. Verify build produces correct worker bundles in `dist/`

## Open Questions

- Should the Monaco editor be lazy-loaded (imported only when a CODE question is rendered) to reduce initial bundle size? This adds complexity to the component but saves ~2MB on the landing page.
- Should we expose a "reset to starter code" button so users can restore the original scaffold if they accidentally deleted it?
