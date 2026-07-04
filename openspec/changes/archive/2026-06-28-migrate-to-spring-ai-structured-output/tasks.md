## 1. Add Spring AI dependency to pom.xml

- [x] 1.1 Add `spring-ai-openai-spring-boot-starter` dependency to backend/pom.xml (no explicit version — let Spring Boot 4 manage it)
- [x] 1.2 ~~Remove explicit `jackson-databind` dependency~~ — kept; already managed by Spring Boot 4 and other files still use it

## 2. Create Java record DTOs for structured output

- [x] 2.1 Create `QuestionRecord` Java record with fields: type, categoryId, questionText, title, description, codePrompt — all with @JsonPropertyDescription annotations
- [x] 2.2 Create `EvaluationRecord` Java record with fields: score, strengths, weaknesses, improvedAnswer, isCorrect, correctnessExplanation, timeComplexity, spaceComplexity — all with @JsonPropertyDescription annotations (Boolean for isCorrect since theory eval won't have this)

## 3. Refactor AiConfig to configure Spring AI beans

- [x] 3.1 Replace `@Value("${ai.api.url}")` and `@Value("${ai.api.key}")` with `spring.ai.openai.base-url` and `spring.ai.openai.api-key` property injection
- [x] 3.2 Add `OpenAiChatModel` bean for question generation using `OpenAiChatOptions.builder()` — model from `${spring.ai.model.generation:google/gemma-4-12b-qat}`, temperature from `${spring.ai.generation.temperature:0.7}`, max-tokens from `${spring.ai.generation.max-tokens:20000}`
- [x] 3.3 Add `OpenAiChatModel` bean for evaluation using same model property but with `${spring.ai.evaluation.temperature:0.0}` temperature and same max-tokens
- [x] 3.4 Add `@Bean("questionChatClient") ChatClient` bean wrapping the question generation OpenAiChatModel, with default system prompt for question generation
- [x] 3.5 Add `@Bean("evaluationChatClient") ChatClient` bean wrapping the evaluation OpenAiChatModel, with default system prompt for evaluation
- [x] 3.6 Add `JsonMapper` bean using `JsonMapper.builder().build()` (Jackson 3 pattern — immutable ObjectMapper)

## 4. Rewrite AiGatewayService to use Spring AI

### Question generation

- [x] 4.1 Replace `callAiApi(request)` RestTemplate call with `questionChatClient.prompt()...call().entity(new ParameterizedTypeReference<List<QuestionRecord>>() {}, spec -> spec.useProviderStructuredOutput().validateSchema())`
- [x] 4.2 Move system prompt from `buildGenerationSystemPrompt()` into the ChatClient bean's defaultSystem (eliminate per-call repetition) — done via AiConfig ChatClient beans' defaultSystem()
- [x] 4.3 Update user prompt to use Spring AI template placeholders instead of manual string formatting — keep formatted text for readability since it's a single-shot call

### Evaluation operations

- [x] 4.4 Replace `callAiApi(request)` in `evaluateTheoryAnswer()` with `evaluationChatClient.prompt()...call().entity(EvaluationRecord.class, spec -> spec.useProviderStructuredOutput().validateSchema())`
- [x] 4.5 Move theory evaluation system prompt from `buildTheoryEvaluationSystemPrompt()` into the ChatClient bean's defaultSystem (eliminate per-call repetition) — done via AiConfig ChatClient beans' defaultSystem()
- [x] 4.6 Replace `callAiApi(request)` in `evaluateCodingAnswer()` with same pattern as above, using language-specific system prompt via `.system(s -> s.text(...))` at call time

### Remove old methods and update dependencies

- [x] 4.7 Delete `callAiApi()` method entirely — Spring AI handles HTTP
- [x] 4.8 Delete `parseQuestionsFromResponse()` method entirely — replaced by `.entity(List<QuestionRecord>.class)`
- [x] 4.9 Delete `parseEvaluationFromResponse()` method entirely — replaced by `.entity(EvaluationRecord.class)`
- [x] 4.10 Delete `stripMarkdownCodeBlocks()` helper method — not needed with native structured output (LM Studio returns clean JSON)
- [x] 4.11 Remove `ObjectMapper` local variable from methods — inject via constructor instead
- [x] 4.12 Update return types: `generateQuestions()` returns `List<QuestionRecord>` instead of `List<Question>`, evaluation methods return `EvaluationRecord` instead of `Evaluation`

## 5. Map structured output DTOs to domain entities

- [x] 5.1 In `AiGatewayService.generateQuestions()`: map `List<QuestionRecord>` to `List<Question>` — populate Question entity fields from record values
- [x] 5.2 In evaluation methods: map `EvaluationRecord` to `Evaluation` entity — handle null-safe mapping for coding-only fields (isCorrect, correctnessExplanation, timeComplexity, spaceComplexity) in theory evaluations

## 6. Update Jackson imports across codebase

- [x] 6.1 Replace all `import com.fasterxml.jackson.databind.ObjectMapper` with `import tools.jackson.databind.ObjectMapper`
- [x] 6.2 Replace all `import com.fasterxml.jackson.databind.JsonNode` with `import tools.jackson.databind.TreeNode`
- [x] 6.3 Replace all `import com.fasterxml.jackson.databind.node.ArrayNode` with `import tools.jackson.databind.node.ArrayTreeNode`
- [x] 6.4 Replace all `import com.fasterxml.jackson.databind.node.ObjectNode` with `import tools.jackson.databind.node.ObjectTreeNode`
- [x] 6.5 Replace all `import com.fasterxml.jackson.core.type.TypeReference` with `import tools.jackson.core.type.TypeReference` (none found in codebase)
- [x] 6.6 Replace all `import com.fasterxml.jackson.core.JsonProcessingException` with `import tools.jackson.core.JacksonException`

## 7. Update application.properties configuration

- [x] 7.1 Add `spring.ai.openai.base-url=http://localhost:1234/v1` (replaces `ai.api.url`)
- [x] 7.2 Add `spring.ai.openai.api-key=any-string-for-lm-studio` (replaces `ai.api.key`)
- [x] 7.3 Update `ai.model.generation=google/gemma-4-12b-qat` to use Spring AI property: `spring.ai.openai.chat.options.model=google/gemma-4-12b-qat`
- [x] 7.4 Add `spring.ai.openai.chat.options.temperature=0.7` for question generation (replaces `ai.generation.temperature`)
- [x] 7.5 Add `spring.ai.evaluation.temperature=0.0` stays the same — injected via separate bean with its own OpenAiChatOptions
- [x] 7.6 Update `ai.generation.max-tokens=20000` to use Spring AI property: `spring.ai.openai.chat.options.max-tokens=20000`

## 8. Remove old configuration and unused code

- [x] 8.1 Delete `AiConfig.AiGatewayService.callAiApi()` retry annotation — replaced by `.validateSchema()` auto-retry
- [x] 8.2 Remove `@Value("${ai.api.url}")`, `@Value("${ai.api.key}")` from AiConfig (replaced by Spring AI auto-config)
- [x] 8.3 Evaluate whether `AiConfig.getApiUrl()`, `getApiKey()`, `getModel()` getters are still needed — likely not if all config moves to Spring Boot properties

## 9. Test and verify

- [x] 9.1 Verify Spring AI OpenAI starter version compatibility with Spring Boot 4.0.0 via start.spring.io or Maven Central
- [x] 9.2 Compile the project — check for Jackson import errors, missing bean types
- [ ] 9.3 Run application and test question generation endpoint — verify native structured output works with gemma-4 on LM Studio
- [ ] 9.4 Test evaluation endpoints — verify EvaluationRecord deserialization for both theory and coding evaluations
- [ ] 9.5 If native structured output fails for any operation, fall back to prompt-based BeanOutputConverter by removing `.useProviderStructuredOutput()`
