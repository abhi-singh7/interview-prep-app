## ADDED Requirements

### Requirement: Controllers have comprehensive unit tests
All REST controllers (AuthController, InterviewController, AnswerSubmissionController, EvaluationController, AnalyticsController, LanguagesTopicsController) MUST have unit test coverage using Mockito for mocking dependencies. Each controller endpoint MUST be tested with valid and invalid inputs.

#### Scenario: AuthController register endpoint validates input
- **WHEN** user sends POST /auth/register with duplicate email
- **THEN** system returns 400 Bad Request with error message "Email already in use"

#### Scenario: AuthController login validates credentials
- **WHEN** user sends POST /auth/login with invalid password
- **THEN** system returns 400 Bad Request with error message "Invalid credentials"

#### Scenario: InterviewController startInterview validates request parameters
- **WHEN** user sends POST /interview/start with difficulty=null or count=0
- **THEN** system returns 400 Bad Request without processing the request

### Requirement: Service layer has unit test coverage
All service classes (AiGatewayService, TopicAutoSelectionService, LanguageTopicsService, EmbeddingService, AnalyticsService) MUST have unit tests covering core business logic. Tests SHOULD use Mockito to mock external dependencies (repositories, AI clients).

#### Scenario: AiGatewayService generates questions with proper prompt structure
- **WHEN** generateQuestions is called with topic names and difficulty level
- **THEN** system constructs a prompt including category context, topics list, difficulty, and language name

#### Scenario: TopicAutoSelectionService handles invalid IDs gracefully
- **WHEN** autoSelectTopics is called with "invalid" string ID
- **THEN** system returns empty list instead of throwing exception

### Requirement: Configuration classes have test coverage
Security-related configuration classes (JwtService, AuthenticationContext) MUST have unit tests verifying token generation, validation, and authentication context behavior.

#### Scenario: JwtService generates valid tokens with correct claims
- **WHEN** generateToken is called with email and userId
- **THEN** returned token contains subject=email, claim userId=userId, and expiration timestamp within expected range

#### Scenario: AuthenticationContext retrieves current user ID from request attributes
- **WHEN** getCurrentUserId is called during an active request
- **THEN** system returns the Long value stored in request attributes under "currentUserId"

### Requirement: Repository layer has integration test coverage
Repository interfaces MUST have integration tests verifying query correctness against a test database using Spring Boot Test and H2/PostgreSQL Testcontainers.

#### Scenario: UserRepository.findByEmail retrieves correct user
- **WHEN** findByEmail is called with existing email address
- **THEN** system returns Optional containing the matching User entity

#### Scenario: QuestionRepository.findBySessionId filters by session
- **WHEN** findBySessionId is called with valid sessionId
- **THEN** system returns list of questions belonging to that session only
