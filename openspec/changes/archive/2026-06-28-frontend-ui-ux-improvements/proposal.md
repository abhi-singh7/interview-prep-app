## Why

The Angular frontend for AI Interview Prep uses default Material Design styling with no visual identity, no performance optimizations, and several UX gaps that reduce usability — especially during the core interview flow. Users see a generic template-like interface, have no visual feedback on progress, lack code syntax highlighting, and encounter subtle bugs (e.g., view restoration flicker) that degrade confidence in the product. These improvements make the app feel polished and production-ready without changing any backend behavior or data contracts.

## What Changes

- Replace inline `setTimeout` view-restoration hack with proper Angular change detection (`ChangeDetectorRef.detectChanges`) to eliminate interview-resume flicker.
- Add a visual progress bar above each question card during interviews (replaces plain text counter).
- Add colored topic-type badges/icons on theory vs. coding questions for quick scanning.
- Integrate syntax highlighting into code display blocks in both the coding-question component and results page, with optional line numbers.
- Add copy-to-clipboard and clear buttons above the coding textarea (action bar).
- Color-code overall score on the results page (red / amber / green thresholds) using a colored text + icon.
- Fix typo: "Weakenesses" to "Weaknesses" in `results-page.component.ts`.
- Add keyboard shortcuts for interview navigation (`ArrowRight` = Next, `ArrowLeft` = Previous, `Escape` = Skip, `Ctrl+Enter` = Submit) via a small directive on the interview page.
- Add dark-mode support with a navbar toggle and `localStorage` persistence; switch Material prebuilt theme between light (indigo-pink) and dark at runtime.
- Tighten Angular bundle budgets in `angular.json` (`initial maximumWarning: 250kb`, `maximumError: 800kb`) to surface regressions early.
- Enable a lazy-route preloading strategy so route bundles download on idle instead of on first navigation.
- Mark pure/dumb components with `changeDetection: ChangeDetectionStrategy.OnPush` (navbar, auth pages, setup, question cards, results, analytics).
- Add responsive media-query breakpoints for mobile (< 600px) on navbar, coding textarea, metrics grid, and results cards.
- Show a proper loading skeleton/spinner during analytics chart fetch instead of rendering an empty table immediately.

## Capabilities

### New Capabilities

- `ui-theming`: Dark-mode toggle with persisted preference; Material theme switching at runtime via CSS class on `<body>`.
- `code-syntax-highlighting`: Syntax-highlighted code blocks (starter code, user submissions, improved solutions) with line numbers and copy-to-clipboard button.
- `interview-accessibility`: Keyboard shortcuts for question navigation and submit during the interview flow.

### Modified Capabilities

<!-- None of the existing specs (`evaluation-persistence`, `interview-setup`, `spring-ai-integration`, `structured-output-dtos`) define UI behavior, so no requirement-level spec changes are needed here. -->

## Impact

- **Affected code**: All Angular components under `frontend/src/app/`, `angular.json` budgets, `index.html` (favicon / theme link), navbar template + styles.
- **New dependencies**: `highlight.js` (lightweight syntax highlighting).
- **No backend changes**: All work is purely frontend — no API contracts, routes, or data models change.
- **No breaking changes**: All additions are additive; existing navigation, auth flow, and interview lifecycle remain identical.
