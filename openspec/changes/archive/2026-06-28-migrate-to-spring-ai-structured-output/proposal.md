## Why

The backend LLM communication layer uses a raw `RestTemplate` with manual JSON parsing of responses from an OpenAI-compatible API endpoint (LM Studio running gemma-4-12b-qat). This approach is fragile, verbose (~90+ lines of parsing code), and error-prone. Spring AI 2.0.0 provides native structured output support that eliminates manual JSON parsing entirely — the framework deserializes LLM responses directly into strongly-typed Java objects. Additionally, Jackson 3 (bundled with Spring Boot 4) requires migration from Jackson 2 patterns, but this is an opportunity to adopt cleaner, more robust patterns across the board.

## What Changes

- Replace `RestTemplate` calls in `AiGatewayService` with Spring AI's `ChatClient` fluent API
- Eliminate all manual JSON parsing methods (`parseQuestionsFromResponse`, `parseEvaluationFromResponse`, `stripMarkdownCodeBlocks`) — replaced by `.entity()` method with Java record DTOs
- Enable native structured output for LM Studio (gemma-4 supports it via the `response_format` parameter)
- Add Spring AI OpenAI starter dependency — configures an OpenAI-compatible client pointing to LM Studio endpoint (`http://localhost:1234/v1`)
- Replace `ObjectMapper` with Jackson 3's immutable `JsonMapper.builder()` pattern (Jackson package rename: `com.fasterxml.jackson` → `tools.jackson`)
- Create Java record DTOs for structured output: `QuestionRecord` and `EvaluationRecord`
- Configure separate ChatClient beans for question generation (temperature=0.7) and evaluation (temperature=0.0) — all via properties, nothing hardcoded
- Remove `AiConfig.AiGatewayService.callAiApi()` method — Spring AI handles HTTP communication
- Update Jackson imports: `com.fasterxml.jackson` → `tools.jackson`, `JsonNode` → `TreeNode`, `ArrayNode` → `ArrayTreeNode`, `ObjectNode` → `ObjectTreeNode`
- Replace `JsonProcessingException` catch with `JacksonException` (unchecked exception in Jackson 3)

## Capabilities

### New Capabilities

- **spring-ai-integration**: Spring AI ChatClient integration for LLM communication — replaces manual RestTemplate calls. Enables native structured output, auto HTTP management, and typed response deserialization via `.entity()`.
- **structured-output-dtos**: Java record DTOs (`QuestionRecord`, `EvaluationRecord`) with Jackson annotations for type-safe structured output from LLM responses.

### Modified Capabilities

<!-- No existing spec-level capability requirements are changing — this is a new integration -->

## Impact

**Affected code:**
- `backend/src/main/java/com/interviewprep/question/AiGatewayService.java` — complete rewrite of LLM communication layer (~396 lines → ~150 lines)
- `backend/src/main/java/com/interviewprep/config/AiConfig.java` — refactor from manual RestTemplate config to Spring AI bean configuration
- All Jackson imports throughout codebase — package rename from `com.fasterxml.jackson` to `tools.jackson`

**New dependencies:**
- `org.springframework.ai:spring-ai-openai-spring-boot-starter` (managed by Spring Boot 4)

**Removed dependencies:**
- Manual RestTemplate usage in AiGatewayService (eliminated)
- Explicit `jackson-databind` dependency — now managed by Spring Boot 4 automatically

**Config changes:**
- Existing properties (`ai.api.url`, `ai.api.key`, `ai.model.generation`, `ai.generation.temperature`, `ai.evaluation.temperature`, `ai.generation.max-tokens`) mapped to Spring AI equivalents (`spring.ai.openai.base-url`, `spring.ai.openai.api-key`, `spring.ai.openai.chat.options.model`, etc.)
- Model name remains configurable via properties — **not hardcoded**
