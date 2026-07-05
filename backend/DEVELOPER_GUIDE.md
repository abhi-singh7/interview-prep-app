# Developer Guide

## Overview

This guide provides instructions for developers who want to contribute to the AI Interview Prep Platform. It covers development environment setup, coding standards, testing practices, and contribution guidelines.

---

## Development Environment Setup

### Prerequisites

- **Java 25** or higher (recommended: Eclipse Temurin 25)
- **Maven 3.8+**
- **PostgreSQL 14+** with pgvector extension
- **LM Studio** or compatible OpenAI API endpoint
- **Git** for version control

### IDE Setup

Recommended IDEs:
- **IntelliJ IDEA Ultimate** (best Spring Boot support)
- **VS Code** with Java Extension Pack
- **Eclipse IDE for Enterprise Java and Web Developers**

#### IntelliJ IDEA Configuration

1. Install plugins:
   - Spring Boot Assistant
   - Lombok Annotation Support
   - Database Tools and SQL

2. Configure JVM options in `Help > Edit Custom VM Options`:
   ```
   --add-opens java.base/java.lang=ALL-UNNAMED
   --add-opens java.base/java.util=ALL-UNNAMED
   ```

3. Enable annotation processing:
   - `Settings > Build, Execution, Deployment > Compiler > Annotation Processors`
   - Check "Enable annotation processing"

#### VS Code Configuration

1. Install extensions:
   - Extension Pack for Java (Microsoft)
   - Spring Boot Extension Pack (Pivotal)
   - Lombok Annotations Support (Gabriel BB)

2. Configure `settings.json`:
   ```json
   {
     "java.jdt.ls.java.home": "/path/to/jdk-25",
     "java.configuration.runtimes": [
       {
         "name": "JavaSE-25",
         "path": "/path/to/jdk-25"
       }
     ]
   }
   ```

### Database Setup

1. Install PostgreSQL:
   ```bash
   # Ubuntu/Debian
   sudo apt-get install postgresql postgresql-contrib
   
   # macOS (Homebrew)
   brew install postgresql@14
   brew services start postgresql@14
   
   # Windows
   # Download from https://www.postgresql.org/download/windows/
   ```

2. Create database and enable pgvector:
   ```bash
   psql -U postgres
   CREATE DATABASE interview_prep;
   \c interview_prep
   CREATE EXTENSION IF NOT EXISTS vector;
   \q
   ```

3. Configure application properties:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/interview_prep
   spring.datasource.username=postgres
   spring.datasource.password=your_password
   ```

### AI Model Setup (LM Studio)

1. Download and install [LM Studio](https://lmstudio.ai/)

2. Search for and download models:
   - **Question Generation**: `gemma4:e2b-it-qat` or similar
   - **Embeddings**: `nomic-embed-text:latest`

3. Start the local server:
   ```bash
   lm-studio serve --model nomic-embed-text:latest --chat-model gemma4:e2b-it-qat
   ```

4. Verify the server is running:
   ```bash
   curl http://localhost:11434/v1/models
   ```

5. Update application properties:
   ```properties
   spring.ai.openai.base-url=http://localhost:11434/v1
   spring.ai.openai.api-key=any-string-for-lm-studio
   spring.ai.openai.chat.options.model=gemma4:e2b-it-qat
   spring.ai.embedding.options.model=nomic-embed-text:latest
   ```

### Alternative: OpenAI API Setup

If you prefer using OpenAI's API instead of local models:

1. Obtain API key from [OpenAI Platform](https://platform.openai.com/)

2. Update application properties:
   ```properties
   spring.ai.openai.base-url=https://api.openai.com/v1
   spring.ai.openai.api-key=sk-your-api-key-here
   spring.ai.openai.chat.options.model=gpt-4
   spring.ai.embedding.options.model=text-embedding-3-small
   ```

---

## Building and Running the Application

### Build the Project

```bash
# Clean and build (skip tests for faster builds)
mvn clean package -DskipTests

# Run tests
mvn test

# Full build with tests
mvn clean install
```

### Run the Application

```bash
# Using Maven
mvn spring-boot:run

# Using the JAR file
java -jar target/interview-prep-platform-0.0.1-SNAPSHOT.jar

# With specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Verify Application is Running

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Test authentication
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"password123"}'
```

---

## Project Structure

```
src/main/java/com/interviewprep/
├── InterviewPrepPlatformApplication.java    # Main entry point
├── analytics/                               # Analytics functionality
│   ├── AnalyticsController.java             # REST endpoints for analytics
│   └── AnalyticsService.java                # Business logic for analytics
├── auth/                                    # Authentication
│   └── AuthController.java                  # User registration, login, logout
├── config/                                  # Configuration classes
│   ├── AiConfig.java                        # AI model configuration
│   ├── SecurityConfig.java                  # Spring Security setup
│   ├── JwtService.java                      # JWT token generation/validation
│   ├── JwtAuthenticationFilter.java          # JWT request filter
│   └── ... (other config classes)
├── domain/                                  # Domain models and repositories
│   ├── User.java                            # User entity
│   ├── InterviewSession.java                # Session entity
│   ├── Question.java                        # Question entity with embeddings
│   ├── Answer.java                          # Answer entity
│   ├── Evaluation.java                      # Evaluation entity
│   └── ... (other entities and repositories)
├── interview/                               # Interview management
│   ├── InterviewController.java             # Main interview endpoints
│   ├── QuestionGenerationService.java       # AI question generation
│   ├── InterviewResultService.java          # Result aggregation
│   └── ... (other interview-related classes)
├── output/                                  # AI response DTOs
│   ├── QuestionRecord.java                  # Question output schema
│   ├── EvaluationRecord.java                # Evaluation output schema
│   └── QuestionRecordList.java              # List wrapper for OpenAI
├── question/                                # AI gateway
│   └── AiGatewayService.java               # Bridge to AI models
└── service/                                 # Business logic services
    ├── LanguageTopicsService.java           # Language/topic management
    └── TopicAutoSelectionService.java       # Auto-select topics
```

---

## Coding Standards

### Java Conventions

Follow [Oracle's Java Code Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-introduction.html) with these additional guidelines:

#### Naming Conventions

- **Classes**: PascalCase (`InterviewController`, `QuestionGenerationService`)
- **Methods**: camelCase (`getQuestionsForSession`, `generateAndPersistQuestions`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_TOKENS`, `DEFAULT_TIMEOUT_HOURS`)
- **Variables**: camelCase (`sessionId`, `questionText`)

#### Documentation Standards

All public classes, methods, and fields must have Javadoc comments:

```java
/**
 * Service handling AI-powered question generation and vector embedding.
 * 
 * <p>Encapsulates the complete question lifecycle: invoking AiGatewayService
 * to generate questions, creating Question entities with topic metadata,
 * generating vector embeddings via EmbeddingService, and persisting all
 * generated questions linked to a session.</p>
 * 
 * @see AiGatewayService
 * @see EmbeddingService
 */
@Service
public class QuestionGenerationService {

    /**
     * Generates questions for a new interview session via AI and persists them.
     * 
     * <p>This method orchestrates the full question generation pipeline:
     * 1) Calls AiGatewayService to generate QUESTION_RECORD objects
     * 2) Maps each record to a Question entity with type, text, title/description/codePrompt
     * 3) Generates vector embeddings (with zero-vector fallback on failure)
     * 4) Persists all questions linked to the given session</p>
     * 
     * @param sessionId the interview session ID to link generated questions to
     * @param topicNames list of topic names for AI prompt context
     * @param languageNameForPrompt the programming language name for coding challenge prompts
     * @param difficulty the difficulty level string (e.g., "easy", "medium", "hard")
     * @param count number of questions to generate
     * @return list of persisted Question entities
     */
    public List<Question> generateAndPersistQuestions(Long sessionId, ...) {
        // Implementation
    }
}
```

#### Code Organization

- **One class per file**: Each public class in its own file named after the class
- **Imports order**: 
  1. Java standard library (`java.*`)
  2. Third-party libraries (`org.*`, `com.*`)
  3. Project imports (`com.interviewprep.*`)
- **Method ordering**: 
  1. Public methods (API surface)
  2. Protected methods
  3. Private helper methods
  4. Static initializers

#### Error Handling

Use Spring's exception handling mechanisms:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ex.getMessage()));
    }
}
```

#### Logging

Use SLF4J with Spring Boot's logging:

```java
@Slf4j
@Service
public class QuestionGenerationService {

    public List<Question> generateQuestions(...) {
        log.info("Generating {} questions for session {}", count, sessionId);
        
        try {
            // Business logic
            log.debug("Successfully generated {} questions", questions.size());
            return questions;
        } catch (Exception e) {
            log.error("Failed to generate questions for session {}", sessionId, e);
            throw new QuestionGenerationException("Failed to generate questions", e);
        }
    }
}
```

---

## Testing Strategy

### Unit Tests

Write unit tests for business logic using JUnit 5 and Mockito:

```java
@ExtendWith(MockitoExtension.class)
class QuestionGenerationServiceTest {

    @Mock
    private AiGatewayService aiGatewayService;
    
    @Mock
    private EmbeddingService embeddingService;
    
    @Mock
    private QuestionRepository questionRepository;
    
    @InjectMocks
    private QuestionGenerationService service;

    @Test
    void shouldGenerateAndPersistQuestions() {
        // Given
        Long sessionId = 1L;
        List<String> topicNames = List.of("Core Java", "Generics");
        when(aiGatewayService.generateQuestions(any(), anyInt(), anyString(), anyList()))
                .thenReturn(List.of(questionRecord));
        
        // When
        List<Question> questions = service.generateAndPersistQuestions(
                sessionId, topicNames, "Java", "medium", 5);
        
        // Then
        verify(questionRepository).saveAll(anyList());
        assertEquals(5, questions.size());
    }
}
```

### Integration Tests

Test database interactions and API endpoints:

```java
@SpringBootTest
@AutoConfigureMockMvc
class InterviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Test
    void shouldStartInterviewSuccessfully() throws Exception {
        // Given
        User user = createUser();
        String token = jwtService.generateToken(user.getEmail(), user.getId());
        
        InterviewStartRequest request = new InterviewStartRequest();
        request.setTopicIds(List.of("11", "12"));
        request.setDifficulty("medium");
        request.setCount(3);
        
        // When/Then
        mockMvc.perform(post("/interview/start")
                .header("Cookie", "jwt_token=" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.questions").isArray());
    }
}
```

### Test Database Setup

Use H2 in-memory database for tests:

```properties
# application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.flyway.enabled=false  # Disable migrations for tests
```

---

## Git Workflow

### Branching Strategy

Follow Git Flow or simplified branching:

```
main (production)
  ├── develop (integration)
  │   ├── feature/add-evaluation-retry
  │   ├── feature/improve-question-generation
  │   └── bugfix/fix-session-timeout
  └── release/v1.0.0
```

### Commit Message Convention

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Build process or auxiliary tool changes

**Examples**:
```
feat(interview): add evaluation retry endpoint

Add POST /evaluation/retry/{answerId} endpoint to allow users
to retry failed evaluations.

Closes #123
```

```
fix(question): handle null embedding gracefully

When AI model returns empty response, use zero-vector fallback
instead of throwing NullPointerException.
```

### Pull Request Process

1. Create feature branch from `develop`
2. Make changes and commit with conventional commits
3. Push to remote and create PR
4. Fill out PR template:
   - Description of changes
   - Related issues
   - Testing performed
   - Screenshots (if UI changes)
5. Request review from at least one team member
6. Address review comments
7. Merge after approval

---

## Debugging Guide

### Common Issues and Solutions

#### Database Connection Errors

**Symptom**: `org.postgresql.util.PSQLException: FATAL: password authentication failed`

**Solution**:
1. Verify database credentials in `application.properties`
2. Check PostgreSQL is running: `pg_isready -h localhost -p 5432`
3. Test connection manually: `psql -U postgres -d interview_prep`

#### AI Model Connection Failures

**Symptom**: `java.net.ConnectException: Connection refused`

**Solution**:
1. Verify LM Studio or OpenAI endpoint is running
2. Check network connectivity: `curl http://localhost:11434/v1/models`
3. Verify model name matches available models on server
4. Increase timeout if needed in `AiConfig.java`

#### JWT Token Validation Errors

**Symptom**: `io.jsonwebtoken.security.SignatureException: JWT signature does not match`

**Solution**:
1. Ensure `JWT_SECRET` is consistent between token generation and validation
2. Verify secret is at least 256 bits (32 bytes) when base64-encoded
3. Check token expiration: `jwt.expiration-hours` should be reasonable

#### Vector Embedding Errors

**Symptom**: `org.postgresql.util.PSQLException: ERROR: column "embedding" does not exist`

**Solution**:
1. Ensure pgvector extension is enabled: `CREATE EXTENSION IF NOT EXISTS vector;`
2. Verify Question entity has `@JdbcTypeCode(SqlTypes.VECTOR)` annotation
3. Check database schema matches entity definitions

### Debugging Tools

#### Spring Boot Actuator

Enable actuator endpoints for monitoring:

```properties
# application.properties
management.endpoints.web.exposure.include=health,info,metrics,env,loggers
management.endpoint.health.show-details=always
```

Access endpoints:
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Environment: `http://localhost:8080/actuator/env`

#### Database Debugging

Use pgAdmin or DBeaver to inspect database state:

```sql
-- Check active sessions
SELECT * FROM interview_sessions WHERE status = 'ACTIVE';

-- Verify question embeddings exist
SELECT id, question_text, embedding IS NOT NULL as has_embedding 
FROM questions LIMIT 10;

-- Monitor vector similarity search performance
EXPLAIN ANALYZE 
SELECT id, question_text, 
       1 - (embedding <=> '[0.1, 0.2, ...]'::vector) as similarity
FROM questions 
ORDER BY embedding <=> '[0.1, 0.2, ...]'::vector 
LIMIT 5;
```

#### Logging Configuration for Debugging

Enable debug logging for specific packages:

```properties
# application.properties
logging.level.com.interviewprep.interview=DEBUG
logging.level.com.interviewprep.question=DEBUG
logging.level.org.springframework.security=DEBUG
```

---

## Performance Optimization

### Database Query Optimization

1. **Use Indexes**: Ensure foreign keys and frequently queried columns have indexes
2. **Avoid N+1 Queries**: Use `@EntityGraph` or JOIN FETCH for related entities
3. **Batch Operations**: Use `saveAll()` instead of individual saves
4. **Pagination**: Implement pagination for list endpoints

```java
// Good: Batch save
questionRepository.saveAll(questions);

// Bad: Individual saves in loop
for (Question q : questions) {
    questionRepository.save(q);
}
```

### AI Model Performance

1. **Caching**: Cache frequently accessed questions and embeddings
2. **Async Processing**: Use `@Async` for non-critical AI operations
3. **Connection Pooling**: Configure HikariCP for database connections
4. **Timeout Configuration**: Set appropriate timeouts to prevent hanging requests

```java
// Async evaluation processing
@Async
public void evaluateAnswerAsync(Answer answer) {
    try {
        Evaluation evaluation = aiGatewayService.evaluateCodingAnswer(answer.getQuestion(), answer);
        evaluationRepository.save(evaluation);
    } catch (Exception e) {
        log.error("Failed to evaluate answer {}", answer.getId(), e);
    }
}
```

### Memory Management

1. **Lazy Loading**: Use `FetchType.LAZY` for large object graphs
2. **Stream Processing**: Use Java Streams for large collections
3. **Connection Management**: Ensure database connections are properly closed
4. **GC Tuning**: Monitor and tune garbage collection for production

---

## Security Best Practices

### Authentication Security

1. **Strong JWT Secrets**: Use at least 256-bit random secrets in production
2. **Token Expiration**: Set reasonable expiration times (8-24 hours)
3. **HTTPS Only**: Enforce HTTPS in production to protect JWT cookies
4. **HttpOnly Cookies**: Store JWTs in HttpOnly, SameSite=Strict cookies

### Input Validation

1. **Bean Validation**: Use `@Valid` and validation annotations on DTOs
2. **SQL Injection Prevention**: Use parameterized queries (JPA handles this)
3. **XSS Prevention**: Sanitize user input before rendering
4. **Rate Limiting**: Implement rate limiting for authentication endpoints

```java
// Input validation example
@Data
public class InterviewStartRequest {
    @NotBlank(message = "Difficulty is required")
    private String difficulty;
    
    @Min(value = 1, message = "Count must be at least 1")
    @Max(value = 50, message = "Count cannot exceed 50")
    private int count;
}
```

### Data Protection

1. **Password Hashing**: Use BCrypt with appropriate work factor
2. **Sensitive Data Logging**: Never log passwords or JWT tokens
3. **Database Encryption**: Consider encrypting sensitive columns at rest
4. **Backup Security**: Encrypt database backups and secure storage

---

## Contributing Guidelines

### Before You Start

1. Fork the repository
2. Clone your fork locally
3. Create a new branch for your feature/fix
4. Make your changes following coding standards
5. Write tests for new functionality
6. Update documentation if needed
7. Submit a pull request

### Code Review Checklist

- [ ] Code follows project conventions and style guidelines
- [ ] All public methods have Javadoc comments
- [ ] Error handling is appropriate and consistent
- [ ] Tests are written and passing
- [ ] Documentation is updated for new features
- [ ] No sensitive data in code or commits
- [ ] Performance implications considered
- [ ] Security considerations addressed

### Getting Help

If you encounter issues:
1. Check existing documentation (README, ARCHITECTURE, CONFIGURATION_GUIDE)
2. Search GitHub Issues for similar problems
3. Ask questions in project discussions
4. Reach out to maintainers via email or Slack

---

## Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [PostgreSQL pgvector Extension](https://github.com/pgvector/pgvector)
- [LM Studio Documentation](https://lmstudio.ai/docs)
- [OpenAI API Documentation](https://platform.openai.com/docs)
