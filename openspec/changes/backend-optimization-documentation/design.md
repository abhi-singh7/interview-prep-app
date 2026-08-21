# Backend Optimization & Documentation - Design

## Context

**Current State:**
The backend is a Spring Boot 4.0 application with Angular frontend, using PostgreSQL + pgvector for AI-powered interview preparation. The system generates and evaluates coding/theory questions via LM Studio.

**Architecture:**
```
Controllers → Services → Repositories → Database
     ↓           ↓            ↓
  Auth       Business      JPA/SQL
              Logic
```

**Identified Issues:**
1. **Test Coverage**: Only 2 service classes have tests (LanguageTopicsService, TopicAutoSelectionService)
2. **Code Duplication**: InterviewController has duplicate `parseLong` and `parseLongId` methods
3. **Large Classes**: InterviewController is 556 lines, violating Single Responsibility Principle
4. **Missing Documentation**: Zero JavaDoc comments across all classes
5. **Raw Types**: Controllers use `Map.of()` instead of typed DTOs
6. **No Validation**: Request objects lack validation annotations

**Constraints:**
- Must maintain backward compatibility with existing Angular frontend
- No breaking changes to API contracts
- PostgreSQL + pgvector dependencies must be preserved
- JWT authentication flow must remain unchanged

## Goals / Non-Goals

**Goals:**
- Achieve 80%+ test coverage on critical path classes (controllers, services)
- Add comprehensive JavaDoc for all public APIs and complex business logic
- Eliminate code duplication through extracted utilities
- Improve maintainability by decomposing large controllers
- Add input validation to request DTOs

**Non-Goals:**
- No changes to database schema or Flyway migrations
- No changes to AI/ML model endpoints (LM Studio integration)
- No frontend modifications
- No breaking API changes that would require Angular updates

## Decisions

### Decision 1: Test-First Approach Before Refactoring

**Choice**: Write tests before making any refactoring changes

**Rationale**: 
- Current codebase has minimal test coverage
- Refactoring without safety net risks introducing bugs in authentication, question generation, and evaluation flows
- Tests provide confidence for subsequent optimizations

**Alternatives Considered:**
1. *Refactor first, then test* - Risky; would need to manually verify all changes
2. *Test and refactor simultaneously* - Harder to reason about what's being tested

### Decision 2: Service Layer Extraction from InterviewController

**Choice**: Split InterviewController (556 lines) into focused service classes:
- `InterviewSetupService` - Handles start/resume logic
- `QuestionGenerationService` - Manages AI integration and question persistence
- `InterviewResultService` - Handles finish/results retrieval

**Rationale**:
- Current controller mixes REST concerns with business logic
- Each extracted service has a single responsibility
- Enables independent testing of each concern

**Alternatives Considered:**
1. *Keep as-is with JavaDoc* - Doesn't improve maintainability
2. *Split by HTTP method* - Would create controllers without clear boundaries

### Decision 3: DTO Introduction Pattern

**Choice**: Replace raw `Map.of()` calls with typed record classes:
```java
public record AuthStatusResponse(boolean authenticated) {}
public record QuestionSummary(Long id, String type, String questionText) {}
```

**Rationale**:
- Type safety for both backend and frontend
- Better IDE support (autocomplete, refactoring)
- Clear API contract documentation

**Alternatives Considered:**
1. *Use Map<String, Object>* - Currently used; loses type information
2. *Keep using raw types but add JavaDoc* - Still lacks compile-time safety

### Decision 4: Utility Extraction for Parsing Logic

**Choice**: Extract `parseLongSafe()` into a shared utility class:
```java
public final class ParsingUtils {
    public static Long parseLongSafe(String value) { ... }
}
```

**Rationale**:
- Eliminates duplicate parsing logic across controllers/services
- Centralizes error handling for invalid inputs
- Makes tests easier to write and maintain

**Alternatives Considered:**
1. *Use Optional.ofNullable().map(Long::parseLong)* - More functional but less explicit error handling
2. *Keep duplicates* - Violates DRY principle, harder to update later

### Decision 5: JavaDoc Documentation Strategy

**Choice**: Document public APIs with focus on business logic and complex scenarios:
- All controller endpoints (purpose, parameters, responses)
- Service methods with business rules explained
- Complex parsing/generation logic in AiGatewayService

**Rationale**:
- Focus documentation where it provides most value
- Avoid documenting trivial getters/setters
- Enable better IDE assistance for developers

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| **Test breaks existing behavior** | Run full test suite after each phase; use integration tests for critical paths |
| **Controller split affects API routing** | Maintain same endpoints/paths; only reorganize internal logic |
| **DTO changes break Angular frontend** | Verify TypeScript types match new response structures before merging |
| **Performance impact from validation** | Validation runs once per request; negligible overhead vs. benefits |
| **Time investment in documentation** | Prioritize critical paths first; JavaDoc improves long-term maintainability |

## Migration Plan

**Phase 1: Foundation (Tests)**
1. Add unit tests for JwtService, AuthenticationContext
2. Add integration tests for AuthController endpoints
3. Add tests for AiGatewayService question generation/evaluation
4. Verify all existing functionality still works

**Phase 2: Refactoring**
1. Extract `ParsingUtils` utility class
2. Replace duplicate parsing methods across controllers
3. Introduce DTOs (AuthStatusResponse, etc.)
4. Split InterviewController into focused services

**Phase 3: Documentation**
1. Add JavaDoc to all controller endpoints
2. Document service layer business logic
3. Add comments for complex AI prompt generation
4. Update API documentation if OpenAPI/Swagger configured

## Open Questions

1. **Should we add Swagger/OpenAPI annotations?** - Would improve API discoverability but adds another dependency
2. **What's the target test coverage percentage?** - Proposal suggests 80%, but critical auth flows may need 100%
3. **Should we add integration tests with Testcontainers?** - More realistic than mocks but slower; consider for CI/CD pipeline
