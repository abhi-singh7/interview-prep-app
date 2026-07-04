## Why

Questions created during an interview session have their `category_id` always set to NULL because `QuestionGenerationService.generateAndPersistQuestions()` discards the AI-returned topic names instead of resolving them to category IDs via `InterviewSetupService.resolveTopicNamesToIds()`. This causes two downstream problems: (1) the analytics SQL JOIN on `categories` finds no matches, so the Category Breakdown chart renders empty, and (2) the session history table shows "N/A" for the Category column instead of the actual category name.

## What Changes

- **Fix `QuestionGenerationService.generateAndPersistQuestions()`** to resolve AI response topic names to their corresponding category IDs using `InterviewSetupService.resolveTopicNamesToIds()`, and set `q.setCategoryId(resolvedId)` on each persisted question entity.
- **Inject `InterviewSetupService` (or its ID-resolution method) into `QuestionGenerationService`** so it can look up category IDs from topic name strings returned by the AI.
- **Fix frontend analytics template** to reference `session.categoryName` (the correct DTO field) instead of the non-existent `session.categoryId`, so the Category column displays actual names like "Collections", "Streams" etc.

## Capabilities

### New Capabilities
<!-- None – this is a bug fix, not a new capability -->

### Modified Capabilities
- `analytics`: Analytics page must display category *names* (not IDs or "N/A") in the session history table and Category Breakdown chart by ensuring question entities have valid categoryId values at creation time.

## Impact

**Backend:**
- `backend/src/main/java/com/interviewprep/interview/QuestionGenerationService.java` — inject dependency, replace null assignment with resolved category ID lookup
- `backend/src/main/java/com/interviewprep/domain/Question.java` — no schema change needed (field already exists as String)
- Existing question records in the DB will still have NULL categoryId; they will appear as "N/A" until new sessions create questions with proper IDs

**Frontend:**
- `frontend/src/app/analytics/analytics-page/analytics-page.component.html` — template binding: `session.categoryId` → `session.categoryName`
