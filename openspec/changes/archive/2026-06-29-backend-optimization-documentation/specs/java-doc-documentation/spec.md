## ADDED Requirements

### Requirement: All public classes have JavaDoc documentation
All public classes (controllers, services, configuration classes) MUST include class-level JavaDoc comments explaining purpose, responsibility, and key behavior. Private helper methods MAY be documented if they contain non-obvious logic.

#### Scenario: Controller class documents its API contract
- **WHEN** developer reads AuthController class definition
- **THEN** class-level JavaDoc explains that the controller manages authentication flows with JWT cookies

#### Scenario: Service class documents business rules
- **WHEN** developer reads AiGatewayService class definition
- **THEN** class-level JavaDoc explains the service handles AI question generation and evaluation via Spring AI ChatClient

### Requirement: All public methods have JavaDoc documentation
All public methods MUST include method-level JavaDoc with parameter descriptions, return value explanation, and exceptions thrown. Complex business logic methods SHOULD document algorithmic approach or important edge cases.

#### Scenario: Controller endpoint documents parameters and response
- **WHEN** developer reads @PostMapping("/register") method signature
- **THEN** JavaDoc explains the RegisterRequest structure and response format (AuthResponse with userId, email)

#### Scenario: Service method documents business logic
- **WHEN** developer reads generateQuestions() method in AiGatewayService
- **THEN** JavaDoc explains prompt construction including category context, topics, difficulty, and language

### Requirement: Complex logic has inline comments
Non-obvious algorithmic logic (parsing utilities, embedding generation, AI prompt building) MUST have inline comments explaining the rationale or edge cases handled.

#### Scenario: Parsing utility documents null/blank handling
- **WHEN** developer reads parseLongSafe() method implementation
- **THEN** comment explains that null/blank strings return null instead of throwing NumberFormatException

#### Scenario: Embedding generation documents fallback behavior
- **WHEN** developer reads embedding generation code in InterviewController.startInterview()
- **THEN** comment explains that failed embeddings are replaced with zero arrays to prevent persistence errors

### Requirement: API endpoints have Swagger/OpenAPI documentation
Public REST endpoints SHOULD include @Operation annotations or equivalent documentation for API discoverability via Swagger UI (if configured). Endpoint descriptions MUST explain purpose and expected behavior.

#### Scenario: Auth endpoint documents authentication flow
- **WHEN** Swagger UI renders /auth/register endpoint
- **THEN** description explains that this creates a new user account and returns JWT cookie
