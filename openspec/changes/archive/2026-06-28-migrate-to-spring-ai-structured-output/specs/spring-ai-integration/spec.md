## ADDED Requirements

### Requirement: Spring AI ChatClient handles LLM communication
The system SHALL use Spring AI's `ChatClient` fluent API to communicate with the OpenAI-compatible LLM backend (LM Studio) instead of raw RestTemplate calls. All HTTP request/response handling, including headers and authentication, SHALL be managed by Spring AI internally.

#### Scenario: Generate questions via ChatClient
- **WHEN** a question generation request is made through `AiGatewayService.generateQuestions()`
- **THEN** the method uses `ChatClient.prompt().system().user().call().entity()` to send the prompt and receive structured output, with no manual HTTP handling

#### Scenario: Evaluate answer via ChatClient
- **WHEN** an evaluation request is made through `AiGatewayService.evaluateTheoryAnswer()` or `AiGatewayService.evaluateCodingAnswer()`
- **THEN** the method uses `ChatClient.prompt().system().user().call().entity()` to send the prompt and receive structured output, with no manual HTTP handling

### Requirement: Question generation temperature configurable via properties
The question generation ChatClient SHALL use a temperature value configurable through application.properties under `spring.ai.openai.chat.options.temperature` (default 0.7). The model name SHALL be configurable through `spring.ai.openai.chat.options.model`.

#### Scenario: Default question generation temperature
- **WHEN** no custom temperature is configured in application.properties
- **THEN** question generation uses temperature=0.7 as the default

#### Scenario: Custom model via properties
- **WHEN** a different model name is set in `spring.ai.openai.chat.options.model`
- **THEN** that model is used for all requests through the question generation ChatClient

### Requirement: Evaluation operations use deterministic temperature
The evaluation ChatClient SHALL use a temperature value configurable through application.properties under `spring.ai.evaluation.temperature` (default 0.0). The same configurable model name from the generation client applies to evaluation as well.

#### Scenario: Default evaluation temperature
- **WHEN** no custom evaluation temperature is configured in application.properties
- **THEN** evaluation uses temperature=0.0 for deterministic results

#### Scenario: Custom max tokens via properties
- **WHEN** a different max tokens value is set in `spring.ai.openai.chat.options.max-tokens`
- **THEN** that value is used for all requests through both ChatClients

### Requirement: Native structured output enabled for LM Studio responses
The system SHALL enable native structured output via the `response_format.json_schema` parameter when calling LM Studio, using Spring AI's `.entity()` method with DTO types. The LLM response schema SHALL be auto-generated from the DTO type and sent to the API. Non-conforming JSON triggers automatic retry (via OpenAiChatOptions validation config) — in Spring AI 2.x this is done via `.useProviderStructuredOutput().validateSchema()`.

#### Scenario: Question generation with native structured output
- **WHEN** `generateQuestions()` is called
- **THEN** the ChatClient sends the request with `response_format` containing the BeanOutputConverter-generated JSON schema, and the response is auto-retried on validation failure up to 3 times

#### Scenario: Evaluation with native structured output
- **WHEN** either evaluation method is called
- **THEN** the same native structured output + retry mechanism applies for consistent type safety

### Requirement: OpenAI-compatible endpoint configuration via Spring Boot properties
The system SHALL configure the LM Studio endpoint using Spring Boot auto-configured properties under `spring.ai.openai.base-url` (default `http://localhost:1234/v1`) and `spring.ai.openai.api-key` (any string value for LM Studio).

#### Scenario: Default endpoint configuration
- **WHEN** no custom base URL is configured in application.properties
- **THEN** the ChatClient connects to `http://localhost:1234/v1/chat/completions` by default

#### Scenario: Custom API key for LM Studio
- **WHEN** a value is set in `spring.ai.openai.api-key`
- **THEN** that value is used as the Bearer token for authentication with LM Studio
