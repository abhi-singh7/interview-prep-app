## Context

The question creation flow (`POST /interview/start`) generates questions via AI, which returns topic names (e.g., "Collections", "Streams") as strings. `QuestionGenerationService` reads these but unconditionally sets `categoryId = null`. The method in `InterviewSetupService.resolveTopicNamesToIds()` that would convert topic names back to category IDs exists but is never called during question persistence.

Downstream, the analytics page runs native SQL queries joining `questions.category_id::text` against `categories.id::text`, and displaying `session.categoryId` (a non-existent field) in the HTML template instead of `session.categoryName`. Result: empty Category Breakdown chart and "N/A" category column.

**Constraints:**
- `Question.categoryId` is a plain `String`, not a JPA relationship — no FK constraint at Hibernate level.
- Existing question records already have NULL categoryId; we only fix new sessions going forward.
- Spring Boot 4.0 + Lombok @Data on DTOs (LSP false positives are expected).

## Goals / Non-Goals

**Goals:**
1. Every newly created Question entity must have a non-null `categoryId` derived from its topic names via existing resolution logic.
2. Analytics page displays actual category names in both the chart and session history table.

**Non-Goals:**
- Backfilling categoryId on pre-existing question rows.
- Converting `Question.categoryId` to a JPA relationship — keeping it as String for simplicity.
- Adding new database migrations or schema changes.

## Decisions

### Decision 1: Inject `InterviewSetupService` into `QuestionGenerationService`

**Choice:** Add `InterviewSetupService` (or its ID-resolution capability) as a constructor dependency of `QuestionGenerationService`, and call `resolveTopicNamesToIds(topicNames)` inside the question creation loop.

**Rationale:**
- `InterviewSetupService.resolveTopicNamesToIds()` already exists and does exactly what's needed: takes topic name strings, looks up categories with type="TOPIC", returns comma-separated IDs as a string.
- This avoids duplicating category lookup logic or creating a new method elsewhere.
- Constructor injection follows existing patterns in the codebase (other services are already Spring-managed beans).

**Alternatives considered:**
- Create a dedicated `CategoryIdResolver` service — over-engineered for a single one-line call to an existing method.
- Use topic names directly as categoryId — would break the analytics SQL JOIN which compares against `categories.id`.

### Decision 2: Handle empty/null topics gracefully

**Choice:** If `record.topics()` is null or empty, leave `categoryId` as null (existing behavior). Only set it when the AI returned valid topics.

**Rationale:**
- Some question types or edge cases may not produce topic assignments from the AI. We shouldn't crash or guess in those cases.
- Matches existing defensive pattern: only update when data is available.

### Decision 3: Frontend template fix — change `session.categoryId` to `session.categoryName`

**Choice:** Simple one-line edit in the analytics HTML template binding from `session.categoryId` to `session.categoryName`.

**Rationale:**
- The DTO field is `categoryName`; the template references a non-existent property. This is a straightforward mismatch fix.
- No additional transformation needed on the backend — the SQL query already returns category names (when categoryId is populated).

## Risks / Trade-offs

[Risk: Existing questions still show N/A] → Mitigation: Acceptable for this change scope; backfilling can be a future task. New sessions will have proper category IDs immediately.

[Risk: AI sometimes omits topic names from response] → Mitigation: Null/empty guard already in place; no data corruption, just null categoryId which gracefully degrades to "N/A" rather than wrong values.
