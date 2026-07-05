# AI Interview Prep — Frontend Application

## Overview

**AI Interview Prep** is an Angular 18 web application that provides an interactive platform for users to practice technical interviews with AI-generated questions. The app supports coding challenges, theory-based questions, and analytics dashboards.

### Key Features

- **User Authentication**: Register, login, and logout with JWT token stored in HttpOnly cookies
- **Interview Setup**: Choose programming language, topic(s), difficulty level, and question count
- **Live Interview Mode**: Answer theory questions and coding challenges using Monaco Editor (VS Code's code editor)
- **Session Management**: Resume interrupted interviews, view past sessions, and see detailed results
- **Analytics Dashboard**: Visualize performance trends with Chart.js-based charts

---

## Tech Stack

| Category | Technology |
|----------|------------|
| Framework | Angular 18 (Standalone Components) |
| UI Library | Angular Material (Indigo-Pink theme) |
| Code Editor | Monaco Editor (embedded VS Code editor) |
| Charts | ng2-charts + Chart.js |
| HTTP Client | HttpClient with interceptors |
| State Management | Angular Signals (`signal()`, `WritableSignal`) |
| Routing | Lazy-loaded route modules, custom preloading strategy |
| Styling | SCSS |

---

## Project Structure

```
src/
├── index.html                    # Application entry point (HTML)
├── main.ts                       # Angular bootstrap file
├── monaco-worker.config.js       # Monaco Editor worker configuration
└── styles.css                    # Global styles
└── app/
    ├── app.component.ts          # Root component — navbar + router outlet
    ├── app.config.ts             # Application config (providers, interceptors)
    ├── app.routes.ts             # Route definitions with lazy loading
    │
    ├── analytics/
    │   └── analytics-page/       # Analytics dashboard component
    │
    ├── auth/
    │   ├── login/login.component.ts      # Login form
    │   └── register/register.component.ts  # Registration form
    │
    ├── components/
    │   └── navbar.component.ts           # Top navigation bar
    │
    ├── evaluation/
    │   └── results-page/                 # Interview results display
    │
    ├── guards/
    │   ├── auth.guard.ts                 # Route guard for authenticated access
    │   └── idle-preloading.strategy.ts   # Custom preloading strategy (idle-based)
    │
    ├── interceptors/
    │   └── csrf.interceptor.ts           # CSRF token interceptor
    │
    ├── interview/
    │   ├── components/
    │   │   ├── code-editor.component.ts  # Monaco Editor wrapper component
    │   │   ├── coding-question.component.ts  # Coding question display + submission
    │   │   └── theory-question.component.ts  # Theory question display + answer input
    │   ├── interview-page/               # Main interview session page
    │   ├── my-sessions/                  # List of past sessions
    │   ├── session-detail/               # Detailed view of a single session
    │   └── setup-page/                   # Interview configuration form
    │
    ├── models/                           # TypeScript interfaces/types
    ├── rules/                            # Custom webpack rule plugins
    ├── services/
    │   ├── auth.service.ts               # Authentication service (login, register, logout)
    │   ├── dropdown-data.service.ts      # Dropdown data with caching (languages, topics)
    │   └── interview-signal.service.ts   # Central state management via Signals + HTTP
    │
    └── shared/
        ├── badges/question-type-badge.component.ts  # Question type badge component
        ├── components/highlight-block.component.ts  # Code highlight block component
        ├── directives/keyboard-shortcuts.directive.ts  # Keyboard shortcut directive
        ├── services/
        │   ├── highlight.service.ts           # Syntax highlighting service
        │   └── view-restorer.service.ts       # View position restoration service
        ├── theme/
        │   ├── theme-toggle.component.ts      # Dark/light mode toggle
        │   ├── theme.constants.ts             # Theme color constants
        │   └── theme.service.ts               # Theme management service
```

---

## Getting Started

### Prerequisites

- **Node.js** ≥ 18.x
- **npm** ≥ 9.x (or use `npx`)

### Installation

```bash
# Navigate to the frontend directory
cd interview-app/frontend

# Install dependencies
npm install
```

### Running Locally

```bash
# Start development server with proxy configuration
npm start
```

The app will be available at **http://localhost:4200**. The `proxy.conf.json` file proxies `/api/*` requests to the backend server.

### Building for Production

```bash
npm run build
```

Build output is placed in `../backend/src/main/resources/static/browser/` (configured via `angular.json`).

---

## Configuration Files

| File | Purpose |
|------|---------|
| `angular.json` | Angular CLI configuration, output path, styles, scripts, assets |
| `package.json` | Dependencies and npm scripts |
| `tsconfig.json` / `tsconfig.app.json` | TypeScript compiler options |
| `proxy.conf.json` | Dev server proxy — forwards `/api/*` to backend |
| `webpack.config.cjs` | Custom webpack configuration (Monaco Editor loader) |
| `custom-webpack.config.cjs` | Additional custom webpack rules |

---

## Authentication Flow

1. User enters credentials on **Login** or **Register** page
2. Backend validates and sets an **HttpOnly JWT cookie** (`jwt_token`)
3. Frontend stores `isAuthenticated = true` and `user_name` in **sessionStorage**
4. Subsequent requests automatically include the HttpOnly cookie (browser handles this)
5. The **CSRF interceptor** reads `_csrf` from cookies and adds it as `X-XSRF-TOKEN` header for non-GET requests

> ⚠️ **Important**: The JWT token is HttpOnly — it cannot be read via JavaScript (`document.cookie`). Authentication state is tracked in sessionStorage after successful login/register.

---

## State Management

This project uses **Angular Signals** for reactive state management:

```typescript
// In InterviewSignalService
currentSessionId = signal<number | null>(null);   // Reactive session ID
questions = signal<any[]>([]);                     // Reactive question list
evaluations = signal<any[]>([]);                   // Reactive evaluation results
```

Signals are used instead of RxJS Subjects for simple reactive state. The `InterviewSignalService` is the central hub — all components interact with it to read/write shared state.

---

## Key Services

### AuthService (`src/app/services/auth.service.ts`)

- Handles login, register, logout HTTP calls
- Manages auth state via Signals and sessionStorage
- Provides `isAuthenticated$` signal for reactive UI updates

### InterviewSignalService (`src/app/services/interview-signal.service.ts`)

- Central state management hub (Signals + Maps)
- All interview-related API endpoints are defined here
- Tracks current session ID, questions, answers, coding submissions, and evaluations

### DropdownDataService (`src/app/services/dropdown-data.service.ts`)

- Provides language/topic dropdown options with caching
- Caches languages in sessionStorage to avoid repeated API calls
- Supports search functionality for topics

---

## Routing & Navigation

Routes are defined in `app.routes.ts` using **lazy-loaded components** (not modules):

```typescript
{ path: 'login', loadComponent: () => import('./auth/login/login.component') }
```

### Route Hierarchy

| Path | Guard | Component | Description |
|------|-------|-----------|-------------|
| `/` | — | Redirect to `/login` | Home page redirect |
| `/login` | — | LoginComponent | User login |
| `/register` | — | RegisterComponent | User registration |
| `/interview/setup` | AuthGuard | InterviewSetupComponent | Create new interview |
| `/interview/my-sessions` | AuthGuard | MySessionsComponent | View past sessions |
| `/interview/:sessionId` | AuthGuard | InterviewPageComponent | Active interview session |
| `/interview/detail/:sessionId` | AuthGuard | SessionDetailComponent | Session details |
| `/results/:sessionId` | AuthGuard | ResultsPageComponent | Evaluation results |
| `/analytics` | AuthGuard | AnalyticsPageComponent | Performance analytics |
| `**` | — | Redirect to `/login` | Catch-all redirect |

### Custom Preloading Strategy

The app uses **IdlePreloadingStrategy** instead of Angular's built-in `PreloadAllModules`. It preloads all lazy-loaded route modules after the initial render stabilizes (0ms timeout), giving instant navigation without blocking the first paint.

---

## HTTP Interceptors

### CsrfTokenInterceptor (`src/app/interceptors/csrf.interceptor.ts`)

- Reads `_csrf` token from cookies on initialization
- Adds `X-XSRF-TOKEN` header to all non-GET requests
- On 403 responses, re-reads the CSRF cookie and retries (production-ready version should auto-retry)

---

## Monaco Editor Integration

The code editor uses **Monaco Editor** (the same engine as VS Code). Key setup:

1. Monaco assets are copied via `angular.json` → `assets` section
2. Worker configuration is bundled via `monaco-worker.config.js`
3. The `code-editor.component.ts` wraps Monaco in an Angular component with proper lifecycle management

---

## Theming

The app supports **dark/light mode** toggle:

- `theme.service.ts` — Manages theme state and applies CSS classes to the body
- `theme.constants.ts` — Defines color constants for both themes
- `theme-toggle.component.ts` — UI component for switching themes

---

## Development Conventions

### Component Structure

All components are **standalone** (no NgModules). Imports are declared directly in the `@Component` decorator:

```typescript
@Component({
  selector: 'app-coding-question',
  standalone: true,
  imports: [CommonModule, MonacoEditorComponent],
  templateUrl: './coding-question.component.html',
})
export class CodingQuestionComponent { ... }
```

### Service Injection

Services are injected via the constructor. The `@Injectable({ providedIn: 'root' })` decorator makes them available app-wide without needing to declare them in any module.

### Signal Usage

- Use `signal()` for reactive state that changes over time
- Access signal values by calling the function: `this.currentSessionId()`
- Set signal values using `.set()`: `this.currentSessionId.set(42)`
- For derived/computed values, use `computed()` (not shown in current codebase)

### HTTP Calls

All API calls go through `HttpClient`. The proxy configuration (`proxy.conf.json`) handles dev-server CORS. In production, the backend serves the static files directly.

---

## Testing

```bash
# Run unit tests
npm test

# Lint the codebase
npm run lint
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Monaco Editor not loading in dev mode | Ensure `proxy.conf.json` is correctly configured and backend is running |
| CSRF errors on POST requests | Check that `_csrf` cookie is being set by the backend |
| Auth state not persisting after refresh | Verify sessionStorage is accessible; check `isAuthenticated` flag |
| Build fails with Monaco loader error | Ensure `webpack.config.cjs` has the correct file-loader for `.ttf`, `.woff`, `.woff2` extensions |

---

## Quick Reference — Backend API Endpoints (from frontend)

| Service | Method | Endpoint | Purpose |
|---------|--------|----------|---------|
| AuthService | POST | `/api/auth/register` | Register user |
| AuthService | POST | `/api/auth/login` | Login user |
| AuthService | POST | `/api/auth/logout` | Logout user |
| AuthService | GET | `/api/auth/status` | Verify auth status |
| InterviewSignalService | POST | `/api/interview/start` | Start interview |
| InterviewSignalService | GET | `/api/interview/resume` | Resume session |
| InterviewSignalService | GET | `/api/interview/session/:id/questions` | Get questions |
| InterviewSignalService | POST | `/api/interview/:sessionId/answer` | Submit answer |
| InterviewSignalService | POST | `/api/interview/finish` | Finish interview |
| InterviewSignalService | GET | `/api/interview/results/:sessionId` | Get results |
| DropdownDataService | GET | `/api/interview/languages` | Get languages |
| DropdownDataService | GET | `/api/interview/topics?langId=...` | Get topics by language |
| DropdownDataService | GET | `/api/interview/topics?langId=...&search=...` | Search topics |

---

## Contributing

1. Create a feature branch from `main`
2. Make changes following the conventions above (standalone components, Signals for state)
3. Run `npm run lint` before committing
4. Submit a pull request with a clear description of changes
