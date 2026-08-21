## ADDED Requirements

### Requirement: All request DTOs have Jakarta Validation annotations
All request body classes (RegisterRequest, LoginRequest, InterviewStartRequest, AnswerRequest) MUST include validation constraints (@NotBlank, @NotNull, @Min, etc.) to validate inputs at the controller layer.

#### Scenario: RegisterRequest validates email format
- **WHEN** user sends POST /auth/register with malformed email address
- **THEN** system returns 400 Bad Request with constraint violation message "Email is not valid"

#### Scenario: RegisterRequest validates password length
- **WHEN** user sends POST /auth/register with password less than 6 characters
- **THEN** system returns 400 Bad Request with constraint violation message "Password must be at least 6 characters"

#### Scenario: InterviewStartRequest validates count range
- **WHEN** user sends POST /interview/start with count of 51 (exceeds maximum)
- **THEN** system returns 400 Bad Request with validation error "Count cannot exceed 50 questions"

### Requirement: Validation errors return consistent response format
When validation fails, the API MUST return a standardized error response structure containing field name, invalid value, and constraint violated. This enables frontend to display targeted validation messages.

#### Scenario: Controller returns ValidationErrorResponse on failure
- **WHEN** any request DTO violates its validation constraints
- **THEN** system returns 400 status with JSON body containing list of ValidationError objects (field, message)

#### Scenario: Global exception handler processes ConstraintViolationException
- **WHEN** Jakarta Validation throws ConstraintViolationException during deserialization
- **THEN** Spring @ControllerAdvice catches the exception and formats response consistently

### Requirement: Authentication context validates user ownership
Endpoints accessing user-specific resources MUST verify that the authenticated user owns or has permission to access the requested resource. This prevents horizontal privilege escalation attacks.

#### Scenario: Answer submission verifies session ownership
- **WHEN** POST /interview/{sessionId}/answer is called with sessionId belonging to another user
- **THEN** system returns 403 Forbidden instead of processing the answer

#### Scenario: Interview results verify session access
- **WHEN** GET /interview/results/{sessionId} is called for a session not owned by authenticated user
- **THEN** system returns 403 Forbidden or 404 Not Found (no information leakage)

### Requirement: Input sanitization prevents injection attacks
All user-supplied strings MUST be validated and sanitized before database queries. While JPA Parameterized Queries prevent SQL injection, additional validation for XSS and other injection vectors SHOULD be applied where appropriate.

#### Scenario: Topic search text doesn't allow malicious payloads
- **WHEN** GET /languages/{languageId}/topics?search=<script>alert('xss')</script> is called
- **THEN** system treats the input as a literal string for database LIKE query without executing it

### Requirement: Rate limiting protects against abuse
Authentication endpoints (POST /auth/register, POST /auth/login) SHOULD implement rate limiting to prevent brute force attacks and credential stuffing. The system MUST reject more than 100 requests per minute from a single IP address on auth endpoints.

#### Scenario: Auth endpoint enforces rate limit
- **WHEN** user makes more than 100 login attempts within one minute window
- **THEN** system returns 429 Too Many Requests with retry-after header
