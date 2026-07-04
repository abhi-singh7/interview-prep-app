## ADDED Requirements

### Requirement: User can toggle dark mode from the navbar
The application SHALL provide a visible toggle control in the top navigation bar that lets the user switch between light and dark themes at any time.

#### Scenario: Toggle dark mode on
- **WHEN** the user clicks the theme-toggle button in the navbar while the app is in light mode
- **THEN** the application switches to dark theme — background becomes dark, text becomes light, Material cards and inputs adapt their colors accordingly

#### Scenario: Toggle back to light mode
- **WHEN** the user clicks the theme-toggle button while in dark mode
- **THEN** the application switches back to light theme with the original indigo-pink palette restored

### Requirement: Dark-mode preference persists across sessions
The user's theme preference SHALL be persisted in `localStorage` under a stable key and applied automatically on next page load (before Angular bootstraps).

#### Scenario: Preference survives page refresh
- **WHEN** the user selects dark mode, navigates away, then reloads the page
- **THEN** the application renders in dark theme immediately, before any component initializes

#### Scenario: New browser tab inherits preference
- **WHEN** the user opens a new tab or window on the same origin after selecting dark mode in another tab (using `storage` event)
- **THEN** the new tab applies dark theme without requiring a manual toggle

### Requirement: Theme applies uniformly to all components
Theme switching SHALL affect every UI surface — navbar, cards, forms, tables, charts, code blocks, snack-bar, and dialog overlays.

#### Scenario: All Material components adapt
- **WHEN** the user toggles between light and dark modes
- **THEN** `mat-card`, `mat-form-field`, `mat-table`, `mat-toolbar`, `mat-chip`, and `mat-snack-bar` all update their colors within one animation frame

#### Scenario: Custom component styles adapt
- **WHEN** the user toggles theme while viewing results, analytics, or interview pages
- **THEN** inline-styled elements (progress bar background, question text color, score display) follow the theme palette

### Requirement: Theme toggle uses a semantic icon and accessible label
The navbar toggle button SHALL expose its state to assistive technology via `aria-label` that updates with the current mode.

#### Scenario: Screen reader reads correct label
- **WHEN** a screen reader focuses the theme-toggle button in light mode
- **THEN** it announces "Switch to dark mode"

#### Scenario: Label updates on toggle
- **WHEN** the user toggles to dark mode and focus returns to the button
- **THEN** it announces "Switch to light mode"
