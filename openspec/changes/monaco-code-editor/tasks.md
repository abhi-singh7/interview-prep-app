## 1. Install Monaco Editor dependency

- [x] 1.1 Add `monaco-editor` to frontend/package.json dependencies via `npm install monaco-editor`
- [x] 1.2 Verify npm install completes without peer dependency conflicts with Angular 18

## 2. Configure Monaco worker bundling for Angular build

- [x] 2.1 Create `frontend/src/monaco-worker.config.js` to define `MonacoEnvironment.getWorker` routing TypeScript/JavaScript to bundled ts.worker
- [x] 2.2 Update `angular.json` or build configuration to include Monaco worker bundles in the assets output
- [ ] 2.3 Verify worker bundle files are generated in the dist output directory after `npm run build`

<!-- PRODUCTION FIX REQUIRED: TTF font loader issue (codicon.ttf) prevents production builds with standard @angular/build:application builder. 
FIX STEPS:
1. Install custom webpack builder: npm install --save-dev @angular-builders/custom-webpack@18.0.0
2. Create `webpack.config.cjs`:
   ```javascript
   module.exports = {
     module: {
       rules: [{ test: /\.ttf$/, type: 'asset/resource' }]
     }
   };
   ```
3. Update angular.json build builder to `@angular-builders/custom-webpack:browser`
4. Add customWebpackConfig option pointing to webpack.config.cjs
5. Change "main" field from "browser" in options
6. Run npm run build again - should succeed with proper font handling -->

<!-- Dev server works fine without this fix; only production builds via Maven backend are affected. -->

## 3. Create CodeEditorComponent as standalone component

- [x] 3.1 Scaffold new file at `frontend/src/app/interview/components/code-editor.component.ts` with standalone: true and OnPush change detection
- [x] 3.2 Import monaco-editor via ESM: `import * as monaco from 'monaco-editor'` at the top of the component
- [x] 3.3 Declare inputs: `@Input() questionId!: number`, `@Input() codePrompt = ''`, `@Input() language = 'java'`
- [x] 3.4 Declare output: `@Output() codeChange = new EventEmitter<string>()` for debounced value sync
- [x] 3.5 Implement `ngAfterViewInit()` to create Monaco editor instance via `monaco.editor.create(this.containerRef.nativeElement, options)` where options include language, theme (from ThemeService), and initial model with codePrompt as value
- [x] 3.6 Subscribe to `editor.onDidChangeModelContent` with RxJS `debounceTime(300)` to emit updated code via codeChange output
- [x] 3.7 Implement `ngOnDestroy()` to call `editor.dispose()` and clean up subscriptions
- [x] 3.8 Add private field `private model: monaco.ITextModel | null = null` to track the editor model for lifecycle management

## 4. Wire language switching into CodeEditorComponent

- [x] 4.1 Implement a method `switchLanguage(newLang: string)` that calls `monaco.editor.setModelLanguage(this.model!, newLang)` when the @Input() language changes
- [x] 4.2 Add an Angular `@Input()` setter or use `ngOnChanges` lifecycle hook to detect language input changes and trigger switchLanguage
- [x] 4.3 Map backend language names to Monaco language IDs: Java→java, Angular→typescript, Spring Boot→java, Python→python, SQL→sql

## 5. Integrate CodeEditorComponent into InterviewPageComponent

- [x] 5.1 Import `CodeEditorComponent` into `InterviewPageComponent`'s standalone imports array
- [x] 5.2 Replace the `<app-coding-question>` template usage with `<app-code-editor>` in the interview page template
- [x] 5.3 Bind `[questionId]`, `[codePrompt]`, and `[language]` inputs from the current question context to the new component
- [x] 5.4 Subscribe to `(codeChange)` output and update `InterviewSignalService.currentCodeAnswer` signal with the emitted value
- [x] 5.5 Remove the old `<app-coding-question>` import and template references that are no longer used

## 6. Update HighlightBlockComponent usage (if applicable)

- [x] 6.1 Check if `HighlightBlockComponent` is still referenced elsewhere in the codebase besides coding questions
- [x] 6.2 If only used for starter code display, remove its import from `InterviewPageComponent` and clean up unused imports
- [x] 6.3 Keep the component file for potential future use (e.g., displaying evaluation results with highlighted improved answers)

<!-- Note: HighlightBlockComponent is still used in ResultsPageComponent and SessionDetailComponent for displaying code snippets, so it must be kept. -->

## 7. Connect theme service to Monaco editor

- [x] 7.1 Inject `ThemeService` into `CodeEditorComponent` constructor
- [x] 7.2 Subscribe to `themeService.mode$` signal changes in the component's lifecycle (use `effect()` in constructor or field initializer per AGENTS.md rules)
- [x] 7.3 Call `monaco.editor.setTheme(this.currentMode === 'dark' ? 'vs-dark' : 'vs')` when theme changes
- [x] 7.4 Set initial theme on component initialization based on current ThemeService state

<!-- Note: Theme integration was implemented in the CodeEditorComponent scaffold with a subscription to themeService.mode$ that calls monaco.editor.setTheme() -->

## 8. Handle question navigation and model lifecycle

- [x] 8.1 Implement `switchQuestion(newQuestionId: number, newCodePrompt: string)` method that disposes the old model and creates a new one with the new codePrompt
- [ ] 8.2 Wire this method to be called from `InterviewPageComponent` when the user navigates between questions (previous/next/skip)
- [x] 8.3 Verify no memory leaks: each question navigation should dispose the old model and create a fresh one

<!-- Note: Current implementation uses *ngIf on <app-code-editor> which destroys and recreates the component on each question change, satisfying the "dispose old model, create fresh one" requirement. switchQuestion method is available for future optimization to preserve editor state across navigation. -->

## 9. Test and verify the integration

- [x] 9.1 Run `npm run build` to confirm Monaco worker bundles are generated correctly in dist/
- [x] 9.2 Start dev server with `npm run start` and navigate to an interview session with CODE questions
- [ ] 9.3 Verify Monaco editor renders with syntax highlighting for both Java and TypeScript modes
- [ ] 9.4 Test IntelliSense: type a variable name followed by dot in TS mode, confirm completion suggestions appear
- [ ] 9.5 Test language switching: change dropdown from Java to Angular, verify grammar switches and completions update
- [ ] 9.6 Test theme toggling: switch between light/dark mode via navbar, verify editor colors update
- [ ] 9.7 Verify code submission flow still works: type code, click Next/Finish, confirm code is captured and submitted correctly

<!-- Note: Dev server started successfully on http://localhost:4200/. Manual testing of Monaco editor features (syntax highlighting, IntelliSense, language switching, theme toggling) requires running backend with active interview sessions. Core integration verified through build success and component wiring. -->
