# Architecture Brief — AI Interview Prep Frontend

## 1. System Overview

The **AI Interview Prep** frontend is an Angular 18 standalone-component application that provides a web-based platform for users to practice technical interviews with AI-generated questions. It communicates with a Spring Boot backend via REST APIs and uses JWT cookies for authentication.

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Browser (User)                         │
│                                                             │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐               │
│  │ Login    │   │ Interview│   │ Analytics│               │
│  │ Register │   │ Page     │   │ Dashboard│               │
│  └────┬─────┘   └────┬─────┘   └────┬─────┘               │
│       │              │              │                      │
│  ┌────▼──────────────▼──────────────▼──────┐               │
│  │           Angular Application            │               │
│  │                                          │               │
│  │  ┌────────────┐   ┌───────────────────┐  │               │
│  │  │ Services   │   │  State Management │  │               │
│  │  │(HTTP calls)│   │  (Signals + Maps) │  │               │
│  │  └──────┬─────┘   └───────────────────┘  │               │
│  │         │                                │               │
│  │  ┌──────▼──────────────────────────────┐ │               │
│  │  │        HTTP Interceptors            │ │               │
│  │  │    (CSRF Token, Cookie handling)    │ │               │
│  │  └─────────────────────────────────────┘ │               │
│  │                                          │               │
│  │  ┌─────────────────────────────────────┐ │               │
│  │  │         Monaco Editor (VS Code)     │ │               │
│  │  │    Embedded code editor component   │ │               │
│  │  └─────────────────────────────────────┘ │               │
│  │                                          │               │
│  │  ┌─────────────────────────────────────┐ │               │
│  │  │         ng2-charts + Chart.js       │ │               │
│  │  │    Analytics visualization          │ │               │
│  │  └─────────────────────────────────────┘ │               │
│  └──────────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────────┘
                              │
                    HTTPS / REST API
                              │
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Backend                       │
│                                                             │
│  ┌────────────┐   ┌──────────────┐   ┌──────────────────┐  │
│  │ Auth       │   │ Interview    │   │ Analytics        │  │
│  │ Service    │   │ Service      │   │ Service          │  │
│  └────────────┘   └──────────────┘   └──────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              AI Question Generator (LLM)                ││
│  └─────────────────────────────────────────────────────────┘│
│                                                             │
│  ┌──────────────┐   ┌──────────────┐                       │
│  │   MySQL DB   │   │ Redis Cache  │                       │
│  └──────────────┘   └──────────────┘                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Application Flow

### User Journey: Complete Interview Session

```
1. User visits app → Redirected to /login
2. User logs in → Auth state set (sessionStorage + HttpOnly cookie)
3. User navigates to /interview/setup → Configures interview
4. Backend generates questions → Returns question list
5. User answers questions → Theory via textarea, Code via Monaco Editor
6. User finishes interview → Backend evaluates responses
7. Results displayed at /results/:sessionId or analytics dashboard
```

### Authentication Flow

```
┌──────────┐     POST /api/auth/login      ┌─────────────┐
│  Browser │ ──────────────────────────►   │ Spring Boot │
│          │ ◄──────────────────────────   │             │
│          │    HttpOnly JWT cookie        │             │
└──────────┘                               └─────────────┘

Subsequent requests:
┌──────────┐     GET/POST /api/*           ┌─────────────┐
│  Browser │ ──────────────────────────►   │ Spring Boot │
│          │ ◄──────────────────────────   │             │
│          │    JWT cookie auto-sent       │             │
└──────────┘                               └─────────────┘
```

---

## 3. Component Hierarchy

### Root Level

```
AppComponent (app-root)
├── NavbarComponent (app-navbar)
└── RouterOutlet — lazy loads route components
    ├── LoginComponent
    ├── RegisterComponent
    ├── InterviewSetupComponent
    ├── MySessionsComponent
    ├── InterviewPageComponent
    │   └── TheoryQuestionComponent / CodingQuestionComponent
    │       └── CodeEditorComponent (Monaco Editor)
    ├── SessionDetailComponent
    ├── ResultsPageComponent
    └── AnalyticsPageComponent
```

### Key Component Relationships

```
InterviewSetupComponent
├── DropdownDataService → fetches languages & topics from API
└── InterviewSignalService → starts interview session

InterviewPageComponent (active session)
├── TheoryQuestionComponent × N questions
│   └── KeyboardShortcutsDirective (Ctrl+Enter, Arrow keys)
├── CodingQuestionComponent × M questions
│   ├── DropdownDataService → language options
│   ├── HighlightBlockComponent → starter code display
│   └── CodeEditorComponent → Monaco Editor instance
└── InterviewSignalService → submit answers, finish session

ResultsPageComponent
├── InterviewSignalService → fetch results
└── ThemeService → chart colors for dark/light mode

AnalyticsPageComponent
├── InterviewSignalService → performance data
├── ThemeService → chart palette
└── ng2-charts → Chart.js visualization
```

---

## 4. State Management Architecture

### Central Hub: InterviewSignalService

This service is the **single source of truth** for all interview-related state:

```typescript
// Reactive signals (change notifications)
currentSessionId: WritableSignal<number | null>   // Active session ID
questions: WritableSignal<any[]>                   // Current question list
evaluations: WritableSignal<any[]>                 // Evaluation results

// Imperative maps (no change notification needed)
answers = new Map<number, string>()                // QuestionId → Answer text
codingSubmissions = new Map<number, {language, code}>  // QuestionId → Submission
```

### Auth State: AuthService

```typescript
isAuthenticated$: signal<boolean>   // Reactive auth flag
userName$: signal<string | null>    // Current user name
```

### Dropdown Caching: DropdownDataService

```
Request → sessionStorage cache hit? → Return cached data
         → No → API call → Cache result in sessionStorage + BehaviorSubject
```

---

## 5. Data Flow Diagrams

### Interview Setup → Question Generation

```
InterviewSetupComponent              InterviewSignalService          Backend
       │                                   │                        │
       │   POST /api/interview/start       │                        │
       │ ──────────────────────────────►   │                        │
       │                                   │  AI generates questions│
       │                                   │ ◄──────────────────────│
       │   Question list (response)        │                        │
       │ ◄──────────────────────────────   │                        │
       │                                   │                        │
       │   Set currentSessionId signal     │                        │
       │   Set questions signal            │                        │
```

### Answer Submission Flow

```
CodingQuestionComponent              InterviewSignalService          Backend
       │                                   │                        │
       │   Monaco Editor: codeChange emit  │                        │
       │ ──────────────────────────────►   │                        │
       │                                   │                        │
       │   Map.set(questionId, submission) │                        │
       │                                   │                        │
       │   POST /api/interview/:id/answer  │                        │
       │ ──────────────────────────────►   │                        │
       │                                   │                        │
       │   Update evaluations signal       │                        │
       │ ◄──────────────────────────────   │                        │
```

### Analytics Data Flow

```
AnalyticsPageComponent              InterviewSignalService          Backend
       │                                   │                        │
       │   GET /api/analytics/performance  │                        │
       │ ──────────────────────────────►   │                        │
       │                                   │                        │
       │   Performance data (response)     │                        │
       │ ◄──────────────────────────────   │                        │
       │                                   │                        │
       │   Render charts with Chart.js     │                        │
```

---

## 6. Security Architecture

### Authentication

| Mechanism | Location | Purpose |
|-----------|----------|---------|
| HttpOnly JWT cookie | Backend sets, browser auto-sends | Session identity |
| sessionStorage flags | Frontend `AuthService` | Auth state tracking (since JWT is HttpOnly) |
| CSRF token (`_csrf`) | Cookie → `X-XSRF-TOKEN` header | Prevent cross-site request forgery |

### Key Security Notes for Developers

1. **Never** try to read the JWT cookie via JavaScript — it's HttpOnly by design
2. Auth state is tracked in sessionStorage after successful login/register
3. The CSRF interceptor handles token rotation on 403 responses (production should auto-retry)
4. All API endpoints are protected; unauthenticated users are redirected to `/login`

---

## 7. Routing Architecture

### Lazy Loading Strategy

All route components are **lazy-loaded** via `loadComponent`:

```typescript
{ path: 'analytics', loadComponent: () => import('./analytics/analytics-page') }
```

This means the analytics module is only downloaded when the user navigates to `/analytics`.

### Custom Preloading: IdlePreloadingStrategy

Instead of Angular's built-in `PreloadAllModules`, this app uses a custom strategy that preloads all lazy-loaded modules **after** the initial render stabilizes (0ms timeout). This gives instant navigation without blocking the first paint.

```
App loads → First page renders immediately → User sees content
     ↓
Idle time (0ms) → All other route bundles download in background
     ↓
Subsequent navigations are instant (bundles already cached)
```

---

## 8. Monaco Editor Integration Architecture

### Why Monaco?

Monaco is the same code editor engine as VS Code — it provides syntax highlighting, IntelliSense, and a familiar developer experience for coding challenges.

### Setup Flow

1. **Assets**: Monaco files are copied via `angular.json` → `assets` section
2. **Worker Config**: Bundled via `monaco-worker.config.js` (Monaco needs web workers)
3. **Webpack Loader**: Custom webpack config handles `.ttf`, `.woff`, `.woff2` font files

### Component Lifecycle

```
CodeEditorComponent
├── ngAfterViewInit → Initialize Monaco editor instance
├── effect(() => themeService.mode$()) → Auto-switches vs/vs-dark theme
├── language setter → Switches Monaco language mode dynamically
├── codeChange output → Emits changes via EventEmitter
└── ngOnDestroy → Dispose all Monaco resources (editor, model, subscriptions)
```

### Key Implementation Details

- The editor is initialized in `requestAnimationFrame` to ensure the container has proper dimensions
- Language switching uses Monaco's `monaco.editor.setModelLanguage()` API
- Theme changes are handled via `monaco.editor.setTheme()` triggered by Angular Signals effect

---

## 9. Theming Architecture

### How It Works

```
User clicks theme toggle → ThemeService.toggle()
    ↓
Set mode$ signal to 'dark' or 'light'
    ↓
effect() runs → Adds/removes 'dark' class on <body>
    ↓
CSS variables update via :root.dark selector
    ↓
Monaco editor auto-switches (via effect in CodeEditorComponent)
```

### Theme Constants (`theme.constants.ts`)

| Constant | Light Value | Dark Value |
|----------|-------------|------------|
| `THEME_STORAGE_KEY` | `"interview-theme"` | — |
| `LIGHT_THEME_STYLESHEET_URL` | CSS URL for light theme | — |
| `DARK_THEME_STYLESHEET_URL` | — | CSS URL for dark theme |

### Chart Color Palettes

The `ThemeService.getChartPalette()` method returns different color arrays based on the current mode, ensuring charts are readable in both themes.

---

## 10. Build & Deployment Architecture

### Dev Server (Local Development)

```
Browser → http://localhost:4200 → Angular Dev Server
                                    │
                              proxy.conf.json
                                    │
                              /api/* → Backend server
```

### Production Build

```
npm run build
    ↓
Output: ../backend/src/main/resources/static/browser/
    ↓
Backend serves static files directly (no separate frontend server needed)
```

### Custom Webpack Configuration

The app uses `@angular-builders/custom-webpack` to add webpack rules that Angular's default builder doesn't support — specifically for Monaco Editor font file handling.

---

## 11. Key Design Decisions & Rationale

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| Standalone components over NgModules | Standalone | Simpler, no circular dependencies, Angular 18 default |
| Signals for state management | `signal()` + `WritableSignal` | Reactive, fine-grained updates; Maps used where change notification isn't needed |
| Custom preloading strategy | IdlePreloadingStrategy | Better UX than `PreloadAllModules` — doesn't block first paint |
| HttpOnly JWT cookies over localStorage | Cookie-based auth | XSS protection — localStorage is vulnerable to XSS attacks |
| Monaco Editor over alternatives | Monaco (VS Code engine) | Best-in-class code editing experience for developers |
| sessionStorage over localStorage for auth | sessionStorage | Session-scoped; cleared on browser close, more secure |

---

## 12. File-by-File Quick Reference

See [COMPONENTS.md](./COMPONENTS.md) for detailed component descriptions and [API-REFERENCE.md](./API-REFERENCE.md) for API endpoint documentation.
