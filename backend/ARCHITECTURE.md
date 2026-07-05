# Architecture Brief

## System Overview

The AI Interview Prep Platform is a Spring Boot backend application that provides intelligent interview preparation through AI-generated questions and automated evaluation. The system follows a layered architecture with clear separation of concerns between presentation, business logic, and data access layers.

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (SPA)                           │
│                    React/Angular/Vue Application                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     API Gateway / Load Balancer                 │
│                   (Nginx/HAProxy - Optional)                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                      │
│  ┌─────────────┬──────────────┬──────────────┬───────────────┐  │
│  │   Auth      │  Interview   │  Analytics   │    Config     │  │
│  │ Controller  │  Controller  │  Controller  │   Filters     │  │
│  └─────────────┴──────────────┴──────────────┴───────────────┘  │
│                              │                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   Service Layer                          │   │
│  │  InterviewSetupService │ QuestionGenerationService       │   │
│  │  InterviewResultService │ LanguageTopicsService          │   │
│  │  TopicAutoSelectionService                               │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   Repository Layer                       │   │
│  │  UserRepository │ InterviewSessionRepository             │   │
│  │  QuestionRepository │ AnswerRepository                   │   │
│  │  EvaluationRepository │ CategoryRepository               │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│   PostgreSQL + pgvector │     │    AI Model Server      │
│   (Questions, Users,    │     │  (LM Studio / OpenAI)   │
│   Sessions, Evaluations)│     │   (Question Generation, │
└─────────────────────────┘     │  Evaluation, Embeddings)│
                                └─────────────────────────┘
```

## Core Components

### 1. Authentication Layer

**Purpose**: Secure API endpoints with JWT-based authentication

**Components**:
- `AuthController` - Handles registration, login, logout, and status checks
- `JwtService` - Generates and validates JWT tokens
- `JwtAuthenticationFilter` - Extracts and validates JWT from cookies on each request
- `SecurityConfig` - Configures Spring Security filter chain
- `AuthenticationContext` - Thread-local storage for current user context

**Flow**:
```
Request → JwtAuthenticationFilter → Extract JWT from cookie
    ↓
JwtService.validateToken() → Parse claims (userId, email)
    ↓
AuthenticationContext.setCurrentUserId(userId)
    ↓
Controller methods access via AuthenticationContext.getCurrentUserId()
```

### 2. Interview Management Layer

**Purpose**: Manage interview session lifecycle and question generation

**Components**:
- `InterviewController` - REST endpoints for interview operations
- `InterviewSetupService` - Session creation, topic resolution, language validation
- `QuestionGenerationService` - AI-powered question generation with vector embeddings
- `AnswerSubmissionController` - Handle user answer submissions
- `EvaluationController` - Retry failed evaluations

**Flow**:
```
Start Interview → InterviewSetupService.createSession()
    ↓
Resolve topics (auto-select or explicit) → TopicAutoSelectionService
    ↓
Generate questions → QuestionGenerationService.generateAndPersistQuestions()
    ↓
Call AI Gateway → AiGatewayService.generateQuestions()
    ↓
Create vector embeddings → EmbeddingService.embed(questionText)
    ↓
Persist to database → QuestionRepository.saveAll(questions)
```

### 3. Evaluation Layer

**Purpose**: Automated scoring and feedback generation using AI

**Components**:
- `AiGatewayService` - Bridge between application and AI models
- `EvaluationController` - Retry mechanism for failed evaluations
- `InterviewResultService` - Aggregate evaluation results into ResultsResponse

**Flow**:
```
User submits answer → AnswerSubmissionController.submitAnswer()
    ↓
Persist answer → AnswerRepository.save(answer)
    ↓
Trigger async evaluation (optional)
    ↓
Evaluate answer → AiGatewayService.evaluateCodingAnswer() / evaluateTheoryAnswer()
    ↓
Parse AI response → EvaluationRecord (score, strengths, weaknesses, etc.)
    ↓
Create Evaluation entity → EvaluationRepository.save(evaluation)
    ↓
Aggregate results → InterviewResultService.assembleResults(sessionId)
```

### 4. Analytics Layer

**Purpose**: Track and present user performance metrics

**Components**:
- `AnalyticsController` - REST endpoints for analytics data
- `AnalyticsService` - Compute aggregated metrics from raw session data
- `UserPerformanceRepository` - CRUD operations for performance records

**Flow**:
```
Request performance data → AnalyticsController.getPerformanceData()
    ↓
Query sessions and answers → InterviewSessionRepository, AnswerRepository
    ↓
Compute aggregates (avg score, category breakdown) → AnalyticsService
    ↓
Update UserPerformance entity → UserPerformanceRepository.save()
    ↓
Return AnalyticsResponse to frontend
```

### 5. Configuration Layer

**Purpose**: Security, CORS, CSRF, and request filtering

**Components**:
- `SecurityConfig` - Spring Security filter chain configuration
- `JwtAuthenticationFilter` - JWT validation on each request
- `ApiPrefixStripFilterConfig` - Strip `/api` prefix from URLs
- `CsrfCookieFilter` - Handle CSRF token cookies for cross-origin requests
- `CorsConfig` - Configure CORS policies
- `WebConfig` - Register custom filters

## Data Flow Architecture

### Question Generation Pipeline

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  Frontend   │────▶│ Interview    │────▶│ AI Gateway  │
│  (React)    │     │ Controller   │     │ Service     │
└─────────────┘     └──────────────┘     └─────────────┘
                          │                      │
                          ▼                      ▼
                   ┌──────────────┐      ┌─────────────┐
                   │ Interview    │      │ LM Studio / │
                   │ Setup        │      │ OpenAI API  │
                   │ Service      │      └─────────────┘
                   └──────────────┘              │
                          │                      ▼
                          ▼              ┌─────────────┐
                   ┌──────────────┐      │ Question    │
                   │ Question     │◀─────│ Record DTO  │
                   │ Generation   │      └─────────────┘
                   │ Service      │
                   └──────────────┘
                          │
                          ▼
                   ┌──────────────┐
                   │ Embedding    │
                   │ Service      │
                   └──────────────┘
                          │
                          ▼
                   ┌──────────────┐
                   │ Question     │
                   │ Repository   │
                   └──────────────┘
```

### Answer Evaluation Pipeline

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│  Frontend   │────▶│ Answer           │────▶│ AI Gateway  │
│  (React)    │     │ Submission       │     │ Service     │
└─────────────┘     │ Controller       │     └─────────────┘
                    └──────────────────┘              │
                          │                           ▼
                          ▼                  ┌─────────────┐
                   ┌──────────────────┐      │ Evaluation  │
                   │ Answer           │◀─────│ Record DTO  │
                   │ Repository       │      └─────────────┘
                   └──────────────────┘              │
                          │                           ▼
                          ▼                  ┌─────────────┐
                   ┌──────────────────┐      │ Evaluation  │
                   │ Evaluation       │◀─────│ Entity      │
                   │ Repository       │      └─────────────┘
                   └──────────────────┘              │
                          │                           ▼
                          ▼                  ┌─────────────┐
                   ┌──────────────────┐      │ Interview   │
                   │ Interview        │◀─────│ Result      │
                   │ Result Service   │      │ Service     │
                   └──────────────────┘      └─────────────┘
```

## Database Schema Architecture

### Entity Relationships

```
User (1) ──── (N) InterviewSession (1) ──── (N) Question (1) ──── (N) Answer
   │                    │                        │                      │
   │                    │                        ▼                      ▼
   │                    │              Evaluation (1)            Evaluation
   │                    │                        │                      │
   │                    └────────────────────────┴──────────────────────┘
   │                                              |
   └──────────────────────────────────────────────┘
                                        (N) UserPerformance
```

### Key Tables

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `users` | User accounts | id, email, password, name, created_at |
| `categories` | Language/topic hierarchy | id, name, type, parent_id |
| `interview_sessions` | Session metadata | id, user_id, category_id, status, started_at, ended_at |
| `questions` | Generated questions | id, session_id, type, question_text, embedding (vector) |
| `answers` | User responses | id, session_id, question_id, answer_text, language_submitted |
| `evaluations` | AI evaluations | id, answer_id, score, strengths_json, weaknesses_json, status |
| `user_performance` | Aggregated metrics | id, user_id, total_sessions, avg_score, category_breakdown_json |

## Security Architecture

### Authentication Flow

1. **Registration**: User submits credentials → Password hashed with BCrypt → JWT generated and set as HttpOnly cookie
2. **Login**: User submits credentials → Verify password hash → Generate JWT → Set cookie
3. **Request Validation**: Each request → `JwtAuthenticationFilter` extracts JWT from cookie → Validate signature and expiration → Populate `AuthenticationContext`
4. **Authorization**: Controllers check `AuthenticationContext.getCurrentUserId()` to verify resource ownership

### Security Features

- **Stateless Authentication**: No server-side sessions; JWT acts as sole credential
- **HttpOnly Cookies**: Prevents XSS access to JWT tokens
- **SameSite=Strict**: Mitigates CSRF attacks
- **BCrypt Password Hashing**: Automatic salt generation, resistant to brute-force
- **JWT Expiration**: Configurable token lifetime (default: 8 hours)

## AI Integration Architecture

### Model Configuration

The platform uses Spring AI with OpenAI-compatible endpoints:

```properties
# Question Generation Model
spring.ai.openai.chat.options.model=gemma4:e2b-it-qat
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.chat.options.max-tokens=20000

# Evaluation Model (deterministic)
spring.ai.evaluation.temperature=0.0

# Embedding Model
spring.ai.embedding.options.model=nomic-embed-text:latest
```

### Structured Output Schema

AI responses are validated against strict schemas using Spring AI's native structured output:

**QuestionRecord**:
```json
{
  "type": "THEORY|CODE",
  "topics": ["topic1", "topic2"],
  "questionText": "...",
  "title": "...",           // CODE only
  "description": "...",     // CODE only
  "codePrompt": "..."       // CODE only
}
```

**EvaluationRecord**:
```json
{
  "score": 8,
  "strengths": ["...", "..."],
  "weaknesses": ["...", "..."],
  "improvedAnswer": "...",
  "isCorrect": true,
  "correctnessExplanation": "...",
  "timeComplexity": "O(n log n)",
  "spaceComplexity": "O(1)"
}
```

## Performance Considerations

### Vector Embeddings

- Questions are embedded using `nomic-embed-text:latest` model
- Embeddings stored in PostgreSQL `vector` column type (pgvector extension)
- Enables semantic similarity search for question recommendation

### Session Management

- Active sessions timeout after configurable hours (default: 2 hours)
- Background cleanup task (`SessionTimeoutCleanupTask`) runs every 5 minutes
- Abandoned sessions marked as `ABANDONED` status, not deleted

### Caching Strategy

Currently no explicit caching layer. Performance optimizations:
- Database indexes on frequently queried columns
- Lazy loading for large object graphs
- Batch operations where possible

## Deployment Architecture

### Development Environment

```
Local PostgreSQL (port 5432)
    ↓
Spring Boot Application (port 8080)
    ↓
LM Studio (port 11434) - Local AI model server
```

### Production Environment

```
Load Balancer (Nginx/HAProxy)
    ↓
Application Server Cluster (multiple Spring Boot instances)
    ↓
Managed PostgreSQL with pgvector (AWS RDS, Azure Database, etc.)
    ↓
Cloud AI Service (OpenAI API, Azure OpenAI, or self-hosted model server)
```

### Containerization Support

The application can be containerized with Docker:

```dockerfile
FROM eclipse-temurin:25-jre-alpine
COPY target/interview-prep-platform-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Monitoring and Observability

### Logging

Spring Boot provides structured logging via SLF4J/Logback. Configure in `application.properties`:

```properties
logging.level.com.interviewprep=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### Health Checks

Spring Boot Actuator can be added for health monitoring:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Endpoints available at `/actuator/health`, `/actuator/info`, etc.

## Scalability Considerations

### Horizontal Scaling

- Stateless authentication enables multiple application instances
- Database connection pooling (HikariCP) for efficient resource usage
- AI model server can be scaled independently

### Vertical Scaling

- Increase JVM heap size for larger question batches
- Optimize database queries with proper indexing
- Tune pgvector search parameters for embedding queries

## Future Enhancements

1. **Redis Caching**: Cache frequently accessed questions and user performance data
2. **Message Queue**: Async evaluation processing using RabbitMQ or Kafka
3. **WebSocket Support**: Real-time feedback during interview sessions
4. **Multi-tenant Architecture**: Support multiple organizations with isolated data
5. **Advanced Analytics**: Trend analysis, skill gap identification, personalized recommendations
