## 1. Setup and dependencies

- [x] 1.1 Add `highlight.js` to `package.json` (`npm install highlight.js@^11`)
- [x] 1.2 Create `frontend/src/app/shared/theme/` directory with `theme.service.ts`, `theme-toggle.component.ts`, and `theme.constants.ts`
- [x] 1.3 Pre-load the saved theme in `frontend/src/main.ts` before bootstrapping Angular (read `localStorage['ai-interview-prep-theme']`, append correct stylesheet to `<head>`, add `body.dark` class if dark)
- [x] 1.4 Create a small `HighlightService` wrapper around `highlight.js` that exposes `highlight(code: string, lang?: string): string` and lazy-loads language definitions on first use

## 2. Dark mode / theming

- [x] 2.1 Wire `theme-toggle.component` into the navbar template (`app-navbar.component.ts`) next to the user name, with sun/moon icon and dynamic `aria-label`
- [x] 2.2 Add CSS custom-properties or `[body.dark]` selectors that override Material's indigo-pink palette for dark mode — cover `mat-card`, `mat-form-field`, `mat-toolbar`, `mat-table`, `mat-chip`, `mat-snack-bar`, code `<pre>` backgrounds, and chart canvas colors
- [x] 2.3 Extract hardcoded bar-chart colors in `analytics-page.component.ts:buildChartData()` to a theme-aware source so they adapt when dark mode toggles
- [x] 2.4 Add `[class.score-high]` / `[class.score-mid]` / `[class.score-low]` conditional classes on the score display in `results-page.component.ts`, with corresponding CSS (green / amber / red foreground + icon)

## 3. Interview flow polish

- [x] 3.1 Replace the plain-text progress indicator in `interview-page.component.ts` template with `<mat-progress-bar mode="determinate" [value]="(currentIndex / currentQuestions.length) * 100">`
- [x] 3.2 Add a small theme-aware chip/badge above each question card (e.g., "THEORY" blue, "CODING" green) rendered by a new `QuestionTypeBadgeComponent` used inside both `TheoryQuestionComponent` and `CodingQuestionComponent`
- [x] 3.3 Replace the `setTimeout(() => ..., 0)` answer-restoration pattern in `interview-page.component.ts:185` and `:300` with explicit `ChangeDetectorRef.detectChanges()` on the target child component via a small `ViewRestorerService`
- [x] 3.4 Mark `navbar`, `login`, `register`, `interview-setup`, `theory-question`, `coding-question`, `results-page`, and `analytics-page` components with `changeDetection: ChangeDetectionStrategy.OnPush`; audit each component's inputs to confirm references change when data changes (use `markForCheck()` where needed)

## 4. Code display enhancements

- [x] 4.1 In `coding-question.component.ts`, replace the raw `<pre><code>{{ question.codePrompt }}</code></pre>` with a new `<app-highlight-block [code]="question.codePrompt" [lang]="currentLanguage"></app-highlight-block>` wrapper component that applies syntax highlighting and adds a line-number gutter via CSS counters
- [x] 4.2 In `results-page.component.ts`, replace both `<pre><code>{{ eval.submittedCode }}</code></pre>` and `<pre><code>{{ eval.improvedAnswer }}</code></pre>` with the same `<app-highlight-block>` component, passing the appropriate code and language fields
- [x] 4.3 Add a small action bar above the user-code textarea in `coding-question.component.ts` containing a "Copy" button (uses `HighlightService.getRawText()` for clipboard) and a "Clear" button; wire copy to `navigator.clipboard.writeText(...)` with a fallback snackbar on failure
- [x] 4.4 Style line-number gutters consistently: narrow right-aligned column in muted text, separated from code by a 1px border

## 5. Keyboard shortcuts / accessibility

- [x] 5.1 Create `keyboard-shortcuts.directive.ts` that listens for `keydown` on the interview page host element; expose output events for `next`, `previous`, `skip`, and `submit`; only fire when not inside an input (or always, since these are page-level shortcuts)
- [x] 5.2 In `interview-page.component.ts`, subscribe to the directive's output events and map them to existing `moveNext()`, `prevQuestion()`, `skipQuestion()`, and a new `submitCurrentAnswer()` method; guard all handlers with `if (showEvaluating) return`
- [x] 5.3 Add a "?" help icon in the interview page header that reveals a tooltip listing "Arrow keys to navigate, Esc to skip, Ctrl+Enter to submit" on hover and focus

## 6. Performance, quality of life, and fixes

- [x] 6.1 Tighten bundle budgets in `frontend/angular.json`: change `initial maximumWarning` from `500kb` to `250kb`, `maximumError` from `1mb` to `800kb`
- [x] 6.2 In `app.config.ts`, switch the router provider from default no-preloading to custom `IdlePreloadingStrategy` (imported locally) — this preloads all lazy bundles on idle without waiting for user navigation
- [x] 6.3 Add responsive breakpoints: `@media (max-width: 600px)` rules in each component's styles covering navbar (stack links vertically or show a hamburger), coding textarea (full width, reduced padding), analytics metrics grid (single column), and results cards (stack vertically with full width)
- [x] 6.4 Add a loading state on the `analytics-page.component.ts` — show a `<mat-spinner>` while `this.analytics === null`, then render the table/chart once data arrives
- [x] 6.5 Fix the typo in `results-page.component.ts`: change both instances of "Weakenesses" to "Weaknesses"
