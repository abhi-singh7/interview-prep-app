## ADDED Requirements

### Requirement: Duplicate utility methods are extracted to shared classes
Identical or near-identical logic across multiple classes MUST be extracted into a single shared utility class. The `parseLong` and `parseLongId` methods in InterviewController, which have identical implementations, MUST be consolidated into one method.

#### Scenario: ParsingUtils centralizes long parsing logic
- **WHEN** any controller or service needs to parse a string to Long with null/blank handling
- **THEN** system uses `ParsingUtils.parseLongSafe()` instead of duplicate inline methods

#### Scenario: Refactored code maintains identical behavior
- **WHEN** developer replaces duplicate parseLong methods with ParsingUtils call
- **THEN** application behavior remains unchanged; all existing tests continue to pass

### Requirement: Raw Map types are replaced with typed DTOs
Controller response objects using `Map.of()` MUST be replaced with proper typed record or class instances. This ensures type safety and better API contracts for the frontend.

#### Scenario: Auth controller returns typed response
- **WHEN** GET /auth/status endpoint is called
- **THEN** system returns AuthStatusResponse(boolean authenticated) instead of Map<String, Boolean>

#### Scenario: Interview result endpoints return proper DTOs
- **WHEN** GET /interview/results/{sessionId} endpoint is called
- **THEN** system returns ResultsResponse with typed evaluations list instead of raw ObjectNode

### Requirement: Large controllers are decomposed by responsibility
Controllers exceeding 200 lines MUST be refactored to separate concerns. InterviewController (556 lines) SHOULD be split into focused service classes for setup, execution, and results management.

#### Scenario: InterviewSetupService handles start/resume logic
- **WHEN** user calls POST /interview/start or GET /interview/resume
- **THEN** system delegates to InterviewSetupService which manages session creation and topic resolution

#### Scenario: QuestionGenerationService handles AI integration
- **WHEN** questions need to be generated via AI gateway
- **THEN** system uses QuestionGenerationService which encapsulates embedding generation and persistence logic

### Requirement: Request validation annotations are added
All request DTOs (RegisterRequest, LoginRequest, InterviewStartRequest, AnswerRequest) MUST include Jakarta Validation annotations (@NotBlank, @Min, etc.) to validate inputs before processing.

#### Scenario: Interview start validates difficulty field
- **WHEN** user sends POST /interview/start with null or empty difficulty
- **THEN** system returns 400 Bad Request with validation error message "Difficulty is required"

#### Scenario: Answer submission validates answer text length
- **WHEN** user sends POST /interview/{sessionId}/answer with empty answerText
- **THEN** system returns 400 Bad Request with validation error message "Answer text cannot be empty"

### Requirement: Logging follows consistent patterns
Error logging MUST include contextual information (session ID, user ID) for debugging. Warning logs SHOULD document non-critical failures gracefully (e.g., embedding generation failures).

#### Scenario: Failed embeddings log warning without crashing
- **WHEN** AI embedding generation fails during question creation
- **THEN** system logs warning with question text and continues with zero-vector fallback
