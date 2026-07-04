# Interview Prep Platform — Learning Guide
## Mastering Angular 18 + Spring Boot + Spring AI 2.0 Through Your Own Codebase

---

## Part A: Frontend (Angular 18) — Concept Map to Your Code

### Roadmap Overview

```
Phase 1: Foundations (you likely know these from Angular 8)
├── Standalone Components (NEW in 14+, matured in 18)
├── Functional Config (provideRouter, provideHttpClient)
└── Template Control Flow (@if / @for — replaces *ngIf / *ngFor)

Phase 2: Reactive State (the biggest Angular 16-18 shift)
├── Signals (signal(), computed(), effect())
├── WritableSignal + Maps pattern
└── When signals vs Observables vs BehaviorSubjects

Phase 3: Architecture Patterns
├── OnPush Change Detection + Signal Integration
├── Lazy Loading with loadComponent()
├── Functional Guards & Interceptors
├── Hybrid Signals + RxJS (Observables for HTTP, Signals for state)
└── Dependency Injection (inject() + field initializer DI)

Phase 4: Angular Material UI
├── Standalone module imports in components
├── Form handling with MatFormField / MatSelect / MatAutocomplete
└── Tables, Chips, Cards, Snack bars, Tooltips

Phase 5: Advanced Patterns
├── effect() for side effects (chart re-rendering on theme change)
├── computed() signals for derived state
├── Cross-tab synchronization via StorageEvent
├── Imperative component mutation + ChangeDetectorRef
└── Custom directives with @HostListener
```

---

### Phase 1: Standalone Components — The Biggest Architectural Shift from Angular 8

**What changed:** In Angular 8, every component lives inside an NgModule. You declare it there, import shared modules in that NgModule, and the module is the unit of compilation.

In Angular 18 (your code), **NgModules are gone**. Every component is `standalone: true` and declares its own imports directly:

```
┌───────────────────────────────────────────┐
│  OLD (Angular 8):                         │
│                                           │
│  @NgModule({                               │
│    declarations: [LoginComponent],         │
│    imports: [MatCardModule, MatFormField]  │
│  })                                       │
│  export class AuthModule {}               │
│                                           │
│  AppComponent ──▶ AuthModule ──▶ ...      │
└───────────────────────────────────────────┘

┌───────────────────────────────────────────┐
│  NEW (Angular 18, YOUR CODE):             │
│                                           │
│  @Component({                              │
│    standalone: true,                       │
│    imports: [MatCardModule, MatFormField], │
│    template: `...`                         │
│  })                                       │
│  export class LoginComponent {}           │
│                                           │
│  AppComponent ──▶ LoginComponent (self)   │
└───────────────────────────────────────────┘
```

**Where to study in your code:**

| File | What It Teaches You |
|------|---------------------|
| `app.config.ts` | Functional bootstrap — `bootstrapApplication(AppComponent, { providers: [...] })` replaces `@NgModule.bootstrap` and `AppModule.providers`. Shows how to register interceptors, guards, and routes as multi-providers. |
| `app.routes.ts` | Modern route config with `loadComponent()` lazy loading (replaces `loadChildren: () => import().then(m => m.Module)`). Also shows `provideRouter(routes, withPreloading(IdlePreloadingStrategy))`. |
| Any `*.component.ts` file | Every component has `standalone: true`, explicit `imports: [...]` array listing Material modules it uses. Compare this across components to see which Material pieces each page needs. |

**Key insight:** The imports array on a standalone component IS its dependency declaration. If your template references `<mat-card>`, you MUST add `MatCardModule` to that component's imports array — there is no NgModule doing it for you anymore.

---

### Phase 2: Signals — The Reactive State Revolution

This is the single most important Angular 18 concept. Signals replace a large portion of what you'd do with BehaviorSubjects, ngrx/store, or plain RxJS Subjects.

#### 2a. Read-Only Signal (`signal<T>()`)

A signal is a function that returns its value when called: `value()` reads it, `setValue(newValue)` writes it.

```
┌──────────────────────────────────────────────────────┐
│  AuthService signals (your code):                    │
│                                                      │
│  isAuthenticated$ = signal<boolean>(false)            │
│  userName$        = signal<string|null>(null)         │
│                                                      │
│  Template usage:                                     │
│    <button *ngIf="authService.isAuthenticated$()">    │
│       Welcome, {{ authService.userName$() }}          │
│    </button>                                         │
│                                                      │
│  Key point: Angular's template compiler TRACKS       │
│  signal reads. When isAuthenticated$.set(true) is     │
│  called, Angular knows exactly which templates need   │
│  re-rendering. No change detection cycle needed for   │
│  OnPush components.                                  │
└──────────────────────────────────────────────────────┘
```

**Where to study:** `frontend/src/app/services/auth.service.ts` — the canonical pattern of exposing auth state as signals while HTTP calls remain Observables.

#### 2b. Writable Signal (`WritableSignal<T>`)

Used when a service owns mutable application state:

| Service | Signals Used | Purpose |
|---------|-------------|---------|
| `InterviewSignalService` | `currentSessionId`, `questions[]`, `evaluations[]` as writable signals; `Map<number, string>` for answers/coding submissions | The session's full state lives in one service — questions are replaced wholesale with `.set()`, while answers are mutated inside a Map (no reference change needed) |
| `ThemeService` | `_mode: WritableSignal<'dark'|'light'>` internal, exposed as readonly `mode$` signal | Theme is toggled by the component; consumers only read via `mode$()` |

**Key pattern — Maps for mutable collections:** When you need to update individual entries without replacing the whole array (which would trigger a signal re-render), use a plain JS Map. The signal's reference doesn't change, but the map's contents mutate. This is used in `InterviewSignalService` for storing answers:

```
┌─────────────────────────────────────┐
│  Why Maps instead of signals?       │
│                                     │
│  signal<any[]>([a, b])              │
│    .set([a, b, c])   → re-renders  │ ← new reference = change detected
│                                     │
│  map.set(key, value)                │
│    → NO re-render (no new ref)      │ ← mutation only
│                                     │
│  Use signal for: "the list itself   │
│   changed"                          │
│  Use Map for:     "entries inside   │
│   the list changed"                 │
└─────────────────────────────────────┘
```

#### 2c. Computed Signal (`computed()`)

A derived signal — automatically recalculates when its dependencies change:

**Where to study:** `frontend/src/app/shared/theme/theme-toggle.component.ts`
```ts
isDark = computed(() => this.themeService.mode$() === 'dark');
ariaLabel = computed(() => 
  this.themeService.isDark() ? 'Switch to light mode' : 'Switch to dark mode'
);
```

This is the reactive equivalent of `select()` in RxJS or a getter, but it's tracked by Angular's change detection. When the theme signal changes, these computed signals update automatically before any template reads them.

#### 2d. Effect (`effect()`) — Side Effects from Signal Changes

`effect()` runs whenever its dependency signals change. It's for **side effects** (things that happen outside Angular's reactive graph): DOM manipulation, logging, rebuilding charts, toggling CSS classes.

```
┌───────────────────────────────────────────────────────┐
│  effect() RULE: ONLY in constructors or field         │
│  initializers. NEVER inside ngOnInit, methods, or     │
│  event handlers.                                      │
│                                                       │
│  Why? Angular tracks effect() dependency reads at     │
│  creation time (in constructor). If you call it in    │
│  ngOnInit(), the tracking is broken and changes won't │
│  be detected reliably.                                │
└───────────────────────────────────────────────────────┘
```

**Where to study — two examples in your code:**

1. **AnalyticsPageComponent** (constructor): Rebuilds chart data whenever theme changes:
   ```ts
   constructor(...) {
     effect(() => {
       const mode = this.themeService.mode$();
       // ... rebuild charts with new theme colors
     });
   }
   ```

2. **ThemeService** (field initializer): Toggles the `dark-mode` CSS class on `<body>` whenever mode signal changes:
   ```ts
   constructor() {
     effect(() => {
       const isDark = this._mode();
       document.body.classList.toggle('dark-mode', isDark);
     });
   }
   ```

---

### Phase 3: Architecture Patterns

#### 3a. OnPush Change Detection + Signal Integration

```
┌─────────────────────────────────────────────────────────────┐
│  Angular CD strategies compared:                            │
│                                                             │
│  Default (your HighlightBlockComponent):                    │
│    Checks on: every event, timer, XHR, WebSocket            │
│    Pros: simple. Cons: expensive on large apps              │
│                                                             │
│  OnPush (your Navbar, Login, Register, Results, etc.):      │
│    Checks on: only when signal changes OR @Input ref changes│
│           OR cdr.markForCheck() / detectChanges() called    │
│    Pros: dramatically fewer change detection cycles         │
│    Cons: must use signals or markForCheck for async state   │
│                                                             │
│  KEY INSIGHT — Angular 16+: OnPush + signal reads in       │
│  template = automatic CD. The framework tracks which        │
│  templates read which signals and re-renders them when the  │
│  signal changes, even on OnPush components.                 │
└─────────────────────────────────────────────────────────────┘
```

**Where to study:** Compare `NavbarComponent` (OnPush, reads signals in template) vs `ResultsPageComponent` (OnPush but uses `cdr.markForCheck()` after async load). The Navbar doesn't need markForCheck because signal reads in templates trigger CD. ResultsPage loads data via HttpClient Observable — the subscription callback mutates a plain property, so it needs explicit markForCheck.

#### 3b. Hybrid Signals + Observables Pattern

Your app uses **both** patterns together:

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   HTTP Call (Observable)         State Update (Signal)      │
│   ───────────────────            ───────────────────        │
│                                                             │
│   authService.login(email, pw)  →  .subscribe()             │
│     .pipe(                                                    │     this.isAuthenticated$.set(true)
│       map(res => res.json()))                                │     this.userName$.set(res.name)
│     .subscribe({                                               this.sessionStorage.setItem(...)
│       next: (res) => {                                        }
│         this.setAuthState(true, res.name);                    │
│       },                                                     │
│       error: ...                                             │
│     })                                                      │
│                                                             │
│   Why two paradigms?                                        │
│   • Observables = one-time events (HTTP responses)          │
│   • Signals = persistent state (isAuthenticated, mode)      │
│   • Signals in templates = automatic OnPush CD              │
└─────────────────────────────────────────────────────────────┘
```

**Where to study:** `auth.service.ts` — the `login()` method returns an Observable but internally sets signal state. The template reads signals, not Observables. This is the canonical Angular 18 pattern.

#### 3c. Lazy Loading with loadComponent()

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   OLD (Angular 8):                                          │
│   loadChildren: () => import('./auth/login.module')        │
│                  .then(m => AuthModule)                     │
│                                                             │
│   NEW (your code, app.routes.ts):                           │
│   {                                                          │
│     path: 'login',                                           │
│     loadComponent: () => import('./auth/login/login'),       │
│   }                                                         │
│                                                             │
│   You load the COMPONENT directly — no module layer.        │
│   Angular bundles it as a separate chunk.                   │
│                                                             │
│   Plus your custom IdlePreloadingStrategy preloads all      │
│   lazy chunks after initial render (0ms setTimeout), so     │
│   navigation to any route is instant.                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 3d. Functional Guards & Interceptors

**Guards:** Your `AuthGuard` uses the new functional pattern:

```ts
// auth.guard.ts — NOT a class with canActivate() method
export const AuthGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  if (!sessionStorage.getItem('isAuthenticated')) {
    return router.createUrlTree(['login'], { queryParams: { returnUrl: state.url } });
  }
  return true;
};
```

**Interceptors:** Your `CsrfTokenInterceptor` reads the CSRF cookie and adds `X-XSRF-TOKEN` header to non-GET requests. It's registered as a multi-provider in `app.config.ts`:

```ts
{ provide: HTTP_INTERCEPTORS, useClass: CsrfTokenInterceptor, multi: true }
```

---

### Phase 4: Angular Material UI — Components Used in Your App

| Component | Where Used | Concept to Learn |
|-----------|-----------|------------------|
| `MatCardModule` | Login, Register, Setup, InterviewPage, Results, Analytics, CodingQuestion, TheoryQuestion | Card layout with header/content/actions sub-elements |
| `MatFormFieldModule` + `MatInputModule` | Login, Register, Setup | Form field wrapping inputs with floating labels and validation errors (`<mat-error>`) |
| `MatSelectModule` | SetupPage (language/topic), CodingQuestion (language) | Dropdown selects with reactive forms binding |
| `MatAutocompleteModule` | SetupPage (topic search) | Autocomplete with valueChanges subscription for filtering |
| `MatChipsModule` | SetupPage (selected topics), ResultsPage, QuestionTypeBadge | Selectable chips that show/hide based on model state |
| `MatButtonModule` / `MatIconModule` | Almost every component | Buttons and icons; note how `mat-icon-button` is used in ThemeToggle |
| `MatSnackBarModule` | Login, Register, Setup, InterviewPage, ResultsPage | Toast notifications — `snackBar.open(message, action)` with optional duration |
| `MatProgressBarModule` | InterviewPage | Progress indicator during question transitions |
| `MatProgressSpinnerModule` | ResultsPage, AnalyticsPage | Loading spinner while results fetch |
| `MatTooltipModule` | InterviewPage | Tooltip on skip button |
| `MatTableModule` | AnalyticsPage | Material data table with column definitions for session history |

**Key pattern in your code:** Every component imports only the specific Material modules it needs (e.g., Login only imports `MatCardModule`, `MatFormFieldModule`, `MatInputModule`, `MatButtonModule`). This is not NgModule — it's per-component imports. The Angular compiler tree-shakes unused Material components out of the bundle.

---

### Phase 5: Advanced Patterns in Your Codebase

#### 5a. Custom Directive with @HostListener

**File:** `frontend/src/app/shared/directives/keyboard-shortcuts.directive.ts`

```ts
@Directive({ selector: '[appKeyboardShortcuts]', standalone: true })
export class KeyboardShortcutsDirective {
  @Output() shortcutNext = new EventEmitter<void>();
  @Output() shortcutPrevious = new EventEmitter<void>();
  
  @HostListener('keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'ArrowRight') this.shortcutNext.emit();
    // ...
  }
}
```

**Concept to learn:** Directives attach behavior to existing DOM elements. `@HostListener` listens for events on the host element (or document) and re-emits them as custom outputs that parent components can bind to in templates.

#### 5b. Cross-Tab Theme Sync via StorageEvent

**File:** `frontend/src/app/shared/theme/theme.service.ts`

When theme changes in one tab, other tabs of the same app detect it via the browser's `storage` event:
```ts
ngOnInit() {
  window.addEventListener('storage', (e) => {
    if (e.key === 'theme-mode') this.setMode(e.newValue as 'dark'|'light');
  });
}
```

**Concept to learn:** The Web Storage API fires `storage` events on OTHER tabs (not the tab that made the change). This is how cross-tab state sync works without polling.

#### 5c. Imperative Child Component Mutation + ChangeDetectorRef

This is an unusual but instructive pattern in your interview flow:

```
┌───────────────────────────────────────────────────────┐
│  InterviewPageComponent (parent)                      │
│       │                                               │
│       ├── @ViewChild(TheoryQuestionComponent) theoryQ │
│       └── @ViewChild(CodingQuestionComponent) codingQ │
│                                                     │
│  Problem: Parent needs to set values on child        │
│  components imperatively (not through @Input).        │
│                                                     │
│  Solution in your code:                               │
│    1. viewRestorer.applyAndRefresh(theoryQ) sets      │
│       the question data on the child directly         │
│    2. cdr.markForCheck() / detectChanges() forces     │
│       OnPush CD to pick up the change               │
│                                                     │
│  Why this pattern? Because the interview page        │
│  controls the entire flow sequentially — it's not a  │
│  typical parent-child data flow.                      │
└───────────────────────────────────────────────────────┘
```

**File:** `frontend/src/app/shared/services/view-restorer.service.ts` — wraps the markForCheck/detectChanges pattern into a reusable function.

#### 5d. Field Initializer DI + Forms as Class Fields

Angular 16+ allows dependency injection in field initializers:

```ts
// login.component.ts
loginForm = this.fb.group({ ... });  // fb is injected via constructor, 
                                      // form is declared as a class field initializer

// auth.guard.ts — inject Router without constructor:
export const AuthGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);   // ← inject() in function body!
};

// theme.service.ts — inject DOCUMENT token:
constructor(private readonly doc: Document) {} 
// Or sometimes: private doc = inject(DOCUMENT);
```

**Concept to learn:** `inject()` allows DI without constructor parameters. Useful for guards, services where you need conditional injection, or when extending base classes that have their own constructors.

---

## Part B: Backend (Spring Boot 4 + Spring AI 2.0) — Concept Map

### Roadmap Overview

```
Phase 1: Spring Boot Foundations
├── Layered Architecture (Controller → Service → Repository → Entity)
├── Dependency Injection & @Bean Configuration
└── Configuration via application.properties + @Value / @ConfigurationProperties

Phase 2: Data Layer
├── JPA Entities with Hibernate mappings
├── Spring Data Repositories (derived queries, JPQL, native SQL)
├── pgvector integration via @JdbcTypeCode(SqlTypes.VECTOR)
└── Flyway Database Migrations

Phase 3: Security
├── SecurityFilterChain configuration
├── Cookie-based JWT auth flow
├── JwtAuthenticationFilter (OncePerRequestFilter)
├── AuthenticationContext (request-scoped auth state)
└── CSRF handling for cross-origin SPAs

Phase 4: Spring AI 2.0 (the new frontier)
├── ChatClient + ChatModel beans
├── Native Structured Output with Java Records
├── Prompt engineering patterns in service layer
├── EmbeddingModel for vector search
└── Dual temperature strategy (creative vs deterministic)

Phase 5: Production Patterns
├── Scheduled cleanup tasks (@Scheduled)
├── Exception handling (@ControllerAdvice + @ExceptionHandler)
├── Resilience patterns (zero-vector fallback, per-answer try/catch)
├── Functional SPA fallback controller
└── JSONB storage for AI-generated feedback
```

---

### Phase 1: Spring Boot Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  REQUEST FLOW                                               │
│                                                             │
│  Browser ──▶ Controller (AuthController, InterviewControl-  │
│              ler, etc.)                                     │
│           ▶ Service (InterviewSetupService, AiGatewayServ-   │
│             ice, etc.)                                      │
│           ▶ Repository (JpaRepository interface)            │
│           ▶ Entity (JPA @Entity class)                      │
│           ▶ Database (PostgreSQL + pgvector)                │
│                                                             │
│  RESPONSE FLOW:                                             │
│  Entity ──▶ Service maps to DTO/Record ──▶ Controller      │
│             returns as HTTP response body                   │
└─────────────────────────────────────────────────────────────┘
```

**Your code's structure is slightly non-standard:** instead of a single `service` package, services are distributed by domain:

| Package | Contains |
|---------|----------|
| `interview/InterviewSetupService.java` | Session creation, topic resolution |
| `interview/QuestionGenerationService.java` | AI call → entity mapping → embedding → persistence |
| `interview/InterviewResultService.java` | Aggregates evaluations into results response |
| `question/AiGatewayService.java` | **The Spring AI bridge** — prompt construction, ChatClient calls, structured output parsing |
| `embedding/EmbeddingService.java` | Thin wrapper around EmbeddingModel |
| `analytics/AnalyticsService.java` | Native SQL queries for performance data |

This is a **modular-by-domain** layout rather than strict layered. Each domain owns its services. This is more common in larger Spring Boot apps than a single service package.

---

### Phase 2: Data Layer — JPA, Repositories, pgvector

#### 2a. Entity Relationships (your domain model)

```
┌──────────┐     1:N     ┌──────────────┐     1:N     ┌─────────┐
│  User    │────────────▶│ Interview-   │────────────▶│Question │
│          │             │ Session      │             │         │
│ - email  │  LAZY       │ - status     │  LAZY       │ - type  │
│ - pwHash │◀────────────│ - difficulty │◀────────────│ - text  │
└──────────┘            └──────────────┘             └────┬────┘
                                                         │ 1:1
                                                    ┌────▼─────┐     1:N
                                                    │  Answer  │────────▶┌──────────┐
                                                    │          │           │Evaluation│
                                                    │ - text   │           │ - score  │
                                                    │ - lang   │           │ - JSONB  │
                                                    └──────────┘           └──────────┘

    Category (self-referencing)                          Evaluation has:
      parent_id → id                                     strengthsJson, weaknessesJson
                                                           stored as JSONB strings
```

**Where to study:** Read all entity files in `domain/`:
- `User.java` — basic JPA entity with `@CreationTimestamp`
- `InterviewSession.java` — LAZY relationships, EnumType.STRING for status
- `Question.java` — **pgvector column**: `@JdbcTypeCode(SqlTypes.VECTOR)` on `double[] vectorEmbedding`, plus the minimal `AttributeConverter<double[], double[]>` required by Hibernate 7
- `Answer.java` — joins session to question
- `Evaluation.java` — JSONB columns for AI-generated strengths/weaknesses
- `Category.java` — self-referencing @ManyToOne parent relationship

#### 2b. Repository Patterns

```java
// UserRepository.java — simple derived queries
boolean existsByEmail(String email);
Optional<User> findByEmail(String email);

// InterviewSessionRepository.java — complex JPQL
@Query("SELECT i FROM InterviewSession i WHERE i.userId = :userId " +
       "AND i.status = ACTIVE ORDER BY i.startedAt DESC LIMIT 1")
Optional<InterviewSummary> findActiveSummary(Long userId);

// CategoryRepository.java — JOIN FETCH for eager loading parents
@Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent p WHERE c.id IN :ids")
List<Category> findByIdIn(@Param("ids") List<Long> ids);
```

**Concepts to learn from your repositories:**
- **Derived query methods**: Spring Data generates SQL from method names (`findByStatusAndStartedAtBefore`)
- **JPQL with @Query**: Type-safe queries using entity field names, not column names
- **@Param annotation**: Named parameters in custom JPQL
- **LEFT JOIN FETCH**: Eagerly loads related entities to avoid N+1 query problems

#### 2c. pgvector Integration (Hibernate 7 + VECTOR type)

```java
// Question.java — the vector column declaration:
@JdbcTypeCode(SqlTypes.VECTOR)
@Column(columnDefinition = "vector(768)")
private double[] vectorEmbedding;
```

**How it works:** PostgreSQL's pgvector extension stores vectors as a binary format. Hibernate 7 (in Spring Boot 4) has native support via `SqlTypes.VECTOR`. The minimal `AttributeConverter<double[], double[]>` delegates the actual byte encoding/decoding to Hibernate's JdbcTypeRegistry — you don't need to manually convert between Java arrays and PostgreSQL's binary vector format.

**Where to study:** 
- Entity: `domain/Question.java` (the column declaration)
- Migration: `db/migration/V1__create_pgvector_extension.sql` (enables pgvector extension)
- Service: `InterviewSetupService.resolveTopicNamesToIds()` — topic IDs stored as `{id1,id2,...}` JSON string in the database

#### 2d. Flyway Migrations

```
┌─────────────────────────────────────────┐
│  V1__create_pgvector_extension.sql       │
│    CREATE EXTENSION IF NOT EXISTS vector;│
│                                          │
│  V2__add_topic_hierarchy.sql             │
│    Adds parent_id FK + CHECK constraint   │
│    on categories table                   │
│                                          │
│  V3__seed_topics.sql                     │
│    Seeds 5 languages with their topics   │
│    Uses ON CONFLICT DO NOTHING           │
└─────────────────────────────────────────┘
```

**Concept:** Flyway manages schema changes. Each migration is an ordered SQL file. Spring Boot runs them on startup (controlled by `spring.flyway.enabled=true`). This is the industry standard for production database migrations in Spring Boot.

---

### Phase 3: Security — JWT Cookie Auth Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  REGISTER / LOGIN FLOW:                                         │
│                                                                 │
│  POST /auth/register/login                                     │
│    Request: { email, password }                                  │
│    → SecurityConfig checks no auth required for these routes     │
│    → AuthController validates @Valid request body                │
│    → BCryptPasswordEncoder encodes password                      │
│    → JwtService generates signed JWT (HS256 with secret)         │
│    → ResponseCookie.from("jwt_token", token, .httpOnly(.true))   │
│      sets Set-Cookie header on response                          │
│                                                                 │
│  SUBSEQUENT REQUESTS:                                           │
│                                                                 │
│  Any authenticated request                                       │
│    → JwtAuthenticationFilter (OncePerRequestFilter)              │
│       extracts "jwt_token" cookie from request                   │
│       validates JWT signature + expiration                       │
│       sets request attributes: currentUserId, currentEmail       │
│       creates UsernamePasswordAuthenticationToken for Spring     │
│    → AuthenticationContext.getCurrentUserId() reads the attribute│
│      (ThreadLocal-style access without re-parsing the token)     │
│                                                                 │
│  LOGOUT:                                                        │
│    POST /auth/logout                                             │
│    → Sets cookie with maxAge=0, expires immediately              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Where to study:**
- `config/SecurityConfig.java` — SecurityFilterChain with STATELESS session policy, CSRF disabled (safe because HttpOnly + SameSite=Strict JWT cookie)
- `config/JwtAuthenticationFilter.java` — OncePerRequestFilter that extracts token from cookies and populates request attributes
- `config/JwtService.java` — jjwt 0.12.x API: Jwts.builder().signWith().compact() for generation, Jwts.parser().verifyWith().build().parseSignedClaims() for validation
- `config/AuthenticationContext.java` — reads user identity from request attributes via RequestContextHolder (avoids re-parsing JWT in every service)

**Key design decisions to understand:**
1. **CSRF disabled globally** — The JWT cookie is HttpOnly + SameSite=Strict, so it cannot be accessed by JavaScript (XSS-proof) and cross-site requests don't send it (CSRF-proof).
2. **Stateless sessions** — No server-side HttpSession. Each request self-authenticates via the JWT cookie.
3. **Request attribute pattern** — Instead of using SecurityContextHolder directly in services, the filter sets user identity as request attributes that `AuthenticationContext` reads via RequestContextHolder. This is cleaner for service-layer code.

---

### Phase 4: Spring AI 2.0 Integration (the most important new concept)

This is where your backend gets intelligent — it talks to an LLM (LM Studio at localhost:1234, or any OpenAI-compatible endpoint).

#### 4a. ChatClient + ChatModel Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  AI CONFIGURATION (AiConfig.java)                           │
│                                                             │
│  @Bean("questionChatClient")                                │
│  ChatClient questionChatClient(ChatModel chatModel) {       │
│    return ChatClient.builder(chatModel)                      │
│      .defaultSystem("You are a technical interview...")     │
│      .build();                                              │
│  }                                                          │
│                                                             │
│  @Bean("evaluationChatClient")                              │
│  ChatClient evaluationChatClient(ChatModel chatModel) {     │
│    return ChatClient.builder(chatModel)                      │
│      .defaultSystem("You are an AI interviewer evaluating...")│
│      .build();                                              │
│  }                                                          │
│                                                             │
│  Two separate beans because they have different temperatures│
│  configured in application.properties:                       │
│    question gen: temperature=0.7 (creative)                 │
│    evaluation:   temperature=0.0 (deterministic scoring)     │
└─────────────────────────────────────────────────────────────┘
```

**Concept:** Spring AI auto-configures a `ChatModel` bean from your `spring.ai.openai.*` properties — it doesn't have to be OpenAI; any OpenAI-compatible endpoint works (LM Studio, Ollama, local models). The `ChatClient` is the high-level API you use to construct prompts and call the model.

#### 4b. Native Structured Output with Java Records (Spring AI 2.0 Feature)

This is the **star feature of Spring AI 2.0**: telling the LLM exactly what JSON shape to return, validated against a Java record schema.

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  WITHOUT structured output:                                 │
│    "Generate interview questions" → free-text JSON          │
│    LLM might return wrong fields, extra fields, or bad types│
│    You must manually parse and validate everything           │
│                                                             │
│  WITH Spring AI native structured output (your code):       │
│                                                             │
│    public record QuestionRecord(                            │
│        @JsonPropertyDescription("Question type") String type,│
│        @JsonPropertyDescription("Topic list") List<String>   │
│          topics,                                            │
│        ...                                                  │
│    ) {}                                                     │
│                                                             │
│    var result = chatClient.prompt()                        │
│      .user(prompt)                                          │
│      .call();                                               │
│    return result.entity(QuestionRecordList.class,           │
│      spec -> spec                                             │
│        .useProviderStructuredOutput()                       │  ← tells LLM to output JSON     │
│        .validateSchema())                                    │  ← validates at runtime       │
│      .questions();                                           │
│                                                             │
│  Spring AI generates a JSON Schema from the record's fields │
│  and sends it to the LLM. The LLM is constrained to output  │
│  valid JSON matching that schema.                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Where to study:**
- `output/QuestionRecord.java` — record defining question shape with @JsonPropertyDescription annotations
- `output/EvaluationRecord.java` — record defining evaluation result shape (score, strengths, weaknesses, improvedAnswer)
- `question/AiGatewayService.java` — where structured output is actually used in `.entity(Class, spec -> ...)` calls

**Why a wrapper record (`QuestionRecordList`)?:** OpenAI's structured output requires the top-level to be a JSON object, not an array. So you wrap your `List<QuestionRecord>` in a record:
```java
public record QuestionRecordList(List<QuestionRecord> questions) {}
```

#### 4c. Prompt Engineering Patterns (in AiGatewayService.java)

Your prompts use Java text blocks (`"""..."""`) with `.formatted()` for interpolation:

**Question Generation prompt template:**
```java
return """
    Generate %d interview questions for a technical interview platform.
    
    Category: Java
    Topics: Core Java, Streams
    
    Difficulty: medium
    Programming Language (for coding challenges): Java
    
    Requirements:
    - Include both THEORY and CODE type questions
    - For coding questions, provide title, description, and codePrompt (starter code)
    ...""".formatted(count, topicDisplay, difficulty, language);
```

**Theory Evaluation prompt template:**
```java
return """
    Evaluate this theory interview answer for a technical assessment.
    
    Question: %s
    Category: %s
    Difficulty: %s
    
    User's answer:
    %s
    
    Provide your evaluation as JSON with these exact fields:
    - score: integer 0-10 rating of the answer quality
    ...""".formatted(questionText, categoryId, difficulty, userAnswer);
```

**Language-specific system prompts for coding evaluation:**
Three separate prompts based on language (Java/Python/Javascript), each instructing the model to focus on language-appropriate best practices. These are injected via `.system(s -> s.text(languagePrompt))`.

#### 4d. EmbeddingModel for Vector Search

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  QuestionGenerationService generates questions →            │
│    → AiGatewayService embeds question text via              │
│      embeddingChatClient.prompt().user(text).call()         │
│    → Returns float[] (768 dimensions)                       │
│    → Stored in Question.vectorEmbedding column (pgvector)   │
│                                                             │
│  If embedding fails: zero-vector fallback [0.0, 0.0, ...]  │
│    The system degrades gracefully — interviews still work   │
│    without vector search capability                         │
│                                                             ─────────────────────────────────────┘
```

**Where to study:** `embedding/EmbeddingService.java` — thin wrapper around Spring AI's EmbeddingModel. `QuestionGenerationService.java` — where embeddings are generated and stored with zero-vector fallback.

#### 4e. JSONB Storage for AI Feedback

AI-generated strengths and weaknesses are stored as JSON strings in VARCHAR columns (PostgreSQL treats them as JSONB when the column has `columnDefinition = "jsonb"`):

```java
// AiGatewayService.mapToEvaluation():
String jsonStr = jsonMapper.writeValueAsString(record.strengths());  // Jackson serialization
eval.setStrengthsJson(jsonStr);  // Stored in DB
```

**Concept:** This is a denormalized approach. Instead of creating a separate table for strengths/weaknesses, they're serialized as JSON and stored inline with the evaluation record. Querying requires parsing at query time (AnalyticsService uses native SQL + `Transformers.ALIAS_TO_ENTITY_MAP` to extract data from these JSONB fields).

---

### Phase 5: Production Patterns

#### 5a. Scheduled Tasks

```java
// SessionTimeoutCleanupTask.java
@Scheduled(fixedRate = 300000) // every 5 minutes
@Transactional(readOnly = true)
public void cleanupExpiredSessions() {
    LocalDateTime cutoffTime = LocalDateTime.now().minusHours(2);
    sessionRepository.findByStatusAndStartedAtBefore(SessionStatus.ACTIVE, cutoffTime)
            .forEach(session -> {
                session.setStatus(SessionStatus.ABANDONED);
                session.setEndedAt(LocalDateTime.now());
            });
}
```

**Concept:** `@Scheduled` runs methods on a fixed interval. Combined with Spring Data's derived query `findByStatusAndStartedAtBefore`, it finds all active sessions older than the configured timeout (2 hours) and marks them abandoned. Error handling: individual session failures don't stop the whole task.

#### 5b. Exception Handling (@ControllerAdvice)

```java
// ValidationExceptionHandler.java
@ControllerAdvice
public class ValidationExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(...) {
        // Returns structured error response with field-level validation errors
    }
}
```

**Concept:** `@ControllerAdvice` + `@ExceptionHandler` is Spring's global exception handling mechanism. Catches Bean Validation failures from `@Valid` on request bodies and returns clean JSON error responses instead of stack traces.

#### 5c. Resilience Patterns

| Pattern | Where Used | Why |
|---------|-----------|-----|
| Zero-vector fallback | `QuestionGenerationService` | If embedding fails, store zero vector so question still persists |
| Per-answer try/catch | `InterviewController.finish()` | One AI evaluation failure shouldn't abort the entire session completion |
| Retry endpoint | `/evaluation/retry/{answerId}` | Allows manual recovery of failed evaluations without restarting the interview |
| CSRF self-healing | Frontend `CsrfTokenInterceptor` | On 403 response, re-reads the CSRF cookie in case it was rotated server-side |

#### 5d. Functional SPA Fallback Controller

```java
// SpaFallbackController.java — serves index.html for any non-API route
@GetMapping({"/", "/{path:[^.]*}"})
public ServerResponse index() {
    return new ClassPathResource("static/browser/index.html");
}
```

**Concept:** The regex `/{path:[^.]*}` matches any URL that doesn't contain a dot (so CSS/JS/image files are served normally by Spring's static resource handler). For everything else, it returns the Angular SPA's index.html. This is what allows client-side routing to work — when you navigate to `/analytics`, Spring serves index.html and Angular's router takes over.

---

## Part C: Cross-Cutting Concepts — Connecting Frontend to Backend

### Request Flow: Complete Example (Start Interview)

```
┌─────────────┐                    ┌───────────┐                     ┌─────────────┐
│  Browser    │  POST /interview/  │           │   Spring AI Call    │  LM Studio  │
│             │  start {topics,}──▶│ Controller│  ────────────────▶  │  (Gemma)    │
│             │                   │           │  ChatClient.prompt()│             │
│             │◀── JSON response──┤ Service   │◀── structured output│             │
│             │  {sessionId,      │           │  QuestionRecordList │             │
│             │   questions}      │ Repository│                       │             │
│             │                   └───────────┘                       │             │
└──────┬──────┘                                                     └─────────────┘
       │
       ▼
 InterviewSignalService.currentSessionId.set(sessionId)
 InterviewSignalService.questions.set(questions)
```

### Auth Flow: Complete Example (Login → Subsequent Requests)

```
┌───────────┐      POST /auth/login     ┌─────────────┐
│  Browser  │ ◀── Set-Cookie: jwt_token │             │
│           │                          │  Backend    │
│           │ ◀── {userId, email}      │             │
└────┬──────┘                          └─────────────┘
     │
     ▼ (every subsequent request)

Browser sends: Cookie: jwt_token=eyJhbGc...
                    │
                    ▼
    JwtAuthenticationFilter extracts token from cookie
        │ validates signature + expiration with jjwt
        │ sets request attributes: currentUserId, currentEmail
        │ creates UsernamePasswordAuthenticationToken
                    │
                    ▼
    Controller method calls AuthenticationContext.getCurrentUserId()
        reads the request attribute set by the filter
            → no JWT re-parsing needed in service code
```

---

## Part D: Learning Exercises — Study Your Own Code

### Angular Frontend Exercises

| # | Exercise | Files to Read | Concept Tested |
|---|----------|---------------|----------------|
| 1 | Trace the full lifecycle of a signal read from template to DOM re-render | NavbarComponent, AuthService | Signal tracking in OnPush components |
| 2 | Map every Material module import to the component that uses it | All component files | Standalone imports granularity |
| 3 | Identify which services use signals vs Observables vs BehaviorSubjects and explain why each was chosen | Auth, InterviewSignal, DropdownDataService | Hybrid reactive state patterns |
| 4 | Read app.routes.ts and trace how a URL navigates through guards → lazy load → component render | app.routes.ts, AuthGuard, IdlePreloadingStrategy | Modern routing pipeline |
| 5 | Find every place effect() is called and explain what side effect it produces | AnalyticsPageComponent, ThemeService | Effect for non-reactive side effects |
| 6 | Trace how a form submission flows from user input → validation → service call → response handling | LoginComponent, AuthService | Reactive forms + signal state update |

### Spring Boot Backend Exercises

| # | Exercise | Files to Read | Concept Tested |
|---|----------|---------------|----------------|
| 1 | Draw the complete entity relationship diagram from JPA annotations | All domain/*entity*.java files | JPA relationships, lazy loading, cascades |
| 2 | Trace a single request: POST /interview/start → AI generates questions → stored in DB | InterviewController → InterviewSetupService → AiGatewayService → QuestionGenerationService → QuestionRepository | Layered flow across packages |
| 3 | Explain why two separate ChatClient beans exist and how their temperature settings affect behavior | AiConfig.java, application.properties | Dual bean strategy for different AI modes |
| 4 | Walk through Spring AI's structured output pipeline: record definition → schema generation → LLM call → validation | QuestionRecord.java, EvaluationRecord.java, AiGatewayService.java | Native structured output (Spring AI 2.0) |
| 5 | Identify all resilience patterns and explain what failure each protects against | InterviewController, QuestionGenerationService, CsrfTokenInterceptor | Production-ready error handling |
| 6 | Trace the JWT lifecycle: generation → cookie setting → filter extraction → AuthenticationContext access | JwtService, AuthController, JwtAuthenticationFilter, AuthenticationContext | Cookie-based auth architecture |

---

## Part E: Key Differences — Angular 8 vs Angular 18 (Your Code)

| Concept | Angular 8 (what you knew) | Angular 18 (your app) |
|---------|--------------------------|----------------------|
| Component definition | Inside NgModule with `declarations` | `standalone: true`, self-declared imports |
| Module system | NgModules everywhere | No NgModules, functional config in `app.config.ts` |
| State management | BehaviorSubjects, ngrx/store, plain observables | **Signals** (signal, computed, effect) + Observables for HTTP |
| Change detection | Default strategy always runs full cycle | OnPush by default, triggered only by signal reads or @Input ref changes |
| Template syntax | `*ngIf`, `*ngFor` directives | `@if`, `@for` block syntax (in some places) |
| Routing | `loadChildren: () => import().then(m => Module)` | `loadComponent: () => import('./component')` — loads component directly |
| Guards/Interceptors | Class-based (`implements CanActivate`) | Functional (`CanActivateFn = (route, state) => ...`) |
| Dependency injection | Constructor parameters only | `inject()` function allowed in field initializers and functions |
| HTTP | `HttpClientModule` imported once in NgModule | `provideHttpClient()` in app.config.ts |

---

## Part F: Key Differences — Spring Boot Basics vs What's in Your App

| Concept | Beginner Level | Your App Goes Further With |
|---------|---------------|---------------------------|
| Controllers | `@RestController` with `@GetMapping/@PostMapping` | Functional SPA fallback with regex path patterns; multiple controllers per domain |
| Services | Plain `@Service` classes | Domain-organized services; cross-cutting AI integration via AiGatewayService bridge |
| Security | Basic auth or simple JWT filter | Cookie-based HttpOnly JWT + CSRF token flow for cross-origin SPAs |
| Database | JPA entities + CrudRepository | pgvector (AI embeddings) with Hibernate 7 VECTOR type; Flyway migrations; JSONB columns for unstructured AI feedback |
| AI integration | None (basic CRUD app) | Spring AI 2.0 structured output, dual temperature strategy, language-specific system prompts, embedding model for vector search |

---

## Part G: Recommended Study Order

Start with **Part A** if you want to deep-dive the frontend first. Start with **Part B** if backend is your weaker area (you said "very little knowledge"). Then do exercises from **Parts D and E** by reading the actual files in your codebase — that's where real learning happens because you're studying code you already built and understand contextually.

The cross-cutting flow diagrams in **Part C** will help you see how frontend and backend connect, which is essential for full-stack mastery.
