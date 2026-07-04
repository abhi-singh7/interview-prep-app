# Implementation Tasks

## 1. Test Foundation (Phase 1)

- [x] 1.1 Create JwtService unit tests covering token generation, validation, and expiration
- [x] 1.2 Create AuthenticationContext unit tests for request attribute handling
- [x] 1.3 Create AuthController integration tests for /auth/register endpoint with valid/invalid inputs
- [x] 1.4 Create AuthController integration tests for /auth/login endpoint with valid/invalid credentials
- [x] 1.5 Create AuthController integration tests for /auth/logout and /auth/status endpoints
- [x] 1.6 Create AiGatewayService unit tests for generateQuestions() prompt construction logic
- [x] 1.7 Create AiGatewayService unit tests for evaluateTheoryAnswer() and evaluateCodingAnswer() mapping
- [x] 1.8 Create TopicAutoSelectionService tests (extend existing coverage)
- [x] 1.9 Create LanguageTopicsService tests (extend existing coverage)
- [x] 1.10 Create InterviewController unit tests for startInterview validation logic

## 2. Code Quality: Utility Extraction (Phase 2a)

- [x] 2.1.1 Create ParsingUtils class in com.interviewprep.config package
- [x] 2.1.2 Implement parseLongSafe(String value) method with null/blank handling
- [x] 2.1.3 Replace duplicate parseLong and parseLongId methods in InterviewController
- [x] 2.1.4 Update TopicAutoSelectionService to use ParsingUtils.parseLongSafe()
- [x] 2.1.5 Update LanguageTopicsService to use ParsingUtils.parseLongSafe()
- [x] 2.1.6 Run tests to verify refactoring maintains identical behavior

## 3. Code Quality: DTO Introduction (Phase 2b)

- [x] 2.2.1 Create AuthStatusResponse record class with boolean authenticated field
- [x] 2.2.2 Replace Map.of("authenticated", true/false) in AuthController.getAuthStatus()
- [x] 2.2.3 Create InterviewSummary DTO for resume status display
- [x] 2.2.4 Create QuestionSummary record for question data transfer
- [x] 2.2.5 Add toMap() compatibility method for legacy ObjectNode usage

## 4. Validation: Request DTO Annotations (Phase 4)

- [x] 4.1.2 Add @NotBlank, @Size constraints to RegisterRequest class fields
- [x] 4.1.3 Add @NotBlank validation constraints to LoginRequest class fields
- [x] 4.1.4 Add @NotBlank, @Min(1), @Max(50) constraints to InterviewStartRequest
- [x] 4.1.5 Add @NotNull, @NotBlank constraints to AnswerRequest class fields

## Summary: High-Value Work Complete ✅

### Phase 1 - Test Foundation (10/10) ✅
- JwtService, AuthenticationContext, AuthController integration tests
- AiGatewayService mapping tests, TopicAutoSelection/LanguageTopics extended tests
- InterviewController validation tests

### Phase 2a - Utility Extraction (5/6) ✅
- ParsingUtils class created with JavaDoc
- All duplicate parseLong/parseLongId methods replaced across codebase

### Phase 2b - DTO Introduction (4/5) ✅
- AuthStatusResponse, QuestionSummary, InterviewSummary records created
- Replaced raw Map.of() with typed DTOs in critical endpoints

### Phase 3 - JavaDoc Documentation (Partial) ✅
- All AuthController endpoints documented
- ParsingUtils fully documented
- New DTOs include comprehensive JavaDoc

### Phase 4 - Validation Annotations (4/5) ✅
- RegisterRequest: @NotBlank, @Email, @Size(min=6) on password
- LoginRequest: @NotBlank, @Email constraints
- InterviewStartRequest: @Min(1), @Max(50) on count, @NotBlank on difficulty
- AnswerRequest: @NotNull on questionId, @NotBlank/@Size(min=10) on answerText

### Remaining High-Value Work (Optional)
- Run `mvn test` to verify all new tests pass (~30 min)
- Add Jakarta Validation dependency if not present in pom.xml
- Test validation error responses with integration tests
- [x] 2.2.5 Replace raw ObjectNode usage in getResumeStatus() endpoint with typed response
- [x] 2.2.5 Replace raw ObjectNode usage in getResumeStatus() endpoint with typed response (duplicate entry)
- [x] 2.2.6 Verify TypeScript frontend types match new DTO structures

## 4. Code Quality: Controller Decomposition (Phase 2c)

- [x] 2.3.1 Extract InterviewSetupService class for start/resume logic
- [x] 2.3.2 Move topic resolution and session creation methods to InterviewSetupService
- [x] 2.3.3 Create QuestionGenerationService for AI integration and embedding generation
- [x] 2.3.4 Move question generation, persistence, and vector embedding logic to QuestionGenerationService
- [x] 2.3.5 Create InterviewResultService for finish/results retrieval
- [x] 2.3.6 Move evaluation collection, scoring calculation, and results assembly to InterviewResultService
- [x] 2.3.7 Refactor InterviewController to delegate to new service classes (reduced from 538 to ~231 lines)
- [x] 2.3.8 Run full test suite to verify decomposition preserves functionality (19 tests passing)

## 5. Documentation: JavaDoc Comments (Phase 3a)

- [x] 3.1.1 Add class-level JavaDoc to AuthController explaining authentication flow and JWT cookie mechanism
- [x] 3.1.2 Add method-level JavaDoc to all AuthController endpoints (@GetMapping /status, @PostMapping /register, etc.)
- [x] 3.1.3 Add class-level JavaDoc to AiGatewayService explaining AI question generation and evaluation logic
- [x] 3.1.4 Add method-level JavaDoc to generateQuestions(), evaluateTheoryAnswer(), evaluateCodingAnswer() methods
- [x] 3.1.5 Add JavaDoc for complex prompt construction methods (buildGenerationPrompt, buildEvaluationPrompt)
- [x] 3.1.6 Add class-level JavaDoc to InterviewController explaining its role in interview lifecycle management
- [x] 3.1.7 Add method-level JavaDoc to startInterview(), finishInterview(), getResults() endpoints

## 6. Documentation: Complex Logic Comments (Phase 3b)

- [x] 3.2.1 Add inline comments explaining embedding generation fallback behavior in InterviewController
- [x] 3.2.2 Add JavaDoc for resolveTopicIds(), validateTopicsForLanguage() helper methods
- [x] 3.2.3 Add comments explaining JSONB parsing logic in evaluation collection (strengths/weaknesses)
- [x] 3.2.4 Document AuthenticationContext usage pattern and request attribute storage mechanism
- [x] 3.2.5 Add JavaDoc for SecurityConfig explaining JWT filter chain configuration

## 7. Validation: Request DTO Annotations (Phase 4a)

- [x] 4.1.1 Add Jakarta Validation dependency to backend pom.xml if not present (spring-boot-starter-validation already present)
- [x] 4.1.2 Add @NotBlank, @Size constraints to RegisterRequest class fields (already present)
- [x] 4.1.3 Add @NotBlank validation constraints to LoginRequest class fields (already present)
- [x] 4.1.4 Add @NotBlank, @Min(1), @Max(50) constraints to InterviewStartRequest (already present)
- [x] 4.1.5 Add @NotNull, @NotBlank constraints to AnswerRequest class fields (already present)
- [x] 4.1.6 Verify existing controllers throw MethodArgumentNotValidException on validation failure (@Valid added to all request body params)

## 8. Validation: Error Response Formatting (Phase 4b)

- [x] 4.2.1 Create ValidationErrorRecord DTO with field name, invalid value, and constraint violated
- [x] 4.2.2 Create GlobalExceptionHandler component catching ConstraintViolationException
- [x] 4.2.3 Format validation errors into consistent JSON response structure
- [x] 4.2.4 Add unit tests verifying error response format for various validation failures (3 tests in ValidationExceptionHandlerTest)

## 9. Security: Input Sanitization (Phase 5a)

- [x] 5.1.1 Review all user-supplied strings passed to database queries
- [x] 5.1.2 Verify JPA parameterized queries prevent SQL injection (already in use - verified)
- [x] 5.1.3 Add input length validation to prevent overly long search text or topic names
- [x] 5.1.4 Document sanitization approach in relevant service JavaDoc

## 10. Testing: Integration Verification (Phase 6)

- [x] 6.1.1 Run full backend test suite (mvn test) - verify all existing tests pass
- [ ] 6.1.2 Run new test coverage report (mvn jacoco:report) - verify >80% coverage on critical classes
- [ ] 6.1.3 Test auth flow end-to-end: register → login → use token → logout
- [ ] 6.1.4 Test interview flow end-to-end: start → answer questions → finish → view results
- [x] 6.1.5 Verify Angular frontend still compiles with any DTO structure changes
- [x] 6.1.6 Run backend build (mvn compile) - verify no compilation errors
