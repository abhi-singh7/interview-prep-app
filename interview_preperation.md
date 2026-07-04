# AI Interview Prep Platform — Project Overview

## What's Built

This is a full-stack interview preparation platform that uses AI (LM Studio / OpenAI-compatible endpoints) to generate technical questions and evaluate user answers in real-time.

### Current Features

- **Authentication**: JWT cookie-based auth with register/login/logout
- **Interview Sessions**: Create, resume, retake interviews with configurable topics, difficulty, and language
- **Question Generation**: AI generates theory + coding questions via structured output (Spring AI 2.0)
- **Answer Evaluation**: Real-time scoring with strengths, weaknesses, improved answers, and code-specific feedback
- **Analytics Dashboard**: Performance tracking across sessions with charts and category breakdowns
- **Session Management**: Resume interrupted interviews, retry failed evaluations, auto-cleanup of abandoned sessions
- **Theming**: Dark/light mode with cross-tab synchronization

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Angular 18 (standalone components, signals, OnPush CD) |
| Backend | Spring Boot 4.0, Java 25, Spring AI 2.0 |
| Database | PostgreSQL + pgvector (Hibernate 7 SqlTypes.VECTOR) |
| Auth | JWT cookies (HttpOnly), BCrypt passwords |
| AI/LLM | OpenAI-compatible endpoint (LM Studio default: localhost:1234) |
| Build | Maven + Frontend-Maven-Plugin, npm (Angular dev server) |

---

## How to Run

### Backend Only (Development)

```bash
# Prerequisites: PostgreSQL running, LM Studio serving at localhost:1234

cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. Frontend files are served from `src/main/resources/static/browser/` after a full build.

### Full Build (Production)

```bash
cd backend
mvn clean package
java -jar target/interview-prep-platform-*.jar
```

This builds the Angular frontend and packages it inside the Spring Boot JAR.

### Frontend Dev Server

```bash
cd frontend
npm install --legacy-peer-deps
npm start
```

The dev server runs on `http://localhost:80` and proxies `/api/*` requests to the backend on port 8080 (stripping the `/api` prefix).

---

## Project Layout

```
backend/src/main/java/com/interviewprep/
  analytics/          — AnalyticsController + AnalyticsService (native SQL, JSONB queries)
  auth/               — AuthController (register/login/logout/status)
  config/             — SecurityConfig, JwtAuthenticationFilter, AuthenticationContext, CorsConfig, CsrfCookieFilter, SpaFallbackController, AiConfig, ValidationExceptionHandler
  domain/             — JPA entities (User, InterviewSession, Question, Answer, Evaluation, Category), Repositories, DTO records/enums
  interview/          — Controllers + Services: session lifecycle, question generation, results, evaluation retry, language/topic lookup
  question/AiGatewayService.java   — Spring AI bridge: ChatClient calls, prompt templates, native structured output parsing
  embedding/EmbeddingService.java  — Wrapper around EmbeddingModel for vector embeddings
  service/            — TopicAutoSelectionService, LanguageTopicsService

frontend/src/app/
  analytics/          — AnalyticsPageComponent (ng2-charts + Material table)
  auth/               — LoginComponent, RegisterComponent
  components/         — NavbarComponent
  evaluation/         — ResultsPageComponent
  guards/             — AuthGuard (functional CanActivateFn), IdlePreloadingStrategy
  interview/          — SetupPage, InterviewPage, CodingQuestion, TheoryQuestion, MySessions, SessionDetail
  interceptors/       — CsrfTokenInterceptor (adds X-XSRF-TOKEN for non-GET)
  services/           — AuthService (signals + Observables hybrid), InterviewSignalService (WritableSignals + Maps), DropdownDataService (BehaviorSubject cache)
  shared/theme/       — ThemeService (signal-based, localStorage persistence + StorageEvent cross-tab sync)
```

---

## Key Workflows

### Start an Interview

1. User fills out the setup form: selects language, topics (or leave empty for auto-select), difficulty, and question count
2. POST `/interview/start` with `{topicIds[], difficulty, count, languageId}` 
3. Backend generates questions via AI using structured output (QuestionRecord schema)
4. Each question gets a vector embedding stored in pgvector
5. Returns `{sessionId, questions[]}`

### Answer Questions

- **Theory questions**: Type answer in textarea, submit for evaluation
- **Coding questions**: Write code with language selection (Java/Python/Javascript), optional starter code provided

### Finish & Get Results

1. POST `/interview/finish` with `{sessionId}`
2. Backend evaluates each answer via AI (temperature=0.0 for deterministic scoring)
3. Returns full results with scores, strengths, weaknesses, improved answers, and for coding: correctness assessment + time/space complexity
4. Failed evaluations can be retried individually via `POST /evaluation/retry/{answerId}`

### Resume an Interview

- GET `/interview/resume` checks for active sessions
- Frontend shows a "Resume Interview" card if one exists
- Questions loaded from saved session state

---

## API Endpoints

### Authentication (public)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register` | Create account, returns JWT cookie |
| POST | `/auth/login` | Authenticate, returns JWT cookie |
| POST | `/auth/logout` | Clear JWT cookie |
| GET | `/auth/status` | Check auth status (200/401) |

### Interview Session

| Method | Path | Description |
|--------|------|-------------|
| POST | `/interview/start` | Create session + generate questions |
| GET | `/interview/resume` | Check for active session |
| POST | `/interview/finish` | Complete session + evaluate answers |
| GET | `/interview/results/{sessionId}` | Get results for completed session |
| GET | `/interview/session/{id}/questions` | Get questions for active session |

### Evaluation

| Method | Path | Description |
|--------|------|-------------|
| POST | `/evaluation/retry/{answerId}` | Retry a failed evaluation |

### Analytics

| Method | Path | Description |
|--------|------|-------------|
| GET | `/analytics/performance` | Performance data, category breakdown, session history |

### Language & Topics

| Method | Path | Description |
|--------|------|-------------|
| GET | `/languages` | List available programming languages |
| GET | `/topics/{languageId}` | Get topics for a language |
| GET | `/topics/search/{languageId}/{query}` | Search topics by name |

---

## Configuration

### Environment Variables (or set in `application.properties`)

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=interview_prep
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=<at-least-256-bit-random-string>
AI_MODEL_NAME=gemma-4-12b-coder-fable5-composer2.5-v1
EMBEDDING_MODEL_NAME=text-embedding-embeddinggemma-300m
```

### AI Endpoint (default: LM Studio)

In `application.properties`:
```properties
spring.ai.openai.base-url=http://localhost:1234/v1
spring.ai.openai.chat.options.model=gemma-4-12b-coder-fable5-composer2.5-v1
spring.ai.embedding.options.model=text-embedding-embeddinggemma-300m
```

Replace with OpenAI or any OpenAI-compatible API for production use.

---

## Development Notes

### Frontend Dev Server Proxy

The Angular dev server (`npm start`) proxies `/api/*` → `localhost:8080`, stripping the `/api` prefix so controllers see requests at their mapped paths (e.g., `/interview/start`).

See `frontend/proxy.conf.json`.

### Database Migrations

Flyway manages schema changes in `backend/src/main/resources/db/migration/`:
- `V1__create_pgvector_extension.sql` — Enable pgvector extension
- `V2__add_topic_hierarchy.sql` — Topic hierarchy support (language → category → subtopic)
- `V3__seed_topics.sql` — Seed initial language/topic data

### State Management Patterns

- **Auth state**: signals + sessionStorage dual-source (`AuthService`)
- **Interview session**: `InterviewSignalService` owns `currentSessionId`, questions, evaluations as WritableSignals; uses plain JS Maps for answers/coding submissions (no reference change needed when entries mutate individually)
- **Theme state**: internal `_mode` signal exposed as readonly `mode$`; effect in constructor toggles `<body>` CSS class on change

### Critical Rules

1. **`effect()` only in constructors or field initializers.** Inside `ngOnInit()`, methods, or event handlers throws NG0203. Enforced by custom ESLint rule `effect-placement/no-effect-outside-constructor`. Run `npm run lint` to verify.
2. **OnPush + async data requires signals or explicit `markForCheck()`.** Plain property assignments in subscribe callbacks don't trigger CD on OnPush components. Use signal `.set()` (see `AuthService`) or inject `ChangeDetectorRef` and call `cdr.markForCheck()`.
3. **Frontend build output path.** Angular outputs to `dist/interview-prep-frontend/browser/`. The Maven plugin copies it into `backend/src/main/resources/static/browser/`. Changing the Angular outputPath requires updating both the maven plugin config AND `SpaFallbackController`'s classpath resource path.
4. **Lombok annotation processing is configured in Maven** (`maven-compiler-plugin.annotationProcessorPaths`). IDE LSP errors showing undefined getters/setters on Lombok-annotated classes (e.g., `@Data` records) are false positives — `mvn compile` works fine.

---

## Testing

Run backend tests with H2 in-memory database:
```bash
cd backend
mvn test
```

See `TESTING_GUIDE.md` for complete step-by-step testing instructions including API testing with curl and frontend walkthroughs.

---

## Deployment

### Docker (Backend)

Build the JAR and run with PostgreSQL and LM Studio configured via environment variables:
```bash
docker run -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e AI_MODEL_NAME=gemma-4-12b-coder-fable5-composer2.5-v1 \
  interview-prep-platform
```

### Production Checklist

- [ ] Set `JWT_SECRET` to a strong random string (env var, not in code)
- [ ] Configure production database connection
- [ ] Point AI endpoint to OpenAI or your own model server
- [ ] Enable HTTPS (`secure(true)` in ResponseCookie config)
- [ ] Set CORS allowed origins to your frontend domain

---

## Learning Resources

See `LEARNING_GUIDE.md` for a detailed walkthrough of the Angular + Spring Boot patterns used in this codebase, including:
- Standalone components, signals, OnPush change detection
- Spring AI 2.0 structured output with Java records
- JWT cookie auth flow
- pgvector integration with Hibernate 7
