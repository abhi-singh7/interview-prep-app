## Context

The backend communicates with an LLM via a raw `RestTemplate` POST to an OpenAI-compatible endpoint (`http://localhost:1234/v1/chat/completions`). LM Studio runs the model locally (gemma-4-12b-qat). Three operations require LLM responses — question generation and two types of evaluation (theory/coding) — all parsed manually from JSON string responses. The project is on Spring Boot 4.0.0 + Java 25, which aligns with Spring AI 2.0.0's baseline requirements.

## Goals / Non-Goals

**Goals:**
- Replace manual `RestTemplate` calls and JSON parsing with Spring AI ChatClient fluent API
- Enable native structured output for gemma-4 via LM Studio (confirmed supported) — eliminates prompt-injected format instructions
- Create type-safe Java record DTOs (`QuestionRecord`, `EvaluationRecord`) for structured deserialization
- Migrate Jackson 2 patterns to Jackson 3: package rename, immutable JsonMapper builder pattern, unchecked exceptions
- Keep all model/temperature/config configurable via properties — nothing hardcoded
- Maintain separate ChatClient beans for generation (temp=0.7) and evaluation (temp=0.0)

**Non-Goals:**
- Migrating to a different LLM provider or backend
- Adding new AI capabilities beyond structured output (no tool calling, RAG, etc.)
- Migrating the entire codebase Jackson usage — only changes needed for this migration
- Streaming responses — current implementation is synchronous only

## Decisions

### Decision 1: Use Spring AI OpenAI Starter with LM Studio as OpenAI-compatible endpoint

**Choice**: `spring-ai-openai-spring-boot-starter` configured to point at LM Studio's `/v1/chat/completions` endpoint.

**Rationale**: 
- Spring AI 2.0 consolidated OpenAI from 3 variants (Azure, HTTP, SDK) to **1 variant (SDK)**. The openai module uses the official OpenAI Java SDK internally — not a generic HTTP client.
- LM Studio's `/v1/chat/completions` endpoint is fully OpenAI-compatible (confirmed by LM Studio docs). The structured output feature works via `response_format: {type: "json_schema", json_schema: {...}}`.
- This gives us native structured output support without needing a separate Ollama module.

**Alternatives considered**:
- `spring-ai-ollama-spring-boot-starter` — would also work since LM Studio is compatible with OpenAI API, but Spring AI 2.0's Ollama integration is community-maintained (not core), while the openai module is actively maintained by the Spring team.

### Decision 2: Native structured output via `response_format.json_schema` — not prompt-injected format instructions

**Choice**: Enable native structured output using `.entity()` with DTO types, configured via OpenAiChatOptions responseFormat (Spring AI 1.x approach). **Requires manual JSON schema definition per DTO type** since Spring AI 1.x lacks automatic schema generation from Java types. Upgrade to Spring AI 2.x for auto-generated schemas via `.useProviderStructuredOutput().validateSchema()`.

**Rationale**:
- LM Studio confirmed supports JSON schema enforcement for GGUF models via llama.cpp grammar-based sampling.
- Gemma-4 specifically has documented "structured JSON output" support — Google's official announcement lists it as a native capability.
- Native structured output sends the schema to the API (not as prompt text), eliminating prompt-injected format instructions, reducing token usage, and providing stronger guarantees than prompt-based approaches.

**Alternatives considered**:
- Default BeanOutputConverter (prompt-based) — works universally but wastes tokens with format instructions and is less reliable. Use as fallback only if native structured output fails for gemma-4 on LM Studio.

### Decision 3: Two separate ChatClient beans — one per temperature configuration

**Choice**: `questionChatClient` (temp=0.7) and `evaluationChatClient` (temp=0.0), both using the same configurable model name.

**Rationale**:
- Different operations need different temperatures — question generation is creative (0.7), evaluation must be deterministic (0.0).
- Spring AI 2.0 options are immutable once instantiated at ChatModel level, so per-call customization would require rebuilding ChatClient each time.
- Two beans cleanly separate concerns and avoid runtime temperature override logic.

**Alternatives considered**:
- Single ChatClient with `.options()` override per call — but this requires passing customizers on every invocation, which is error-prone and verbose.
- Per-operation options in application.properties — Spring AI 2.0 doesn't support multiple option profiles for the same model type natively.

### Decision 4: Use Java records as structured output DTOs with `@JsonPropertyDescription` annotations

**Choice**: Define `QuestionRecord` and `EvaluationRecord` as Java records (immutable) with Jackson annotations on fields to guide LLM schema generation.

**Rationale**:
- Records are concise, immutable, and carry data structure clearly — Spring AI recommends them for structured output.
- `@JsonPropertyDescription("...")` on record components injects descriptions into the generated JSON schema sent to LM Studio — critical for ensuring the model understands field semantics (e.g., distinguishing "score" 0-10 from time complexity strings).
- Jackson annotations in `jackson-annotations` stay at version 2.x, so they work with both Jackson 2 and Jackson 3 components.

**Alternatives considered**:
- Regular Java classes with Lombok — but records are more concise for this use case (data carriers only) and Spring AI's BeanOutputConverter works equally well with records.

### Decision 5: Inject `JsonMapper` bean instead of creating local instances

**Choice**: Create a single `JsonMapper` bean in AiConfig, inject it into AiGatewayService — no local instantiation.

**Rationale**:
- Jackson 3's ObjectMapper is immutable after construction. Creating local instances would be wasteful and could lead to inconsistent configuration if configurations diverge.
- Spring-injected bean ensures consistent JsonMapper across the application with a single source of truth for configuration (builder pattern).

## Risks / Trade-offs

[Risk] Gemma-4's native structured output via LM Studio may not enforce schema strictly — smaller models can still return non-conforming JSON even with `strict: true`.
[Trade-off] Use `.validateSchema()` which retries up to 3 times on validation failure. If the model consistently fails, falls back to prompt-based BeanOutputConverter.

[Risk] Spring AI OpenAI SDK uses a different HTTP client internally than RestTemplate — connection pooling, timeouts, and error handling differ.
[Trade-off]: Acceptable because Spring AI handles this better (connection reuse via SDK) and eliminates manual HTTP management entirely.

[Risk] Jackson 3 package rename (`com.fasterxml.jackson` → `tools.jackson`) is pervasive — affects all Jackson usage in the codebase, not just AiGatewayService.
[Trade-off]: Do it incrementally — only update imports where needed during migration. Third-party libraries using Jackson 2 internally should NOT be renamed (their jars already contain the right classes).

[Risk] The `spring-ai-openai-spring-boot-starter` version must match Spring Boot 4.0.x managed dependencies — wrong version causes dependency conflicts.
[Trade-off]: No explicit version tag in pom.xml — let Spring Boot parent manage it via dependency management.

## Migration Plan

1. Add `spring-ai-openai-spring-boot-starter` dependency to pom.xml (version managed by Spring Boot 4)
2. Create Java record DTOs: QuestionRecord, EvaluationRecord
3. Refactor AiConfig — replace @Value RestTemplate config with ChatClient bean configuration
4. Rewrite AiGatewayService — replace RestTemplate + manual parsing with ChatClient + .entity()
5. Update Jackson imports throughout codebase (com.fasterxml.jackson → tools.jackson)
6. Remove explicit jackson-databind dependency from pom.xml (managed by Spring Boot 4)
7. Map existing application.properties to Spring AI property names

## Open Questions

1. What is the exact Spring AI version managed by Spring Boot 4.0.0? Need to verify via Maven Central / start.spring.io compatibility matrix before implementation.
2. Does LM Studio's `/v1/chat/completions` endpoint accept `response_format.json_schema` for GGUF models loaded in its server mode? Confirmed yes per LM Studio docs, but worth testing after migration.
3. Should evaluation operations also use native structured output or rely on prompt-based (since temperature=0 is already deterministic)? Native structured output provides stronger guarantees regardless of temperature — recommend using it for both.
