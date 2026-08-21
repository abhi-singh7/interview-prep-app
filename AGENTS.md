# AGENTS.md — Interview Prep Platform

## Stack
- **Frontend:** Angular 18 (standalone components, lazy routes, Material UI, OnPush change detection)
- **Backend:** Spring Boot 4.0, Java 25, PostgreSQL + pgvector, Flyway migrations, JWT auth
- **AI:** Spring AI with LM Studio endpoint (`localhost:1234`) for question generation/evaluation

## Directory layout
```
backend/src/main/java/com/interviewprep/
  analytics/     — AnalyticsController, AnalyticsService (native SQL queries)
  auth/          — AuthController (register/login/logout/status)
  config/        — SecurityConfig, JwtAuthenticationFilter, SpaFallbackController, AuthenticationContext
  domain/        — Entities: User, InterviewSession, Question, Answer, Evaluation, Category + Repositories
  interview/     — InterviewController, AnswerSubmissionController, EvaluationController, LanguagesTopicsController

frontend/src/app/
  analytics/          — AnalyticsPageComponent (charts via ng2-charts)
  auth/               — LoginComponent, RegisterComponent
  components/         — NavbarComponent
  evaluation/         — ResultsPageComponent
  guards/             — AuthGuard, IdlePreloadingStrategy
  interview/          — SetupPage, InterviewPage, CodingQuestion, TheoryQuestion components
  interceptors/       — CsrfTokenInterceptor (GET uses cookie; non-GET adds X-XSRF-TOKEN header)
  services/           — AuthService (signals), InterviewSignalService, ThemeService
  shared/theme/       — ThemeService (signal-based dark/light toggle), constants
```

## Commands

```bash
# Frontend dev server (proxies /api/* → localhost:8080)
cd frontend && npm run start

# Frontend build (outputs to dist/interview-prep-frontend/)
cd frontend && npm run build

# Backend only (skips frontend-maven-plugin — no static files served)
cd backend && mvn compile -DskipTests

# Full build: install Node, npm install, ng build → copies to backend resources/static/browser/
cd backend && mvn compile  # or `mvn package` for executable jar
```

## Critical Angular rules (violating these causes silent bugs)

1. **`effect()` only in constructors or field initializers.** Never inside `ngOnInit()`, methods, or event handlers — throws NG0203.
   ```ts
   // ✅ OK: in constructor
   constructor(...) { effect(() => { ... }); }
   
   // ❌ BROKEN: ngOnInit
   ngOnInit() { effect(() => { ... }); }  // ← runtime error
   ```

2. **OnPush + async data requires signal state or explicit `markForCheck()`.** Plain property assignments inside subscribe callbacks do NOT trigger change detection on OnPush components.
   - Use signals for any state that changes from async callbacks (see `AuthService` as the pattern).
   - If you must use plain properties, inject `ChangeDetectorRef` and call `this.cdr.markForCheck()` after assignment.

3. **Frontend build output path.** Angular 18 wraps files in a `browser/` subdirectory. The Maven plugin is configured to copy them into `backend/src/main/resources/static/browser/`. If you change the Angular output path, update both the frontend-maven-plugin config AND any controller referencing the file (e.g., `SpaFallbackController`).

## Backend auth flow
- JWT stored in HttpOnly cookie named `jwt_token` (set by AuthController on login/register).
- `JwtAuthenticationFilter` extracts token from cookies; rejects requests without valid token with 403.
- `SecurityConfig`: `/auth/*` routes are permitted; all other requests require authentication.
- `SpaFallbackController` serves `index.html` for any non-API route — this is what enables SPA refresh on client-side paths like `/analytics`, `/interview/setup`.

## AI backend
- Default endpoint: `http://localhost:1234/v1` (LM Studio). Change `spring.ai.openai.base-url` in `application.properties` to use OpenAI or another compatible provider.
- Question generation uses temperature=0.7, max-tokens=20000. Evaluation uses temperature=0.0.

## Database
- PostgreSQL with pgvector extension (V1 migration).
- Flyway migrations live at `backend/src/main/resources/db/migration/`.
- Hibernate `ddl-auto=update` is on but Flyway manages schema — do not disable Flyway for production.

## Auth state pattern (use as reference)
`AuthService` exposes signals: `isAuthenticated$`, `userName$`. Call `.set()` alongside `sessionStorage.setItem()` in `setAuthState()`. Navbar reads these directly via `authService.isAuthenticated$()` — Angular tracks signal reads in templates and re-renders OnPush components automatically. This is the pattern to follow for any shared reactive state.

## Common pitfalls
- Don't call `checkAuthState()` or poll sessionStorage from route event handlers to update OnPush views — use signals instead.
- The CSRF interceptor skips GET requests entirely; authentication relies on cookies alone for reads.
- Analytics queries use native SQL with JSONB operations on PostgreSQL — these won't work on other databases.
- Session cleanup runs via `SessionTimeoutCleanupTask` (scheduled). Abandoned sessions older than `interview.session.timeout-hours` are purged automatically.

---

## Workspace Context

This project is part of a larger AI projects workspace at the parent directory. For cross-project context:

- **[Workspace README](../README.md)** — Overview of all 5 projects and shared technology patterns
- **[Workspace Architecture](../WORKSPACE-ARCHITECTURE.md)** — Cross-project architectural analysis, including Interview Prep pipeline diagrams
- **[Workspace AGENTS Guide](../WORKSPACE-AGENTS.md)** — AI agent guidance for navigating multiple projects in this workspace
