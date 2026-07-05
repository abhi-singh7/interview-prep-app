# Component Reference — AI Interview Prep Frontend

## Overview

This document provides detailed descriptions of every component in the application, including their purpose, inputs/outputs, and how they interact with other parts of the system.

---

## Root Components

### AppComponent (`app/app.component.ts`)

**Purpose:** The root component that bootstraps the entire application. It renders the navigation bar and provides a router outlet for lazy-loaded route components.

| Property | Type | Description |
|----------|------|-------------|
| `title` | string | Application title displayed in browser tab ("AI Interview Prep") |

**Imports:** `RouterOutlet`, `NavbarComponent`

**Template Structure:**
```html
<app-navbar></app-navbar>
<router-outlet />
```

---

### NavbarComponent (`app/components/navbar.component.ts`)

**Purpose:** Top navigation bar that provides app branding, page navigation links, user info display, theme toggle, and logout functionality.

| Property | Type | Description |
|----------|------|-------------|
| `interviewRoute` | any[] | Dynamic route array — points to active session or setup page based on current state |

**Inputs:** None (reads auth state from AuthService directly)

**Outputs:** None

**Key Behaviors:**
- Dynamically updates the "Interview" link to point to the active session if one exists
- Shows/hides navigation links and logout button based on authentication state
- Displays the logged-in user's name in the navbar
- Subscribes to router events to update the interview route when navigating between pages

**Dependencies:** `AuthService`, `Router`, `ThemeToggleComponent`

---

## Authentication Components

### LoginComponent (`app/auth/login/login.component.ts`)

**Purpose:** Login form that collects email and password credentials, submits them to the backend via AuthService, and redirects authenticated users to `/interview/setup`.

| Property | Type | Description |
|----------|------|-------------|
| `email` | string | User's email input (two-way bound) |
| `password` | string | User's password input (two-way bound) |
| `isLoading` | boolean | Loading indicator for the login button |

**Outputs:** None

**Key Behaviors:**
- Validates form before submission
- Calls `AuthService.login()` with credentials
- On success, sets auth state in sessionStorage and navigates to `/interview/setup`
- Shows error messages on failed authentication

---

### RegisterComponent (`app/auth/register/register.component.ts`)

**Purpose:** Registration form that collects name, email, and password for new user accounts.

| Property | Type | Description |
|----------|------|-------------|
| `name` | string | User's display name (two-way bound) |
| `email` | string | User's email address (two-way bound) |
| `password` | string | User's password input (two-way bound) |
| `isLoading` | boolean | Loading indicator for the register button |

**Outputs:** None

**Key Behaviors:**
- Validates form before submission
- Calls `AuthService.register()` with credentials
- On success, sets auth state in sessionStorage and navigates to `/interview/setup`
- Shows error messages on failed registration (e.g., duplicate email)

---

## Interview Feature Components

### InterviewSetupComponent (`app/interview/setup-page/interview-setup.component.ts`)

**Purpose:** Configuration form for creating a new interview session. Users select programming language, topic(s), difficulty level, and question count before starting the interview.

| Property | Type | Description |
|----------|------|-------------|
| `selectedLanguage` | string | Selected programming language ID |
| `selectedTopics` | string[] | Array of selected topic IDs |
| `difficulty` | string | Selected difficulty level (EASY/MEDIUM/HARD) |
| `questionCount` | number | Number of questions to generate |

**Outputs:** None

**Key Behaviors:**
- Fetches language options from `DropdownDataService.getLanguages()` on init
- When a language is selected, fetches topics for that language via `DropdownDataService.getTopicsByLanguage()`
- Supports topic search via `DropdownDataService.searchTopics()` (requires 2+ characters)
- Calls `InterviewSignalService.startInterview()` to start the session
- On success, navigates to `/interview/:sessionId`

---

### MySessionsComponent (`app/interview/my-sessions/my-sessions.component.ts`)

**Purpose:** Displays a list of all past interview sessions for the current user. Users can click on any session to view its details or results.

| Property | Type | Description |
|----------|------|-------------|
| `sessions` | any[] | Array of session objects from the backend |

**Outputs:** None

**Key Behaviors:**
- Fetches sessions via `InterviewSignalService.getUserSessions()` on init
- Displays session info: date, difficulty, language, status
- Clicking a session navigates to `/interview/detail/:sessionId` or `/results/:sessionId`

---

### InterviewPageComponent (`app/interview/interview-page/interview-page.component.ts`)

**Purpose:** The main interview page where users answer questions. It renders either `TheoryQuestionComponent` or `CodingQuestionComponent` for each question in the session, along with navigation controls (next/previous/skip).

| Property | Type | Description |
|----------|------|-------------|
| `currentQuestionIndex` | number | Index of the currently displayed question |
| `questions` | any[] | Array of questions from InterviewSignalService |

**Outputs:** None

**Key Behaviors:**
- Loads questions via `InterviewSignalService.getQuestionsForSession()` on init
- Renders the current question using either TheoryQuestionComponent or CodingQuestionComponent based on question type
- Provides navigation: next, previous, skip buttons
- Handles keyboard shortcuts (Ctrl+Enter to submit, Arrow keys for navigation)
- Calls `InterviewSignalService.submitAnswer()` or `submitCodingSubmission()` when user submits

---

### SessionDetailComponent (`app/interview/session-detail/session-detail.component.ts`)

**Purpose:** Displays detailed information about a specific interview session, including all questions, answers, and evaluation results.

| Property | Type | Description |
|----------|------|-------------|
| `sessionId` | number | The session ID from the route parameter |
| `sessionData` | any | Session details fetched from the backend |

**Outputs:** None

**Key Behaviors:**
- Reads sessionId from route parameters via ActivatedRoute
- Fetches session data via `InterviewSignalService.getResults()` or similar endpoint
- Displays question-by-question breakdown with answers and scores

---

## Interview Sub-Components

### TheoryQuestionComponent (`app/interview/components/theory-question.component.ts`)

**Purpose:** Renders a theory-based interview question with a text area for the user's answer. Supports keyboard shortcuts (Ctrl+Enter to submit).

| Property | Type | Description |
|----------|------|-------------|
| `question` | any \| null | The question object (type: "THEORY") |
| `index` | number | Question index in the session |
| `total` | number | Total number of questions in the session |

**Outputs:** None (uses InterviewSignalService directly for submission)

**Key Behaviors:**
- Displays question title, description, and type badge
- Provides a textarea for the user's answer
- Submits via `InterviewSignalService.submitAnswer()` when user clicks submit or presses Ctrl+Enter
- Uses `KeyboardShortcutsDirective` for keyboard navigation

---

### CodingQuestionComponent (`app/interview/components/coding-question.component.ts`)

**Purpose:** Renders a coding challenge with starter code display, language selection dropdown, and a textarea for the user's code answer.

| Property | Type | Description |
|----------|------|-------------|
| `question` | any \| null | The question object (type: "CODE") |
| `index` | number | Question index in the session |
| `total` | number | Total number of questions in the session |

**Outputs:** None (uses InterviewSignalService directly for submission)

**Key Behaviors:**
- Displays question title, description, and type badge
- Shows starter code via `HighlightBlockComponent` with syntax highlighting
- Language dropdown populated from `DropdownDataService.getLanguages()`
- Provides "Copy" and "Clear" buttons for the answer textarea
- Submits via `InterviewSignalService.submitCodingSubmission()` when user submits

---

### CodeEditorComponent (`app/interview/components/code-editor.component.ts`)

**Purpose:** A wrapper component around Monaco Editor (VS Code's code editor engine). Provides syntax-highlighted, IntelliSense-enabled code editing for coding challenges.

| Property | Type | Description |
|----------|------|-------------|
| `questionId` | number | Unique ID for the question being answered |
| `codePrompt` | string | Starter code displayed in the editor |
| `language` | string (Input setter) | Programming language — triggers Monaco mode switch when changed |

**Outputs:** None (uses InterviewSignalService directly for submission)

**Key Behaviors:**
- Initializes Monaco editor instance in `ngAfterViewInit` via `requestAnimationFrame`
- Auto-switches between light (`vs`) and dark (`vs-dark`) themes based on ThemeService
- Switches language mode dynamically when the `language` input changes
- Emits code changes via EventEmitter (used for auto-save or real-time submission)
- Properly disposes all Monaco resources in `ngOnDestroy`

**Monaco Lifecycle:**
```
1. ngAfterViewInit → requestAnimationFrame → Initialize editor instance
2. effect(() => themeService.mode$()) → Auto-switch theme on change
3. language setter → Switch Monaco language mode via setModelLanguage()
4. codeChange output → Emit changes to parent component
5. ngOnDestroy → Dispose editor, model, and content subscription
```

---

## Evaluation Components

### ResultsPageComponent (`app/evaluation/results-page/results-page.component.ts`)

**Purpose:** Displays the evaluation results for a completed interview session, including scores per question, feedback messages, and overall performance summary.

| Property | Type | Description |
|----------|------|-------------|
| `sessionId` | number | The session ID from the route parameter |
| `results` | any | Evaluation data fetched from the backend |

**Outputs:** None

**Key Behaviors:**
- Reads sessionId from route parameters via ActivatedRoute
- Fetches results via `InterviewSignalService.getResults()` on init
- Displays overall score, per-question scores, and AI-generated feedback
- Provides "Retry" button for individual answers (calls `retryEvaluation()`)
- Uses ThemeService for chart colors in dark/light mode

---

## Analytics Components

### AnalyticsPageComponent (`app/analytics/analytics-page/analytics-page.component.ts`)

**Purpose:** Displays performance analytics with charts visualizing the user's interview history, score trends, topic-wise performance, and difficulty breakdown.

| Property | Type | Description |
|----------|------|-------------|
| `performanceData` | any | Analytics data fetched from the backend |

**Outputs:** None

**Key Behaviors:**
- Fetches analytics via `InterviewSignalService.getPerformanceData()` on init
- Renders multiple Chart.js charts using ng2-charts:
  - Score trend over time (line chart)
  - Topic-wise performance (bar chart)
  - Difficulty breakdown (pie/doughnut chart)
- Uses ThemeService for dynamic chart color palettes

---

## Shared Components

### QuestionTypeBadgeComponent (`app/shared/badges/question-type-badge.component.ts`)

**Purpose:** Displays a colored chip indicating whether a question is THEORY or CODING type.

| Property | Type | Description |
|----------|------|-------------|
| `type` | string (Input) | Question type: "THEORY" or "CODE"/"CODING" |

**Outputs:** None

**Key Behaviors:**
- Shows a blue chip for THEORY questions
- Shows an accent-colored chip for CODING questions
- Uses Angular Material chips with `selected` state styling

---

### HighlightBlockComponent (`app/shared/components/highlight-block.component.ts`)

**Purpose:** Renders code blocks with syntax highlighting, line numbers, and a copy-to-clipboard button. Used to display starter code in coding challenges.

| Property | Type | Description |
|----------|------|-------------|
| `code` | string (Input) | The source code to render |
| `lang` | string (Input) | Programming language for syntax highlighting |

**Outputs:** None

**Key Behaviors:**
- Uses HighlightService to highlight the code with proper syntax colors
- Generates line numbers alongside the code block
- Provides a copy-to-clipboard button that shows a checkmark after copying
- Supports dark mode styling via CSS variables
- Handles unsupported languages gracefully (falls back to plain text)

---

### ThemeToggleComponent (`app/shared/theme/theme-toggle.component.ts`)

**Purpose:** A small icon button in the navbar for toggling between light and dark themes.

| Property | Type | Description |
|----------|------|-------------|
| `isDark` | computed signal | Whether the current theme is dark mode |
| `ariaLabel` | computed signal | Accessibility label ("Switch to light/dark mode") |

**Outputs:** None (calls ThemeService.toggle() directly on click)

**Key Behaviors:**
- Shows a sun icon in dark mode, moon icon in light mode
- Uses Angular Signals (`computed`) for reactive UI updates
- Calls `ThemeService.toggle()` when clicked
- Updates aria-label dynamically based on current theme

---

## Shared Directives

### KeyboardShortcutsDirective (`app/shared/directives/keyboard-shortcuts.directive.ts`)

**Purpose:** Adds keyboard shortcut support to any element it's applied to. Used in the interview page for navigation and submission without touching the mouse.

| Property | Type | Description |
|----------|------|-------------|
| `shortcutNext` | EventEmitter<void> (Output) | Emits when user presses ArrowRight or Ctrl+Enter |
| `shortcutPrevious` | EventEmitter<void> (Output) | Emits when user presses ArrowLeft |
| `shortcutSkip` | EventEmitter<void> (Output) | Emits when user presses Escape |
| `shortcutSubmit` | EventEmitter<void> (Output) | Emits when user presses Ctrl+Enter or Cmd+Enter |

**Key Behaviors:**
- Ignores key events when the target is a textarea, input, or contenteditable element
- Handles both Ctrl+Enter (Windows/Linux) and Meta+Enter (Mac) for submission
- Prevents default browser behavior for navigation keys to avoid scrolling

---

## Shared Services

### AuthService (`app/services/auth.service.ts`)

**Purpose:** Central authentication service. Handles login, register, logout HTTP calls and manages auth state via Signals and sessionStorage.

| Property | Type | Description |
|----------|------|-------------|
| `isAuthenticated$` | signal<boolean> | Reactive flag for current auth status |
| `userName$` | signal<string \| null> | Current user's display name |

**Key Methods:**
- `register(name, email, password)` — POST to `/api/auth/register`
- `login(email, password)` — POST to `/api/auth/login`
- `logout()` — POST to `/api/auth/logout`, clears sessionStorage flags
- `isLoggedIn()` — Checks sessionStorage + verifies with backend if no local flag exists
- `getActiveSessionId()` — Returns the active interview session ID from sessionStorage

---

### InterviewSignalService (`app/services/interview-signal.service.ts`)

**Purpose:** The central hub for all interview-related state and API calls. Combines reactive Signals for state with Maps for imperative data storage.

| Property | Type | Description |
|----------|------|-------------|
| `currentSessionId` | WritableSignal\<number \| null\> | Active session ID (reactive) |
| `questions` | WritableSignal\<any[]\> | Current question list (reactive) |
| `evaluations` | WritableSignal\<any[]\> | Evaluation results (reactive) |
| `answers` | Map\<number, string\> | QuestionId → Answer text (imperative) |
| `codingSubmissions` | Map\<number, {language, code}\> | QuestionId → Submission data (imperative) |

**Key Methods:**
- `startInterview(topicIds, difficulty, count, languageId)` — POST to `/api/interview/start`
- `getResumeStatus()` — GET to `/api/interview/resume`
- `getQuestionsForSession(sessionId)` — GET to `/api/interview/session/{id}/questions`
- `submitAnswer(questionId, answerText)` — POST to `/api/interview/{sessionId}/answer`
- `submitCodingSubmission(questionId, language, code)` — POST with languageSubmitted field
- `finishInterview(sessionId)` — POST to `/api/interview/finish`
- `getResults(sessionId)` — GET to `/api/interview/results/{id}`
- `retryEvaluation(answerId)` — POST to `/api/evaluation/retry/{answerId}`
- `getPerformanceData()` — GET to `/api/analytics/performance`
- `getUserSessions()` — GET to `/api/interview/session/list`

---

### DropdownDataService (`app/services/dropdown-data.service.ts`)

**Purpose:** Provides dropdown data (languages, topics) with caching to avoid repeated API calls. Uses BehaviorSubject for reactive updates and sessionStorage for persistence across page refreshes.

| Property | Type | Description |
|----------|------|-------------|
| `languagesCache$` | BehaviorSubject\<LanguageOption[]\> \| null | Cached language options |

**Key Methods:**
- `getLanguages()` — GET to `/api/interview/languages`, caches in sessionStorage + BehaviorSubject
- `getTopicsByLanguage(langId)` — GET to `/api/interview/topics?langId={id}`
- `searchTopics(langId, searchText)` — GET with search query param (requires 2+ characters)
- `clearLanguagesCache()` — Clears both sessionStorage and BehaviorSubject cache

---

## Shared Services (Utilities)

### HighlightService (`app/shared/services/highlight.service.ts`)

**Purpose:** Provides syntax highlighting for code blocks using highlight.js. Supports TypeScript, Java, Python, and JavaScript.

| Property | Type | Description |
|----------|------|-------------|
| `languageCache` | Map\<string, boolean\> | Cache of already-highlighted languages to avoid redundant work |

**Key Methods:**
- `highlight(code, lang?)` — Returns `{ value: string (HTML), raw: string }` with highlighted code
- `getRawText(encodedCode)` — Decodes HTML entities back to original text for clipboard copy
- `getSupportedLanguages()` — Returns array of supported language keys

---

### ViewRestorerService (`app/shared/services/view-restorer.service.ts`)

**Purpose:** Utility service that applies values to child components and triggers Angular change detection. Used as a drop-in replacement for `setTimeout(() => ..., 0)` when setting ViewChild properties directly (which bypasses Angular's dirty-checking).

| Property | Type | Description |
|----------|------|-------------|
| *(none)* | — | No state, purely utility methods |

**Key Methods:**
- `applyAndRefresh(component, cdr, setter)` — Applies value and triggers CD on next tick (Promise.resolve)
- `applyAndDetect(component, cdr, setter)` — Applies value and immediately triggers CD synchronously

---

### ThemeService (`app/shared/theme/theme.service.ts`)

**Purpose:** Manages the application's dark/light theme state. Persists the choice in localStorage and broadcasts changes via Signals for reactive UI updates.

| Property | Type | Description |
|----------|------|-------------|
| `mode$` | signal\<ThemeMode\> (readonly) | Current theme mode ('light' or 'dark') |
| `isDark` | boolean getter | Whether the current mode is dark |

**Key Methods:**
- `init()` — Initializes theme from localStorage, listens for cross-tab storage events
- `toggle()` — Switches between light and dark modes
- `setMode(mode)` — Sets a specific mode (light or dark)
- `getChartPalette()` — Returns different color arrays based on current theme for Chart.js charts
- `getMetricValueColor()` — Returns the primary metric value color based on current theme

---

## Constants

### ThemeConstants (`app/shared/theme/theme.constants.ts`)

| Constant | Value | Description |
|----------|-------|-------------|
| `THEME_STORAGE_KEY` | `'ai-interview-prep-theme'` | localStorage key for theme persistence |
| `LIGHT_THEME_STYLESHEET_URL` | CDN URL for indigo-pink Material theme | Light mode stylesheet |
| `DARK_THEME_STYLESHEET_URL` | CDN URL for deeppurple-amber Material theme | Dark mode stylesheet |

---

## TypeScript Interfaces

### LanguageOption (`app/services/dropdown-data.service.ts`)

```typescript
interface LanguageOption {
  id: string;    // e.g., "java", "python"
  name: string;  // e.g., "Java", "Python"
}
```

### TopicOption (`app/services/dropdown-data.service.ts`)

```typescript
interface TopicOption {
  id: string;    // e.g., "1", "2"
  name: string;  // e.g., "Arrays", "Linked Lists"
}
```

---

## Component Interaction Map

```
AppComponent
├── NavbarComponent
│   ├── AuthService (auth state)
│   ├── ThemeToggleComponent → ThemeService
│   └── Router (navigation)
│
├── LoginComponent → AuthService.login()
├── RegisterComponent → AuthService.register()
│
├── InterviewSetupComponent
│   ├── DropdownDataService (languages, topics)
│   └── InterviewSignalService.startInterview()
│
├── MySessionsComponent → InterviewSignalService.getUserSessions()
│
├── InterviewPageComponent
│   ├── TheoryQuestionComponent × N
│   │   ├── KeyboardShortcutsDirective
│   │   └── InterviewSignalService.submitAnswer()
│   ├── CodingQuestionComponent × M
│   │   ├── DropdownDataService (languages)
│   │   ├── HighlightBlockComponent → HighlightService
│   │   └── InterviewSignalService.submitCodingSubmission()
│   └── CodeEditorComponent (Monaco Editor)
│       └── ThemeService (theme switching)
│
├── SessionDetailComponent → InterviewSignalService.getResults()
├── ResultsPageComponent → InterviewSignalService.getResults() + retryEvaluation()
└── AnalyticsPageComponent → InterviewSignalService.getPerformanceData()
    └── ThemeService (chart colors)
```
