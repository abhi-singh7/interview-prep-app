# AI Interview Prep Platform

An AI-powered interview preparation platform that generates dynamic technical questions and provides intelligent evaluation of user answers. Built with Spring Boot 4, PostgreSQL with pgvector for semantic search, and integrated with LM Studio/OpenAI-compatible AI models.

## Features

- **Dynamic Question Generation**: AI-generated coding and theory questions based on selected topics and difficulty levels
- **Intelligent Evaluation**: Automated scoring and feedback using AI analysis of user answers
- **Semantic Search**: Vector-based question similarity matching using PostgreSQL pgvector extension
- **JWT Authentication**: Stateless authentication with HttpOnly cookies for security
- **Session Management**: Interview session tracking with timeout and auto-cleanup
- **Performance Analytics**: Aggregated metrics on user performance across sessions
- **Multi-Language Support**: Questions in multiple programming languages (Java, Python, SQL, etc.)

## Tech Stack

### Backend
- **Framework**: Spring Boot 4.0.0
- **Language**: Java 25
- **Database**: PostgreSQL with pgvector extension
- **ORM**: Hibernate/JPA with Flyway migrations
- **Security**: Spring Security with JWT authentication
- **AI Integration**: Spring AI 2.0.0 (OpenAI-compatible API)
- **Build Tool**: Maven

### Key Dependencies
- `spring-boot-starter-web` - REST API framework
- `spring-boot-starter-security` - Authentication and authorization
- `spring-boot-starter-data-jpa` - Database persistence
- `spring-boot-starter-validation` - Bean validation
- `postgresql` - Database driver
- `hibernate-vector` - Vector embedding support
- `jjwt` (0.12.5) - JWT token generation and validation
- `spring-ai-starter-model-openai` - AI model integration
- `flyway-core` - Database migrations

## Prerequisites

- **Java 25** or higher
- **Maven 3.8+**
- **PostgreSQL 14+** with pgvector extension enabled
- **LM Studio** (or compatible OpenAI API endpoint) for AI model serving

## Installation & Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd backend
```

### 2. Configure Database

Create a PostgreSQL database:

```sql
CREATE DATABASE interview_prep;
```

Enable pgvector extension:

```sql
\c interview_prep
CREATE EXTENSION IF NOT EXISTS vector;
```

### 3. Configure Application Properties

Edit `src/main/resources/application.properties`:

```properties
# Database configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/interview_prep
spring.datasource.username=postgres
spring.datasource.password=your_password

# JWT Configuration (use strong secret in production)
jwt.secret=your-jwt-secret-key-change-in-production-at-least-256-bits
jwt.expiration-hours=8

# AI Model Configuration (LM Studio or OpenAI-compatible endpoint)
spring.ai.openai.base-url=http://localhost:11434/v1
spring.ai.openai.api-key=any-string-for-lm-studio
spring.ai.openai.chat.options.model=gemma4:e2b-it-qat
spring.ai.embedding.options.model=nomic-embed-text:latest

# Session timeout (hours)
interview.session.timeout-hours=2
```

### 4. Start AI Model Server

Start LM Studio or your preferred OpenAI-compatible server:

```bash
# Example with LM Studio
lm-studio serve --model nomic-embed-text:latest --chat-model gemma4:e2b-it-qat
```

Or use OpenAI API by updating the configuration:

```properties
spring.ai.openai.base-url=https://api.openai.com/v1
spring.ai.openai.api-key=your-openai-api-key
spring.ai.openai.chat.options.model=gpt-4
spring.ai.embedding.options.model=text-embedding-3-small
```

### 5. Build and Run

```bash
# Install dependencies
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

## Project Structure

```
src/main/java/com/interviewprep/
├── InterviewPrepPlatformApplication.java    # Main application entry point
├── analytics/                               # Analytics service and controller
│   ├── AnalyticsController.java
│   └── AnalyticsService.java
├── auth/                                    # Authentication controller
│   └── AuthController.java
├── config/                                  # Security, JWT, and filter configurations
│   ├── AiConfig.java
│   ├── ApiPrefixStripFilterConfig.java
│   ├── AuthenticationContext.java
│   ├── CorsConfig.java
│   ├── CsrfCookieFilter.java
│   ├── CsrfTokenCookieGenerator.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtConfig.java
│   ├── JwtRequestInterceptor.java
│   ├── JwtService.java
│   ├── ParsingUtils.java
│   ├── SecurityConfig.java
│   ├── SessionTimeoutCleanupTask.java
│   ├── SpaFallbackController.java
│   ├── ValidationError.java
│   ├── ValidationExceptionHandler.java
│   └── WebConfig.java
├── domain/                                  # JPA entities, repositories, and DTOs
│   ├── AnalyticsResponse.java
│   ├── Answer.java
│   ├── AnswerRepository.java
│   ├── AnswerRequest.java
│   ├── AuthResponse.java
│   ├── AuthStatusResponse.java
│   ├── Category.java
│   ├── CategoryRepository.java
│   ├── Evaluation.java
│   ├── EvaluationDetail.java
│   ├── EvaluationRepository.java
│   ├── EvaluationStatus.java
│   ├── InterviewFinishRequest.java
│   ├── InterviewSession.java
│   ├── InterviewSessionDetailResponse.java
│   ├── InterviewSessionRepository.java
│   ├── InterviewStartRequest.java
│   ├── InterviewStartResponse.java
│   ├── InterviewSummary.java
│   ├── LoginRequest.java
│   ├── Question.java
│   ├── QuestionRepository.java
│   ├── QuestionSummary.java
│   ├── QuestionType.java
│   ├── RegisterRequest.java
│   ├── ResultsResponse.java
│   ├── SessionStatus.java
│   ├── SessionSummary.java
│   ├── User.java
│   ├── UserPerformance.java
│   ├── UserPerformanceRepository.java
│   └── UserRepository.java
├── embedding/                               # Vector embedding service
│   └── EmbeddingService.java
├── interview/                               # Interview lifecycle controllers and services
│   ├── AnswerSubmissionController.java
│   ├── EvaluationController.java
│   ├── InterviewController.java
│   ├── InterviewResultService.java
│   ├── InterviewSetupService.java
│   ├── LanguagesTopicsController.java
│   ├── QuestionGenerationService.java
│   └── SessionDetailController.java
├── output/                                  # AI response DTOs
│   ├── EvaluationRecord.java
│   ├── QuestionRecord.java
│   └── QuestionRecordList.java
├── question/                                # AI gateway service
│   └── AiGatewayService.java
└── service/                                 # Business logic services
    ├── LanguageTopicsService.java
    └── TopicAutoSelectionService.java

src/main/resources/
├── application.properties                   # Application configuration
└── db/migration/                            # Flyway database migrations
    ├── V1__create_pgvector_extension.sql
    ├── V2__add_topic_hierarchy.sql
    ├── V3__seed_topics.sql
    └── V4__fix_question_session_fk.sql
```

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register new user account |
| POST | `/auth/login` | Login with email and password |
| POST | `/auth/logout` | Logout (clears JWT cookie) |
| GET | `/auth/status` | Check authentication status |

### Interview Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/interview/start` | Start new interview session |
| GET | `/interview/resume` | Resume active interview session |
| POST | `/interview/{sessionId}/answer` | Submit answer for a question |
| POST | `/interview/finish` | Finish interview session |
| GET | `/interview/session/{sessionId}/detail` | Get detailed session results |
| GET | `/interview/session/{sessionId}/questions` | Get questions for session |

### Evaluation

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/evaluation/retry/{answerId}` | Retry failed evaluation |

### Analytics

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/performance` | Get user performance analytics |

### Language & Topics

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/interview/languages` | List available programming languages |
| GET | `/interview/topics` | List topics for a language |

## Database Schema

The application uses PostgreSQL with the following main tables:

- **users** - User accounts with email and hashed password
- **categories** - Hierarchical category structure (Languages → Topics)
- **interview_sessions** - Interview session metadata and status
- **questions** - Generated interview questions with vector embeddings
- **answers** - User's answers to questions
- **evaluations** - AI-generated evaluations of answers
- **user_performance** - Aggregated performance metrics

See [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) for detailed schema documentation.

## Configuration

### Environment Variables

All configuration properties support environment variable overrides:

| Property | Environment Variable | Default | Description |
|----------|---------------------|---------|-------------|
| `server.port` | `SERVER_PORT` | 8080 | Server port |
| `spring.datasource.url` | `DB_URL` | - | Database URL |
| `spring.datasource.username` | `DB_USERNAME` | postgres | Database username |
| `spring.datasource.password` | `DB_PASSWORD` | postgres | Database password |
| `jwt.secret` | `JWT_SECRET` | - | JWT signing secret (256+ bits) |
| `jwt.expiration-hours` | `JWT_EXPIRATION_HOURS` | 8 | Token expiration in hours |
| `spring.ai.openai.base-url` | - | http://localhost:11434/v1 | AI model endpoint |
| `spring.ai.openai.chat.options.model` | `AI_MODEL_NAME` | gemma4:e2b-it-qat | Question generation model |
| `spring.ai.embedding.options.model` | `EMBEDDING_MODEL_NAME` | nomic-embed-text:latest | Embedding model |

### Security Configuration

- **Authentication**: JWT tokens in HttpOnly cookies (`jwt_token`)
- **Password Hashing**: BCrypt with automatic salt generation
- **Session Management**: Stateless (no server-side sessions)
- **CSRF**: Disabled (protected by SameSite=Strict cookie attribute)
- **CORS**: Configured for cross-origin requests from frontend

## Development

### Running Tests

```bash
mvn test
```

### Database Migrations

Flyway automatically runs migrations on startup. Migration files are in `src/main/resources/db/migration/`:

- `V1__create_pgvector_extension.sql` - Enable pgvector extension
- `V2__add_topic_hierarchy.sql` - Add category hierarchy support
- `V3__seed_topics.sql` - Seed initial language and topic data
- `V4__fix_question_session_fk.sql` - Backfill question session references

### AI Model Integration

The platform uses Spring AI with OpenAI-compatible endpoints. Configure your preferred AI provider:

**LM Studio (Local)**:
```properties
spring.ai.openai.base-url=http://localhost:11434/v1
spring.ai.openai.api-key=any-string-for-lm-studio
```

**OpenAI**:
```properties
spring.ai.openai.base-url=https://api.openai.com/v1
spring.ai.openai.api-key=your-api-key
spring.ai.openai.chat.options.model=gpt-4
```

## Production Deployment

### Build JAR

```bash
mvn clean package -DskipTests
```

The executable JAR will be in `target/interview-prep-platform-0.0.1-SNAPSHOT.jar`.

### Run with Docker (Optional)

```dockerfile
FROM eclipse-temurin:25-jre-alpine
COPY target/interview-prep-platform-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Security Checklist

- [ ] Change `jwt.secret` to a strong random 256-bit key
- [ ] Set `secure=true` on JWT cookie for HTTPS
- [ ] Use environment variables for sensitive configuration
- [ ] Enable database connection pooling (HikariCP is default)
- [ ] Configure proper logging and monitoring
- [ ] Set up database backups

## Troubleshooting

### AI Model Connection Issues

If you see errors related to AI model connectivity:

1. Verify LM Studio or OpenAI endpoint is running
2. Check `spring.ai.openai.base-url` configuration
3. Test the endpoint with curl: `curl http://localhost:11434/v1/models`

### Database Connection Issues

```bash
# Test PostgreSQL connection
psql -h localhost -U postgres -d interview_prep

# Verify pgvector extension
SELECT * FROM pg_extension WHERE extname = 'vector';
```

### JWT Token Issues

- Ensure `jwt.secret` is at least 256 bits (32 bytes) when base64-encoded
- Check token expiration with `jwt.expiration-hours`
- Verify cookie domain and path settings match your frontend

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions, please open an issue on GitHub or contact the development team.

---

**Note**: This is a backend API service. The frontend application is maintained in a separate repository.
