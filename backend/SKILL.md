# SKILL.md — AI Interview Prep Platform (Backend)

## Project Overview

This is a **Spring Boot 4.0** backend for an AI-powered technical interview preparation platform. It generates theory and coding questions via an LLM, evaluates user answers, stores results in PostgreSQL with pgvector support, and serves analytics dashboards to a frontend SPA.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 25 / Spring Boot 4.0.0 |
| Framework | Spring Web, Spring Security, Spring Data JPA, Spring AI (BOM 2.0.0) |
| Database | PostgreSQL + pgvector (Hibernate 7 native VECTOR type) |
| Migrations | Flyway (`src/main/resources/db/migration/`) |
| Auth | JWT (jjwt 0.12.5), HttpOnly cookies, BCrypt passwords |
| AI/LLM | Spring AI ChatClient with OpenAI-compatible endpoint (LM Studio default: `http://localhost:1234/v1`) |
| Serialization | Jackson 3 (`tools.jackson.databind.JsonMapper`) + Lombok |
| Build | Maven + Frontend-Maven-Plugin (builds Angular frontend from `../frontend/`) |

---

## Running the Application

```bash
# Prerequisites: PostgreSQL running, LM Studio serving embeddings & chat at localhost:1234

# Set environment variables (or use defaults in application.properties)
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=interview_prep
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=<at-least-256-bit-random-string>

# Build and run
mvn spring-boot:run

# Full build (includes frontend)
mvn clean package
```

The app starts on port 8080 by default. Frontend files are served from `src/main/resources/static/browser/`.

---

## Key Configuration (`application.properties`)

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8080 | HTTP server port |
| `spring.datasource.*` | localhost:5432/interview_prep | PostgreSQL connection |
| `jwt.secret` | dev default (change in production!) | HMAC-SHA256 signing key |
| `jwt.expiration-hours` | 8 | JWT token TTL |
| `spring.ai.openai.base-url` | http://localhost:1234/v1 | LM Studio endpoint |
| `spring.ai.openai.chat.options.model` | gemma-4-12b-coder-fable5-composer2.5-v1 | Chat model |
| `spring.ai.embedding.options.model` | text-embedding-embeddinggemma-300m | Embedding model |
| `spring.ai.openai.chat.options.temperature` | 0.7 | Question generation temperature |
| `spring.ai.evaluation.temperature` | 0.0 | Evaluation (deterministic) |
| `interview.session.timeout-hours` | 2 | Session auto-cleanup threshold |

---

## Architecture at a Glance

```
┌─────────────────────────────────────────────────────────────┐
│                      Frontend SPA                           │
│                  (Angular, ../frontend/)                    │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP + JWT cookie
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              InterviewController (/interview/*)             │
│  startInterview → generateQuestions → save session/questions│
│  submitAnswer → evaluateAnswer → save answer/evaluation     │
│  finishInterview → compile summary                          │
├─────────────────────────────────────────────────────────────┤
│              AuthController (/auth/*)                       │
│  register / login / logout / status                         │
├─────────────────────────────────────────────────────────────┤
│              AiGatewayService (core AI logic)               │
│  generateQuestions → questionChatClient (temperature=0.7)   │
│  evaluateTheoryAnswer / evaluateCodingAnswer                │
│       → evaluationChatClient (temperature=0.0)              │
├─────────────────────────────────────────────────────────────┤
│              EmbeddingService                               │
│  embed(text) → float[] via Spring AI EmbeddingModel         │
├─────────────────────────────────────────────────────────────┤
│              AnalyticsService                               │
│  getPerformanceData(userId) → totalSessions, avgScore,      │
│    categoryBreakdown, session history                       │
└──────────────────────────┬──────────────────────────────────┘
                           │ JPA / Native SQL
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              PostgreSQL + pgvector                          │
│  users | interview_sessions | questions (vector)            │
│  answers | evaluations | categories                         │
└─────────────────────────────────────────────────────────────┘
```

---

## Package Structure

| Package | Purpose |
|---------|---------|
| `com.interviewprep` | Application entry point (`InterviewPrepPlatformApplication`) |
| `com.interviewprep.auth` | Authentication endpoints (register, login, logout) |
| `com.interviewprep.config` | Security, JWT, AI client beans, CSRF, CORS, session cleanup |
| `com.interviewprep.domain` | JPA entities, repositories, DTOs/request objects |
| `com.interviewprep.output` | Structured output POJOs for LLM responses (`QuestionRecord`, `EvaluationRecord`) |
| `com.interviewprep.question` | AI question generation and evaluation gateway |
| `com.interviewprep.embedding` | Vector embedding service wrapping Spring AI `EmbeddingModel` |
| `com.interviewprep.interview` | Interview lifecycle controllers (start, submit, evaluate, finish) + language/topic endpoints |
| `com.interviewprep.service` | Business logic services (`LanguageTopicsService`, `TopicAutoSelectionService`) |
| `com.interviewprep.analytics` | Performance analytics controller and service |

---

## Domain Model

```
User (1) ──< InterviewSession (1) ──< Question (N) ──< Answer (1) ──< Evaluation
  │                  │                    │
  │                  │                    └── categoryId (topic reference)
  │                  └── topicIdsJson, difficulty, languageId, status
  └── email (unique), password (BCrypt), name
```

- **Question** has a `vector_embedding` column (768-dim pgvector) for semantic search.
- **Evaluation** stores strengths/weaknesses as JSONB arrays and optional code-specific fields (`isCorrect`, `timeComplexity`, `spaceComplexity`).
- **InterviewSession** tracks lifecycle: ACTIVE → COMPLETED, with timeout hours for auto-cleanup.

---

## AI / LLM Integration

Two separate `ChatClient` beans are configured in [`AiConfig`](src/main/java/com/interviewprep/config/AiConfig.java):

1. **`questionChatClient`** — temperature 0.7, used for generating interview questions
2. **`evaluationChatClient`** — temperature 0.0 (deterministic), used for scoring answers

Both use **provider-structured output** (`useProviderStructuredOutput()`) to get typed Java objects directly from LLM responses:
- `QuestionRecordList` → list of generated questions
- `EvaluationRecord` → scored evaluation with strengths, weaknesses, improved answer

The AI endpoint defaults to LM Studio at `http://localhost:1234/v1`. Replace with OpenAI or any OpenAI-compatible API for production.

---

## Security Model

- **Stateless JWT** — no server-side sessions; token stored in HttpOnly cookie (`jwt_token`)
- **CSRF disabled** globally (cookie-based auth, not CSRF-protected)
- **Public endpoints**: `/auth/register`, `/auth/login`, `/auth/logout`
- **All other endpoints**: require valid JWT via `JwtAuthenticationFilter`
- Passwords: BCrypt hashed via Spring Security's `PasswordEncoder`

---

## Database Migrations (Flyway)

| Migration | Purpose |
|-----------|---------|
| `V1__create_pgvector_extension.sql` | Enable pgvector extension for vector similarity search |
| `V2__add_topic_hierarchy.sql` | Add topic hierarchy support (language → category → subtopic) |
| `V3__seed_topics.sql` | Seed initial language/topic data |

---

## Testing

Tests are in [`src/test/java/com/interviewprep/`](src/test/java/com/interviewprep/) using JUnit 5 + Spring Boot Test. H2 is used as an in-memory test database. Run with:

```bash
mvn test
```

Key test coverage areas:
- Auth flow (`AuthControllerTest`)
- JWT token generation/validation (`JwtServiceTest`, `AuthenticationContextTest`)
- AI service mapping and retry logic (`AiGatewayServiceTest`, `AiGatewayServiceMappingTest`)
- Interview validation (`InterviewControllerValidationTest`)
- Topic/language services (`LanguageTopicsServiceTest`, `TopicAutoSelectionServiceTest`)

---

## Build & Deployment Notes

- The Maven build includes the **Frontend-Maven-Plugin** which builds the Angular frontend from `../frontend/` and copies output to `src/main/resources/static/browser/`.
- Run `mvn clean package` for a full build (frontend + backend).
- For development, run the frontend dev server separately and only use `mvn spring-boot:run`.

---

## Common Development Tasks

### Adding a new AI prompt / evaluation criterion
1. Update the system prompt in [`AiConfig`](src/main/java/com/interviewprep/config/AiConfig.java) or add a new method in [`AiGatewayService`](src/main/java/com/interviewprep/question/AiGatewayService.java).
2. If returning structured data, create/modify the output POJO in `com.interviewprep.output`.

### Adding a new database field
1. Add the column to the JPA entity in `com.interviewprep.domain`.
2. Create a Flyway migration (`V{n}__description.sql`) for schema changes.

### Changing JWT behavior
- Modify [`JwtService`](src/main/java/com/interviewprep/config/JwtService.java) for token generation/validation logic.
- Update `JwtConfig` or `application.properties` for secret/expiration settings.

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Connection refused" on startup | Ensure PostgreSQL is running and credentials match `application.properties` |
| AI generation fails / returns empty | Verify LM Studio (or configured endpoint) is running at the URL in config; check model name matches a loaded model |
| JWT validation errors after deploy | Update `JWT_SECRET` env var — tokens signed with different secrets won't validate |
| Frontend 404s on refresh | [`SpaFallbackController`](src/main/java/com/interviewprep/config/SpaFallbackController.java) serves `index.html` for unmatched routes |
