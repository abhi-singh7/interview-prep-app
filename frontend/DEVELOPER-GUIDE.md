# Developer Guide — AI Interview Prep Frontend

## Welcome! 👋

This guide is designed to help **new and junior developers** get up to speed quickly with the AI Interview Prep frontend codebase. It covers everything from project setup to coding conventions, common patterns, and troubleshooting tips.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Project Structure Deep Dive](#project-structure-deep-dive)
3. [Coding Conventions & Patterns](#coding-conventions--patterns)
4. [Working with Services](#working-with-services)
5. [Adding New Components](#adding-new-components)
6. [Adding New Routes](#adding-new-routes)
7. [Making API Calls](#making-api-calls)
8. [State Management Patterns](#state-management-patterns)
9. [Working with Monaco Editor](#working-with-monaco-editor)
10. [Theming & Dark Mode](#theming--dark-mode)
11. [Common Tasks](#common-tasks)
12. [Troubleshooting](#troubleshooting)

---

## Getting Started

### Prerequisites Checklist

- [ ] Node.js ≥ 18.x installed ([download here](https://nodejs.org/))
- [ ] npm ≥ 9.x (comes with Node.js, verify with `npm -v`)
- [ ] Git installed and configured
- [ ] Backend server running locally (for API proxy)

### First-Time Setup

```bash
# Clone the repository (if you haven't already)
git clone <repo-url>
cd interview-app/frontend

# Install all dependencies
npm install

# Verify installation
npm list --depth=0
```

### Running the Application

```bash
# Start the Angular dev server with proxy to backend
npm start
```

This will:
1. Compile the TypeScript code
2. Start a webpack dev server on **http://localhost:4200**
3. Proxy `/api/*` requests to your backend server (configured in `proxy.conf.json`)

> 💡 **Tip**: Keep both the frontend and backend running simultaneously for full functionality.

---

## Project Structure Deep Dive

### Directory Layout

```
src/
├── index.html                    # HTML entry point — loads Angular bootstrap
├── main.ts                       # Bootstrap file — starts the app
├── monaco-worker.config.js       # Monaco Editor worker config (for code editor)
└── styles.css                    # Global CSS styles

app/
├── app.component.ts              # Root component — navbar + router outlet
├── app.config.ts                 # App-wide providers (HTTP, routing, etc.)
├── app.routes.ts                 # Route definitions with lazy loading
│
├── analytics/                    # Analytics dashboard feature
│   └── analytics-page/           # Single component for the entire page
│
├── auth/                         # Authentication feature
│   ├── login/login.component.ts  # Login form component
│   └── register/register.component.ts  # Registration form component
│
├── components/                   # Shared UI components (not feature-specific)
│   └── navbar.component.ts       # Top navigation bar
│
├── evaluation/                   # Evaluation/results feature
│   └── results-page/             # Interview results display
│
├── guards/                       # Route guards and strategies
│   ├── auth.guard.ts             # Protects routes that require login
│   └── idle-preloading.strategy.ts  # Custom preloading strategy
│
├── interceptors/                 # HTTP interceptors
│   └── csrf.interceptor.ts       # Adds CSRF token to requests
│
├── interview/                    # Interview feature (main feature)
│   ├── components/               # Reusable sub-components
│   │   ├── code-editor.component.ts  # Monaco Editor wrapper
│   │   ├── coding-question.component.ts  # Coding question display
│   │   └── theory-question.component.ts  # Theory question display
│   ├── interview-page/           # Active interview session page
│   ├── my-sessions/              # Past sessions list
│   ├── session-detail/           # Single session detail view
│   └── setup-page/               # Interview configuration form
│
├── models/                       # TypeScript interfaces/types
│
├── rules/                        # Custom webpack rule plugins
│
├── services/                     # Business logic & API calls
│   ├── auth.service.ts           # Authentication service
│   ├── dropdown-data.service.ts  # Dropdown data with caching
│   └── interview-signal.service.ts  # Central state + API hub
│
└── shared/                       # Cross-cutting utilities
    ├── badges/question-type-badge.component.ts  # Question type badge
    ├── components/highlight-block.component.ts  # Code highlight block
    ├── directives/keyboard-shortcuts.directive.ts  # Keyboard shortcuts
    ├── services/
    │   ├── highlight.service.ts           # Syntax highlighting service
    │   └── view-restorer.service.ts       # View position restoration
    └── theme/
        ├── theme-toggle.component.ts      # Dark/light mode toggle button
        ├── theme.constants.ts             # Theme color constants
        └── theme.service.ts               # Theme management service
```

### Key Files You'll Touch Often

| File | Purpose | When to Edit |
|------|---------|--------------|
| `app.routes.ts` | Add/remove routes | Adding new pages |
| `app.config.ts` | Register providers | Adding services, interceptors |
| `proxy.conf.json` | Dev server proxy config | Backend URL changes |
| `package.json` | Dependencies | Adding npm packages |

---

## Coding Conventions & Patterns

### Standalone Components (No NgModules)

All components are **standalone** — they declare their own imports directly:

```typescript
@Component({
  selector: 'app-coding-question',
  standalone: true,                          // ← Always standalone
  imports: [CommonModule, FormsModule],      // ← Declare dependencies here
  templateUrl: './coding-question.component.html',
})
export class CodingQuestionComponent { ... }
```

**Rule**: Never create an `NgModule` for this project. Use standalone components with direct imports.

### Service Injection

Services are injected via the constructor and registered app-wide using `@Injectable({ providedIn: 'root' })`:

```typescript
@Injectable({ providedIn: 'root' })  // ← Available everywhere, no module needed
export class AuthService { ... }

// In a component — just inject it in the constructor
constructor(private authService: AuthService) {}
```

### Signal-Based State Management

Use Angular Signals for reactive state. Access by calling the signal as a function:

```typescript
// Service with signals
currentSessionId = signal<number | null>(null);  // ← Create signal
sessionIdValue = this.currentSessionId();         // ← Read value (call it!)
this.currentSessionId.set(42);                    // ← Set new value

// In a component — use the service's signals
constructor(private interviewService: InterviewSignalService) {}

ngOnInit() {
  const sessionId = this.interviewService.currentSessionId();  // ← Read signal
}
```

**Rule**: Always call the signal function to read its value. Never access `.value` directly.

### Change Detection Strategy

Use `OnPush` for components that don't need frequent change detection:

```typescript
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,  // ← Optimize performance
})
export class CodingQuestionComponent { ... }
```

**Rule**: Apply `OnPush` to all presentational/dumb components. Use default strategy for container/presentational components that need frequent updates.

### Component Lifecycle Order

1. **Constructor** — Initialize dependencies, set up effects
2. **ngOnInit** — Fetch data from services/APIs
3. **ngAfterViewInit** — DOM-dependent operations (Monaco editor init)
4. **ngOnChanges** — React to input changes
5. **ngOnDestroy** — Clean up subscriptions, dispose resources

---

## Working with Services

### Creating a New Service

```typescript
// src/app/services/my-new.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })  // ← Register app-wide
export class MyNewService {
  private apiUrl = '/api/my-endpoint';

  constructor(private http: HttpClient) {}

  getData(): Observable<MyDataType> {
    return this.http.get<MyDataType>(this.apiUrl);
  }
}
```

### Using a Service in a Component

```typescript
import { MyNewService } from '../services/my-new.service';

constructor(private myService: MyNewService) {}

ngOnInit() {
  this.myService.getData().subscribe({
    next: (data) => console.log(data),
    error: (err) => console.error(err),
  });
}
```

### Service Communication Pattern

Services should **not** directly communicate with each other. Instead, use the central `InterviewSignalService` as a hub for cross-service state sharing:

```typescript
// ❌ Don't do this — services calling each other directly
constructor(private authService: AuthService) { ... }
this.authService.doSomething();  // Tight coupling!

// ✅ Do this — share state through InterviewSignalService
constructor(private interviewSignal: InterviewSignalService) { ... }
interviewSignal.currentSessionId.set(42);  // Clean, decoupled
```

---

## Adding New Components

### Step-by-Step Process

1. **Create the component file** in the appropriate feature folder:

```typescript
// src/app/my-feature/my-component.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-my-component',
  standalone: true,
  imports: [CommonModule],  // ← Add any other imports here
  templateUrl: './my-component.component.html',
  styleUrls: ['./my-component.component.scss'],
})
export class MyComponent { ... }
```

2. **Create the template** (`my-component.component.html`):

```html
<div class="my-component">
  <h3>My Component</h3>
  <!-- Your HTML here -->
</div>
```

3. **Add to routes** if it's a page-level component:

```typescript
// In app.routes.ts
{ path: 'my-feature', loadComponent: () => import('./my-feature/my-component') }
```

4. **Import in parent component** if used as a child:

```typescript
@Component({
  imports: [MyComponent],  // ← Add here
})
export class ParentComponent { ... }
```

### Component Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Component selector | `app-` prefix + kebab-case | `<app-my-component>` |
| Component class | PascalCase + "Component" suffix | `MyComponent` |
| Template file | kebab-case + `.component.html` | `my-component.component.html` |
| Style file | kebab-case + `.component.scss` | `my-component.component.scss` |

---

## Adding New Routes

### Step-by-Step Process

1. **Create the component** (see [Adding New Components](#adding-new-components))

2. **Add to routes in `app.routes.ts`**:

```typescript
// In app.routes.ts — add inside the appropriate route group
{ path: 'my-feature', canActivate: [AuthGuard], loadComponent: () => import('./my-feature/my-page') }
```

3. **Add navigation link** in the navbar component:

```html
<!-- In navbar.component.html -->
<a routerLink="/my-feature" class="nav-link">My Feature</a>
```

### Route Guard Usage

- Use `AuthGuard` for routes that require authentication
- Omit guards for public routes (login, register)
- The guard checks sessionStorage and redirects to `/login` if not authenticated

---

## Making API Calls

### Pattern 1: Using InterviewSignalService (Preferred)

This service centralizes all interview-related API calls. Use it whenever possible:

```typescript
constructor(private interviewService: InterviewSignalService) {}

startInterview() {
  this.interviewService.startInterview(topicIds, difficulty, count, languageId).subscribe({
    next: (response) => console.log('Started!', response),
    error: (err) => console.error(err),
  });
}
```

### Pattern 2: Using AuthService for Auth Calls

```typescript
constructor(private authService: AuthService) {}

login(email: string, password: string) {
  this.authService.login(email, password).subscribe({
    next: () => { /* auth state updated automatically */ },
    error: (err) => console.error(err),
  });
}
```

### Pattern 3: Direct HttpClient for New Endpoints

If you need a new API endpoint not covered by existing services:

```typescript
// In your service
getData(): Observable<MyDataType> {
  return this.http.get<MyDataType>('api/my-endpoint');
}

postData(data: MyPayload): Observable<void> {
  return this.http.post<void>('api/my-endpoint', data);
}
```

### Important Notes About API Calls

1. **All API paths start with `api/`** — the dev server proxy handles forwarding to the backend
2. **CSRF tokens are added automatically** by the interceptor for non-GET requests
3. **Auth cookies are sent automatically** by the browser (HttpOnly JWT)
4. **Always handle errors** in your subscribe callbacks

---

## State Management Patterns

### Signal-Based Reactive State

```typescript
// In a service — define reactive state
currentSessionId = signal<number | null>(null);
questions = signal<any[]>([]);

// In a component — read and react to changes
ngOnInit() {
  // Read the value by calling the signal function
  const sessionId = this.interviewService.currentSessionId();
  
  // Subscribe to changes using effect (Angular 17+)
  effect(() => {
    const questions = this.interviewService.questions();
    console.log('Questions changed:', questions.length);
  });
}

// In a component — set values
this.interviewService.currentSessionId.set(42);
```

### Map-Based Imperative State

For state that doesn't need change notifications, use Maps:

```typescript
// In InterviewSignalService
answers = new Map<number, string>();  // questionId → answerText
codingSubmissions = new Map<number, { language: string; code: string }>();

// Usage in component
this.answers.set(questionId, answerText);
const savedAnswer = this.answers.get(questionId);
```

### Dropdown Caching Pattern

The `DropdownDataService` uses a caching pattern to avoid repeated API calls:

```typescript
getLanguages(): Observable<LanguageOption[]> {
  // 1. Check BehaviorSubject cache first
  if (this.languagesCache$) return this.languagesCache$.asObservable();
  
  // 2. Check sessionStorage cache
  const cached = sessionStorage.getItem('languages_cache');
  if (cached) { /* return from session storage */ }
  
  // 3. Fetch from API and cache the result
  return this.http.get<LanguageOption[]>(`${this.apiUrl}/languages`);
}
```

---

## Working with Monaco Editor

### Overview

Monaco is the code editor engine used by VS Code. It provides syntax highlighting, IntelliSense, and a familiar developer experience for coding challenges.

### Using the CodeEditorComponent

```html
<app-code-editor
  [questionId]="1"
  [codePrompt]="starterCode"
  [language]="selectedLanguage"
  (codeChange)="onCodeChange($event)">
</app-code-editor>
```

### Key Properties & Events

| Property/Event | Type | Description |
|----------------|------|-------------|
| `questionId` | `number` | Unique ID for the question being answered |
| `codePrompt` | `string` | Starter code displayed in the editor |
| `language` | `string` | Programming language (Java, Python, etc.) |
| `codeChange` | `EventEmitter<string>` | Emits when user types in the editor |

### Important Implementation Details

1. **Monaco is initialized in `ngAfterViewInit`** — not in the constructor or `ngOnInit`. This ensures the DOM container has proper dimensions.

2. **Always dispose of Monaco resources in `ngOnDestroy`**:
   ```typescript
   ngOnDestroy(): void {
     this.editor?.dispose();    // Dispose editor instance
     this.model?.dispose();      // Dispose text model
     this.contentSubscription?.dispose();  // Dispose content change subscription
   }
   ```

3. **Theme changes are automatic** — the `effect()` in the constructor watches `themeService.mode$()` and switches Monaco's theme between `vs` (light) and `vs-dark` (dark).

4. **Language switching uses Monaco's API**:
   ```typescript
   monaco.editor.setModelLanguage(this.model, 'java');  // or 'python', etc.
   ```

### Troubleshooting Monaco Issues

| Issue | Solution |
|-------|----------|
| Editor not rendering | Check that the container div has non-zero dimensions (use `requestAnimationFrame`) |
| Font files failing to load | Ensure webpack config handles `.ttf`, `.woff`, `.woff2` as file assets |
| Syntax highlighting broken | Verify language mode is set correctly via `setModelLanguage()` |

---

## Theming & Dark Mode

### How It Works

```
User clicks toggle → ThemeService.toggle()
    ↓
Sets mode$ signal to 'dark' or 'light'
    ↓
effect() adds/removes 'dark' class on <body>
    ↓
CSS variables update via :root.dark selector
    ↓
Monaco auto-switches theme (via effect in CodeEditorComponent)
```

### Adding New Theme Colors

1. **Define the color constant** in `theme.constants.ts`:
   ```typescript
   export const MY_COLOR_LIGHT = '#3f51b5';
   export const MY_COLOR_DARK = '#8c9eff';
   ```

2. **Use it in your component**:
   ```typescript
   constructor(private themeService: ThemeService) {}
   
   getMyColor(): string {
     return this.themeService.isDark ? MY_COLOR_DARK : MY_COLOR_LIGHT;
   }
   ```

3. **Apply via inline style or CSS variable**:
   ```html
   <div [style.color]="getMyColor()">Content</div>
   <!-- OR -->
   <div class="my-element">Content</div>
   ```
   
   ```css
   .my-element { color: var(--my-color); }
   :root.dark .my-element { --my-color: #8c9eff; }
   ```

### Chart Color Palettes

The `ThemeService.getChartPalette()` method returns different colors for charts based on the current theme. Always use this instead of hardcoding chart colors:

```typescript
constructor(private themeService: ThemeService) {}

ngOnInit() {
  const palette = this.themeService.getChartPalette();
  // Use palette for your Chart.js datasets
}
```

---

## Common Tasks

### Task 1: Add a New Page with Authentication Guard

```bash
# Create the component
mkdir -p src/app/my-feature/my-page
touch src/app/my-feature/my-page/my-page.component.ts
touch src/app/my-feature/my-page/my-page.component.html
```

```typescript
// my-page.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-my-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-page.component.html',
})
export class MyPageComponent {}
```

```typescript
// app.routes.ts — add this route
{ path: 'my-feature', canActivate: [AuthGuard], loadComponent: () => import('./my-feature/my-page') }
```

### Task 2: Add a New API Endpoint to an Existing Service

```typescript
// In your service (e.g., InterviewSignalService)
private myNewEndpoint = '/api/interview/my-new-endpoint';

getMyData(id: number): Observable<MyDataType> {
  return this.http.get<MyDataType>(`${this.myNewEndpoint}/${id}`);
}

postMyData(data: MyPayload): Observable<void> {
  return this.http.post<void>(this.myEndpoint, data);
}
```

### Task 3: Add a New Route to the Navbar

```html
<!-- In navbar.component.html -->
<a routerLink="/my-feature" class="nav-link">My Feature</a>
```

### Task 4: Create a Reusable Child Component

```typescript
// my-child.component.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-my-child',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="my-child">
      <h3>{{ title }}</h3>
      <p>{{ content }}</p>
    </div>
  `,
})
export class MyChildComponent {
  @Input() title = '';
  @Input() content = '';
}
```

### Task 5: Add a New Keyboard Shortcut

1. **Add the output in `KeyboardShortcutsDirective`**:
   ```typescript
   @Output() shortcutMyAction = new EventEmitter<void>();
   
   // In onKeydown method:
   if (event.key === 'm' && event.ctrlKey) {
     event.preventDefault();
     this.shortcutMyAction.emit();
   }
   ```

2. **Bind in the component template**:
   ```html
   <div appKeyboardShortcuts (shortcutMyAction)="onMyAction()">
   ```

---

## Troubleshooting

### Common Issues and Solutions

| Problem | Cause | Solution |
|---------|-------|----------|
| `Cannot find module` errors after adding a component | Forgot to import the new component in the parent or route | Add the import to `app.routes.ts` or the parent component's `imports` array |
| Monaco editor not rendering | Container has zero dimensions at init time | Use `requestAnimationFrame()` before initializing (already done in CodeEditorComponent) |
| CSRF 403 errors on POST requests | Backend hasn't set the `_csrf` cookie yet | Check backend configuration; ensure CSRF protection is enabled and cookies are being set |
| Auth state lost after page refresh | sessionStorage cleared or not synced with backend | The `AuthService.isLoggedIn()` method auto-verifies with the backend if no local flag exists |
| Charts showing wrong colors in dark mode | Hardcoded chart colors instead of using ThemeService | Use `themeService.getChartPalette()` for all chart colors |
| Build fails with Monaco font errors | Webpack not configured to handle `.ttf`, `.woff`, `.woff2` files | Check `webpack.config.cjs` and `custom-webpack.config.cjs` have file-loader rules |
| Navigation not working after adding a route | Route path doesn't match the routerLink in navbar | Verify the path in `app.routes.ts` matches the `routerLink` attribute |

### Debugging Tips

1. **Check browser console** for Angular errors and network issues (F12 → Console tab)
2. **Check Network tab** to verify API calls are being made correctly
3. **Use `console.log()` liberally** during development — it's the fastest way to trace state changes
4. **Check sessionStorage** in DevTools → Application → Session Storage to debug auth state
5. **Verify proxy config** if API calls fail: open `proxy.conf.json` and ensure the backend URL is correct

### Useful Commands

```bash
# Run tests
npm test

# Lint code
npm run lint

# Build for production
npm run build

# Watch mode (auto-rebuild on changes)
npm run watch
```

---

## Quick Reference — Key Files by Task

| I want to... | Edit this file |
|--------------|---------------|
| Add a new page/route | `app.routes.ts` + create component folder |
| Add authentication guard | Already exists in `guards/auth.guard.ts` — just add `[AuthGuard]` to route |
| Make an API call | Create/update service in `services/` folder |
| Change the navbar | `components/navbar.component.ts` |
| Add a new theme color | `shared/theme/theme.constants.ts` + CSS |
| Add keyboard shortcuts | `shared/directives/keyboard-shortcuts.directive.ts` |
| Configure dev proxy | `proxy.conf.json` |
| Add npm dependencies | `package.json` → run `npm install` |

---

## Glossary of Terms

| Term | Definition |
|------|-----------|
| **Standalone Component** | An Angular component that doesn't require an NgModule to work |
| **Signal** | A reactive primitive in Angular 17+ for managing state (`signal()`, `computed()`) |
| **HttpOnly Cookie** | A cookie that JavaScript cannot read — used for JWT storage (more secure) |
| **CSRF Token** | A token sent with non-GET requests to prevent cross-site request forgery attacks |
| **Lazy Loading** | Loading route components only when the user navigates to them |
| **OnPush Change Detection** | An Angular optimization that skips change detection unless inputs or signals change |
| **Monaco Editor** | The code editor engine used by VS Code, embedded in this app for coding challenges |
| **NgModel** | Angular's two-way data binding directive (`[(ngModel)]`) |

---

*Last updated: 2026-07-06*
