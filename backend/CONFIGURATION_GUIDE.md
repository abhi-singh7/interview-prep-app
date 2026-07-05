
# Configuration Guide

## Overview

The AI Interview Prep Platform uses Spring Boot's externalized configuration system with support for environment variables, system properties, and property files. This guide covers all configurable aspects of the application.

---

## Application Properties

### Core Configuration (`application.properties`)

```properties
# Server Configuration
server.port=${SERVER_PORT:8080}

# Database Configuration
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:interview_prep}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JWT Configuration
jwt.secret=${JWT_SECRET:your-jwt-secret-key-change-in-production-at-least-256-bits}
jwt.expiration-hours=${JWT_EXPIRATION_HOURS:8}

# Spring AI OpenAI — LM Studio endpoint (replaces ai.api.url and ai.api.key)
spring.ai.openai.base-url=http://localhost:11434/v1
spring.ai.openai.api-key=any-string-for-lm-studio

# Configurable model name via system property or environment variable
spring.ai.openai.chat.options.model=${AI_MODEL_NAME:gemma4:e2b-it-qat}

spring.ai.embedding.base-url=http://localhost:11434/v1
spring.ai.embedding.options.model=${EMBEDDING_MODEL_NAME:nomic-embed-text:latest}

# Question generation — temperature and max tokens for the questionChatClient bean
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.chat.options.max-tokens=20000

# Evaluation operations — deterministic (temperature=0) via separate ChatClient bean with its own OpenAiChatOptions
spring.ai.evaluation.temperature=0.0

# Session timeout in hours (for auto-cleanup of abandoned sessions)
interview.session.timeout-hours=2

# Flyway migration configuration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

---

## Environment Variables

All configuration properties support environment variable overrides using the `${ENV_VAR:default_value}` syntax.

### Required Environment Variables

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `SERVER_PORT` | 8080 | Application server port | `9090` |
| `DB_HOST` | localhost | PostgreSQL host | `db.example.com` |
| `DB_PORT` | 5432 | PostgreSQL port | `5432` |
| `DB_NAME` | interview_prep | Database name | `interview_prod` |
| `DB_USERNAME` | postgres | Database username | `app_user` |
| `DB_PASSWORD` | postgres | Database password | `secure_password` |
| `JWT_SECRET` | (development default) | JWT signing secret (256+ bits required for production) | Use `openssl rand -base64 32` to generate |
| `JWT_EXPIRATION_HOURS` | 8 | Token expiration in hours | `24` |

### Optional Environment Variables

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `AI_MODEL_NAME` | gemma4:e2b-it-qat | AI model for question generation | `gpt-4` |
| `EMBEDDING_MODEL_NAME` | nomic-embed-text:latest | Embedding model name | `text-embedding-3-small` |

---

## JWT Configuration

### Secret Key Generation

Generate a secure secret key for production:

```bash
# Generate 256-bit random key (base64 encoded)
openssl rand -base64 32

# Or use Java's SecureRandom
keytool -genseckey -alias jwt-secret -storetype JCEKS -keystore jwt.jks -keyalg HMACSHA256 -keysize 256 -storepass changeit -keypass changeit -validity 3650
```

### JWT Token Structure

Tokens contain the following claims:

```json
{
  "sub": "user@example.com",           // Subject (email)
  "userId": 42,                        // Custom claim (user ID)
  "iat": 1705312800,                   // Issued at timestamp
  "exp": 1705341600                    // Expiration timestamp
}
```

### Token Validation Flow

1. Extract JWT from `jwt_token` cookie
2. Verify signature using HMAC-SHA256 with configured secret
3. Check expiration timestamp
4. Parse claims to extract user ID and email
5. Populate `AuthenticationContext` for downstream use

---

## AI Model Configuration

### LM Studio Setup

1. Install [LM Studio](https://lmstudio.ai/)
2. Download a model (e.g., `gemma4:e2b-it-qat` for generation, `nomic-embed-text:latest` for embeddings)
3. Start the local server:

```bash
lm-studio serve --model nomic-embed-text:latest --chat-model gemma4:e2b-it-qat
```

4. Update configuration:

```properties
spring.ai.openai.base-url=http://localhost:11434/v1
spring.ai.openai.api-key=any-string-for-lm-studio
spring.ai.openai.chat.options.model=gemma4:e2b-it-qat
spring.ai.embedding.options.model=nomic-embed-text:latest
```

### OpenAI API Setup

1. Obtain API key from [OpenAI Platform](https://platform.openai.com/)
2. Update configuration:

```properties
spring.ai.openai.base-url=https://api.openai.com/v1
spring.ai.openai.api-key=sk-your-api-key-here
spring.ai.openai.chat.options.model=gpt-4
spring.ai.embedding.options.model=text-embedding-3-small
```

### Model Parameters

#### Question Generation (`questionChatClient`)

| Parameter | Default | Description |
|-----------|---------|-------------|
| `temperature` | 0.7 | Controls randomness (higher = more creative) |
| `max-tokens` | 20000 | Maximum response length |
| `model` | gemma4:e2b-it-qat | Model identifier |

#### Evaluation (`evaluationChatClient`)

| Parameter | Default | Description |
|-----------|---------|-------------|
| `temperature` | 0.0 | Deterministic evaluation (no randomness) |
| `model` | Same as question generation | Model identifier |

---

## Database Configuration

### PostgreSQL Connection Pooling (HikariCP)

```properties
# Connection pool settings
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# Connection validation
spring.datasource.hikari.validation-timeout=5000
spring.datasource.hikari.test-on-borrow=true
```

### pgvector Extension

Enable vector similarity search:

```sql
-- Enable extension (run once)
CREATE EXTENSION IF NOT EXISTS vector;

-- Create index for fast similarity search
CREATE INDEX idx_questions_embedding ON questions 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

### Database Migration Strategy

Flyway manages schema migrations:

```properties
# Enable Flyway
spring.flyway.enabled=true

# Migration locations
spring.flyway.locations=classpath:db/migration

# Baseline configuration
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1

# Validate on startup
spring.flyway.validate-on-migrate=true
```

---

## Security Configuration

### Password Encoding

BCrypt is used for password hashing:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**Work Factor**: Default is 10 (adjustable via constructor parameter).

### CORS Configuration

Configure allowed origins in `CorsConfig.java`:

```java
corsRegistry.addMapping("/**")
    .allowedOrigins("http://localhost:4200", "https://yourdomain.com")
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    .allowedHeaders("*")
    .allowCredentials(true)
    .maxAge(3600);
```

### CSRF Protection

CSRF is disabled because the application uses HttpOnly cookies with SameSite=Strict:

```java
http.csrf(csrf -> csrf.disable());
```

**Security Note**: This is safe because:
1. JWT tokens are stored in HttpOnly cookies (not accessible via JavaScript)
2. SameSite=Strict prevents cross-site request forgery
3. All state-changing operations require valid JWT authentication

---

## Session Management

### Interview Session Timeout

Configure session timeout for auto-cleanup:

```properties
# Session timeout in hours (default: 2)
interview.session.timeout-hours=2
```

### Background Cleanup Task

A scheduled task runs every 5 minutes to clean up abandoned sessions:

```java
@Scheduled(fixedRate = 300000) // 5 minutes
public void cleanupAbandonedSessions() { ... }
```

---

## Logging Configuration

### Log Levels

```properties
# Application logging
logging.level.com.interviewprep=INFO
logging.level.com.interviewprep.interview=DEBUG
logging.level.com.interviewprep.question=DEBUG

# Framework logging (reduce noise)
logging.level.org.springframework.security=WARN
logging.level.org.hibernate.SQL=WARN
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=WARN
```

### Log Format

```properties
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### Log File Configuration

```properties
# Enable file logging
logging.file.name=logs/interview-prep.log
logging.file.max-size=10MB
logging.file.max-history=30
```

---

## Production Configuration Example

Create `application-prod.properties`:

```properties
# Server
server.port=8080

# Database (use environment variables in production)
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JWT (use strong secret from secrets manager)
jwt.secret=${JWT_SECRET}
jwt.expiration-hours=24

# AI Models (production endpoints)
spring.ai.openai.base-url=https://api.openai.com/v1
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4
spring.ai.embedding.options.model=text-embedding-3-small

# Performance tuning
spring.datasource.hikari.maximum-pool-size=20
spring.jpa.show-sql=false

# Logging (production level)
logging.level.com.interviewprep=WARN
logging.file.name=/var/log/interview-prep/app.log
```

Activate profile:

```bash
java -jar interview-prep-platform.jar --spring.profiles.active=prod
```

---

## Development Configuration Example

Create `application-dev.properties`:

```properties
# Verbose SQL logging for debugging
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Disable CSRF for easier testing
# (already disabled in SecurityConfig)

# Local AI model
spring.ai.openai.base-url=http://localhost:11434/v1
spring.ai.openai.api-key=test-key
```

Activate profile:

```bash
java -jar interview-prep-platform.jar --spring.profiles.active=dev
```

---

## Configuration Validation

### Required Fields Check

Ensure critical configuration is present at startup:

```java
@PostConstruct
public void validateConfiguration() {
    if (jwtConfig.getJwtSecret().length() < 32) {
        throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes)");
    }
    
    if (!aiConfig.getBaseUrl().startsWith("http")) {
        throw new IllegalStateException("AI base URL must start with http:// or https://");
    }
}
```

### Runtime Configuration Check

Add health check endpoint to verify configuration:

```java
@GetMapping("/health/config")
public ResponseEntity<Map<String, Object>> configHealth() {
    Map<String, Object> status = new HashMap<>();
    
    // Check database connectivity
    try {
        entityManager.createNativeQuery("SELECT 1").getSingleResult();
        status.put("database", "connected");
    } catch (Exception e) {
        status.put("database", "disconnected: " + e.getMessage());
    }
    
    // Check AI model connectivity
    try {
        aiGatewayService.testConnection();
        status.put("ai-model", "connected");
    } catch (Exception e) {
        status.put("ai-model", "disconnected: " + e.getMessage());
    }
    
    return ResponseEntity.ok(status);
}
```

---

## Troubleshooting Configuration Issues

### JWT Validation Failures

**Symptom**: 401 Unauthorized on all authenticated endpoints

**Solution**:
1. Verify `JWT_SECRET` matches between token generation and validation
2. Check token expiration: `jwt.expiration-hours` should be reasonable
3. Ensure secret is at least 256 bits (32 bytes) when base64-encoded

### Database Connection Failures

**Symptom**: Application fails to start with database errors

**Solution**:
1. Verify PostgreSQL is running and accessible
2. Check `spring.datasource.url` format: `jdbc:postgresql://host:port/database`
3. Ensure pgvector extension is enabled: `CREATE EXTENSION IF NOT EXISTS vector;`
4. Test connection manually: `psql -h host -U user -d database`

### AI Model Connection Failures

**Symptom**: Question generation or evaluation fails with timeout errors

**Solution**:
1. Verify AI model server is running at configured URL
2. Check network connectivity: `curl http://localhost:11434/v1/models`
3. Increase timeout if needed: Add custom timeout configuration to ChatClient
4. Verify model name matches available models on the server

### CORS Errors

**Symptom**: Frontend receives CORS errors in browser console

**Solution**:
1. Update `CorsConfig.java` to include frontend origin
2. Ensure `allowCredentials(true)` is set if using cookies
3. Check that allowed methods include all HTTP methods used by frontend

---

## Configuration Best Practices

1. **Use Environment Variables for Secrets**: Never hardcode secrets in property files
2. **Validate Configuration at Startup**: Fail fast on missing required configuration
3. **Use Profile-Specific Configs**: Separate dev, test, and prod configurations
4. **Document All Configuration Options**: Maintain this guide as the source of truth
5. **Test Configuration Changes Locally**: Use Docker Compose for consistent testing
6. **Monitor Configuration in Production**: Log configuration values (excluding secrets) at startup
