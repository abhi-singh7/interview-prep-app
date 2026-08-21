## ADDED Requirements

### Requirement: QuestionRecord DTO for question generation structured output
The system SHALL define a `QuestionRecord` Java record with the following fields, all derived from the BeanOutputConverter-generated JSON schema sent to LM Studio:

- `type`: String — "THEORY" or "CODE" (required)
- `categoryId`: String — category identifier (required)
- `questionText`: String — full question text (required for THEORY questions)
- `title`: String — short title (optional, present only for CODE questions)
- `description`: String — detailed problem description (optional, present only for CODE questions)
- `codePrompt`: String — starter code with method signature (optional, present only for CODE questions)

Each field SHALL be annotated with `@JsonPropertyDescription` to guide LM Studio's schema generation.

#### Scenario: Parse question generation response as list of QuestionRecord
- **WHEN** the LLM returns a JSON array of question objects via native structured output
- **THEN** Spring AI's `.entity(new ParameterizedTypeReference<List<QuestionRecord>>() {})` deserializes it directly into a typed List without manual JSON parsing

#### Scenario: Parse question generation response as container record for OpenAI compatibility
- **WHEN** the LLM returns a JSON object with a "questions" array field (for providers that don't support top-level arrays via native structured output)
- **THEN** `.entity(QuestionList.class)` where QuestionList wraps `List<QuestionRecord>` deserializes correctly

### Requirement: EvaluationRecord DTO for evaluation structured output
The system SHALL define an `EvaluationRecord` Java record with the following fields, all derived from the BeanOutputConverter-generated JSON schema sent to LM Studio:

- `score`: int — score 0-10 (required)
- `strengths`: List\<String\> — what went well (required)
- `weaknesses`: List\<String\> — areas for improvement (required)
- `improvedAnswer`: String — better version of the answer or improved solution code (required)
- `isCorrect`: Boolean — whether the coding solution is algorithmically correct (optional, null for theory evaluation)
- `correctnessExplanation`: String — explanation of correctness assessment (optional, null for theory evaluation)
- `timeComplexity`: String — algorithmic time complexity (optional, null for theory evaluation)
- `spaceComplexity`: String — algorithmic space complexity (optional, null for theory evaluation)

Each field SHALL be annotated with `@JsonPropertyDescription` to guide LM Studio's schema generation.

#### Scenario: Parse theory evaluation response as EvaluationRecord
- **WHEN** the LLM returns a JSON object of theory evaluation fields via native structured output
- **THEN** Spring AI's `.entity(EvaluationRecord.class)` deserializes it directly with null-safe handling for coding-only fields (isCorrect, correctnessExplanation, timeComplexity, spaceComplexity)

#### Scenario: Parse coding evaluation response as EvaluationRecord
- **WHEN** the LLM returns a JSON object of full coding evaluation fields via native structured output
- **THEN** Spring AI's `.entity(EvaluationRecord.class)` deserializes it directly with all fields populated

### Requirement: Mapping from EvaluationRecord to domain Evaluation entity
The system SHALL map `EvaluationRecord` deserialized by Spring AI back into the existing domain `Evaluation` entity, populating all corresponding fields without manual JSON parsing.

#### Scenario: Map theory evaluation record to domain entity
- **WHEN** a theory evaluation returns an EvaluationRecord with score, strengths, weaknesses, improvedAnswer (and null coding fields)
- **THEN** the mapped Evaluation entity has score set correctly, strengths/weaknesses serialized as JSON strings, and isCorrect set to null

#### Scenario: Map coding evaluation record to domain entity
- **WHEN** a coding evaluation returns an EvaluationRecord with all fields populated including isCorrect, timeComplexity, spaceComplexity
- **THEN** the mapped Evaluation entity has all corresponding fields set correctly including Boolean isCorrect and string complexities
