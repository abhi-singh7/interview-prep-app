## Context

The interview setup form currently has hardcoded category/topic dropdown values (6 categories) and a hardcoded programming language dropdown (3 options). Both are embedded directly in Angular templates with no backend support. Adding a new language or topic requires code changes, database seed updates, and redeployment. Users can only select one category at a time for an interview session, which limits their ability to mix topics across domains.

The categories table exists but is unused as a foreign key — it has no type distinction between languages and topics, no parent-child relationship, and the categoryId field in InterviewSession/Question entities is stored as a free-text string rather than a proper FK reference.

## Goals / Non-Goals

**Goals:**
- Make all dropdown values (languages, topics) dynamic from backend APIs
- Support hierarchical categories: languages with sub-topics via type + parent_id columns
- Allow users to select multiple topics for a single interview session
- Provide searchable multi-select for topics with debounced API calls after 2-3 characters typed
- Allow LLM auto-selection of topics when user selects no specific topics (based on selected language)
- Display topic names instead of IDs in results page

**Non-Goals:**
- Admin CRUD UI for managing languages and topics (seed data via Flyway only)
- Topic-based analytics or performance tracking changes
- Migration of existing interview sessions to use new topicIds format
- Real-time collaboration on shared interviews

## Decisions

### Decision 1: Extend categories table with type + parent_id instead of creating separate tables
**Choice**: Add `type` (ENUM: LANGUAGE/TOPIC) and `parent_id` (FK → categories.id) columns to the existing categories table.

**Rationale**: 
- The categories table already exists and is referenced in seed data — extending it avoids unnecessary new entities
- Keeps query patterns simple: single-table hierarchical queries with parent_id
- No new API endpoints needed for a separate languages table — topics endpoint can serve both needs
- Reduces migration complexity compared to creating a new `languages` + `language_topics` junction table

**Alternatives considered**:
1. Separate `languages` table + `language_topics` junction table: More normalized but adds complexity (one more entity, one more API endpoint, one more migration file). Not justified for the current scale of data (~5 languages with ~3-6 topics each).
2. JSON column in languages for topics: Simpler than junction table but loses query capabilities (can't easily search/filter topics by name across all languages without full JSON parsing on DB side).

### Decision 2: topicIds as nullable array in POST /interview/start — empty/null = auto-select
**Choice**: Change `categoryId: string` to `topicIds: string[]` in the request body. When user selects no topics (empty array or null), LLM auto-selects from available sub-topics of the selected language.

**Rationale**:
- Arrays are the natural type for multi-selection — a single string cannot represent multiple values
- Nullable/empty allows graceful degradation: if user doesn't care about specific topics, let LLM decide based on context
- Maintains backward compatibility with the existing AI prompt structure (LLM just receives topic names instead of category ID)

**Alternatives considered**:
1. Keep categoryId but add optional topicIds array: This would create ambiguity — which takes precedence? Cleaner to replace entirely.
2. Require topic selection always: Forces user to make decisions they may not want to make, reducing usability for casual users.

### Decision 3: Debounced search on topics dropdown with 500ms delay after 2 characters typed
**Choice**: Topics dropdown searches via API after typing 2-3 characters with a 500ms debounce timer.

**Rationale**:
- 500ms is standard for UX — fast enough to feel responsive, slow enough to avoid excessive requests
- 2-character minimum prevents premature API calls on first keystroke (e.g., "a" matches everything)
- Filtering client-side first, then making API call only when needed reduces unnecessary backend load

### Decision 4: Coding language dropdown uses same `/interview/languages` endpoint as setup form
**Choice**: Both the interview setup form and the coding question component use the same `GET /interview/languages` endpoint to populate their respective dropdowns.

**Rationale**:
- Eliminates duplication — single source of truth for programming languages
- If a new language is added, both dropdowns automatically get it without separate changes
- Reduces maintenance burden

### Decision 5: Show auto-selected topics via toast notification when user submits without selecting specific topics
**Choice**: When topicIds array is empty and LLM auto-selects, show an Angular Material snackbar/Toast to the user explaining what was selected.

**Rationale**:
- User should know what topics their interview will cover even if they didn't explicitly select them
- Toast provides non-blocking feedback — doesn't interrupt flow but informs decision-making
- Aligns with existing toast usage pattern in the app (used for error messages)

## Risks / Trade-offs

**[Risk]**: Breaking change to `POST /interview/start` API contract — clients using the old `categoryId` field will fail.
→ **Mitigation**: This is a controlled deployment with frontend changes going together. No public-facing client exists outside this app, so no external breaking change risk. Document the migration in the Flyway migration notes.

**[Risk]**: Circular FK dependency — categories.parent_id references categories.id, but parent_id will be NULL for both top-level languages and standalone topics (System Design, Behavioral).
→ **Mitigation**: parent_id is nullable. Standalone topics have parent_id=NULL, which is semantically correct — they are not sub-topics of anything.

**[Risk]**: AI prompt needs to change from receiving a category ID string to receiving topic names. LLM behavior may shift because it now receives multiple topic names instead of one.
→ **Mitigation**: Update the AiGatewayService prompt template to include topic names in natural language format ("Topics: Core Java, Generics, Collections") rather than IDs. Test with both single-topic and multi-topic prompts before deployment.

**[Risk]**: Existing interview sessions will have categoryId as a raw number (1-6) that won't match the new category IDs after migration.
→ **Mitigation**: This is accepted — existing sessions are historical data. The categoryId in existing InterviewSession/Question rows becomes orphaned but doesn't break anything since it's only used for AI prompts during question generation, not for display or logic.

**[Risk]**: Topic search API could be slow with large datasets if parent_id queries aren't indexed.
→ **Mitigation**: Add a database index on categories.parent_id after the migration: `CREATE INDEX idx_categories_parent_id ON categories(parent_id)`. This is included in the migration script.
