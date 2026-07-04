## Context

AI Interview Prep is an Angular 18 standalone-component app using Angular Material (indigo-pink prebuilt theme) for all UI. The app currently has:
- Inline component styles (no global CSS/SCSS file).
- Default Material theming with no customization beyond the prebuilt palette.
- Reactive forms and RxJS signals for state; `sessionStorage` for ephemeral persistence.
- Lazy-loaded route components; default `NoPreloading` strategy.
- No syntax highlighting, no dark mode, no keyboard shortcuts, no responsive breakpoints.

The goal of this change is to make the frontend feel like a finished product rather than an MVP prototype — without touching any backend code or data contracts.

## Goals / Non-Goals

**Goals:**
- Give the app a recognizable visual identity and polished feel.
- Make the interview flow easier to follow (progress, question type, keyboard nav).
- Improve perceived performance via `OnPush` change detection, preloading, and tighter budgets.
- Support developers who prefer dark mode for long study sessions.
- Add zero-cost accessibility improvements (keyboard shortcuts, ARIA labels).

**Non-Goals:**
- Redesigning the information architecture or adding new pages.
- Changing authentication, interview lifecycle, or evaluation logic.
- Introducing a state-management library (NgRx, Ngxs) — signals are sufficient here.
- Replacing Angular Material with a custom component library.
- Adding unit tests for UI components (out of scope; covered by other changes).

## Decisions

### D1: Theme switching via CSS class on `<body>` instead of Angular providers or service

**Choice**: Add/remove a `dark` class on the `<body>` element and use CSS custom properties / `[class.dark]` selectors to restyle every component. Swap the Material prebuilt theme link (`indigo-pink.css` ↔ `dark-theme.css`) at runtime by toggling stylesheet elements in `<head>`.

**Why over alternatives:**
- *Angular provider-based theming (e.g., `MatThemeService`)*: Adds a dependency and couples theme state to Angular's DI tree. The `body.dark` class works before Angular bootstraps, which matters for the flicker-free persistence requirement.
- *CSS-in-JS / runtime SCSS compilation*: Overkill; Material already ships prebuilt themes we just need to swap in/out.

**Non-goals of this decision**: We do not build a custom design system. We stay on top of Material's theme tokens rather than overriding them manually with hardcoded colors.

### D2: Syntax highlighting library — `highlight.js` (not Prism, not server-side)

**Choice**: Use `highlight.js` v11+ loaded client-side via npm (`npm i highlight.js`). Import only the languages we need (`typescript`, `java`, `python`, `javascript`) to keep bundle impact small (~50-80kb gzipped).

**Why over alternatives:**
- *Prism.js*: Smaller default bundle but heavier plugin ecosystem for line numbers and copy buttons — we'd end up with more moving parts.
- *Server-side highlight via Spring AI endpoint*: Would require a backend change, adds latency per render, and breaks offline use (see spec requirement on client-side rendering).

### D3: Keyboard shortcut implementation as an Angular directive (not global `window.addEventListener`)

**Choice**: A small standalone directive (`KeyboardShortcutsDirective`) attached to the interview page component's host element. It listens for `keydown` events with `{ once: false, passive: true }`, filters by key combo, and calls methods on a host-bound `EventEmitter`.

**Why over alternatives:**
- *Global window listener*: Would fire on every route, requiring constant route-based enable/disable logic. A directive scopes the listener to exactly where it's needed and gets torn down automatically via `ngOnDestroy`.
- *Angular's built-in `HostListener` on the component class*: Equivalent, but a separate directive is reusable and keeps the interview-page component focused on orchestration rather than event routing.

### D4: Dark-mode persistence in `localStorage` (not cookie, not backend)

**Choice**: Store `{ theme: 'dark' | 'light' }` in `localStorage` under key `ai-interview-prep-theme`. Read it in `main.ts` before bootstrapping Angular so the correct stylesheet is in place on first paint. Sync across tabs via `window.addEventListener('storage', ...)`.

**Why over alternatives:**
- *Cookie*: Unnecessary server round-trip, adds to every request header.
- *Backend user profile*: Requires backend change; preference is per-browser, not per-account.
- *SessionStorage*: Doesn't survive tab close — unacceptable for a preference users expect to persist.

### D5: `OnPush` applied selectively to leaf components only

**Choice**: Mark the following as `changeDetection: ChangeDetectionStrategy.OnPush`: `NavbarComponent`, `LoginComponent`, `RegisterComponent`, `InterviewSetupComponent`, `TheoryQuestionComponent`, `CodingQuestionComponent`, `ResultsPageComponent`, `AnalyticsPageComponent`. Leave `InterviewPageComponent` on default since it coordinates child components and uses ViewChild string assignment that OnPush would complicate.

**Why over alternatives:**
- *Apply OnPush everywhere*: The interview page is a hub that mutates answers via direct property writes on child components (e.g., `this.theoryQuestionComponent.answerText = storedAnswer`). Converting it to OnPush would require refactoring those patterns — scope creep for this change.
- *No OnPush at all*: Misses the largest performance win; Material-heavy apps see measurable frame-rate improvements with OnPush on leaf components.

### D6: Visual progress indicator — `mat-progress-bar` (not custom SVG/CSS)

**Choice**: Use Angular Material's `<mat-progress-bar mode="determinate" [value]="(currentIndex / total) * 100">` above the question card.

**Why over alternatives:**
- *Custom CSS bar*: More visual control but reinvents an accessible, theme-aware component Material already provides.
- *Circular stepper*: Overkill for a linear flow; horizontal bar maps directly to left-to-right reading order.

### D7: Score color-coding on results — conditional CSS classes (not a new chart type)

**Choice**: Add `[class.score-high]`, `[class.score-mid]`, `[class.score-low]` based on `results.overallScore >= 8 / >= 5 / < 5`. Style each class with distinct foreground color + optional icon (`check_circle`, `warning`, `error`).

**Why over alternatives:**
- *Circular progress ring*: Requires a third-party library or custom canvas/SVG work. CSS classes on existing text are zero-dependency and theme-aware.
- *Badge/chip next to score*: Less visually prominent; the score IS the hero element and should be sized/color-emphasized directly.

## Risks / Trade-offs

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| `highlight.js` adds noticeable bundle weight | Medium | Import only needed languages + use tree-shakeable ESM entry; lazy-load non-critical languages (e.g., Python) on first coding question via `import()`. |
| Dark mode breaks a Material component not designed for dark themes | Low | Test all components (`mat-table`, `mat-chip`, `canvas` charts, code `<pre>` blocks). Add targeted overrides where Material's dark palette produces low contrast. |
| `localStorage` theme read before bootstrap causes flicker if storage is corrupted | Low | Wrap `localStorage.getItem()` in try/catch; default to light on any parse error. |
| Keyboard shortcuts conflict with browser defaults (e.g., Esc closes dialogs) | Medium | Only listen inside the interview page host element, and call `event.stopPropagation()` for non-default shortcuts. Leave `Esc` passthrough when no question navigation is possible. |
| `OnPush` on a leaf component hides a bug where an `@Input` reference isn't updated | Medium | Audit each OnPush-marked component's inputs — any input that mutates without a new reference must use `ChangeDetectorRef.markForCheck()`. |
| Tighter bundle budgets cause CI failures during other feature work | Low | The tightened budgets (250kb warning) are conservative; Material alone is well under this. If future changes push us over, we'll address tree-shaking before relaxing again. |

## Migration Plan

This change is fully additive and deployable in a single release:
1. Add `highlight.js` to `package.json`, import needed languages.
2. Create theme service + toggle component, wire into navbar template.
3. Update `index.html` to load both light and dark Material stylesheets; add small inline script in `<head>` that reads `localStorage` and activates the correct one before Angular boots.
4. Apply `OnPush` to leaf components; fix any broken input bindings discovered during testing.
5. Add keyboard-shortcut directive, wire into interview page template.
6. Replace text progress counter with `mat-progress-bar`, add question-type badge chips.
7. Swap `<pre><code>` blocks in coding-question and results-page templates for a `HighlightPipe` (or wrapper component) that uses the highlight.js service.
8. Add copy-to-clipboard action bar above code textarea; add score color classes on results page.
9. Tighten budgets in `angular.json`, add mobile media queries, fix typo "Weakenesses" → "Weaknesses".
10. Build and verify — no backend changes required; same Docker/container deployment as before.

Rollback: revert the single commit — all new code is isolated to frontend files. No database or API migration needed.

## Open Questions

- **Language list for highlight.js**: Which programming languages does the backend actually emit in `question.codePrompt`? We should import only those (likely Java, TypeScript/JavaScript, Python). If the backend adds a new language later, we want to add its definition without a deploy — consider lazy-loading via dynamic `import()` keyed on language name.
- **Score thresholds**: Proposal mentions red/amber/green at 5 and 8. Confirm with product whether these match the intended pass/fail semantics, or if they should be configurable per-interview difficulty.
- **Chart dark-mode adaptation**: ng2-charts (`BaseChartDirective`) renders on `<canvas>`. Chart colors (bar backgrounds, grid lines, labels) need explicit re-application when theme toggles — currently hardcoded in `buildChartData()`. Should we extract chart colors into a small theme-aware service?
