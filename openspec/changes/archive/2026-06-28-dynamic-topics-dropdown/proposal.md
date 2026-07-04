## Why

The interview setup form currently has hardcoded category/topic and programming language dropdown values, making it impossible to add new languages or topics without code changes. Users need dynamic selection of languages with their associated sub-topics (e.g., selecting "Java" should show Core Java, Generics, Collections as selectable topics), and the ability to select multiple topics for a single interview session.

## What Changes

- **BREAKING**: `POST /interview/start` request body changes from `categoryId: string` to `topicIds: string[]` (nullable array). Empty/null array means LLM auto-selects topics based on the selected language.
- New backend API: `GET /interview/languages` — returns all available programming languages as options for dropdowns
- New backend API: `GET /interview/topics?langId={id}&search={text}` — returns sub-topics filtered by parent language and optional search text
- Categories table extended with `type` (ENUM: LANGUAGE/TOPIC) and `parent_id` columns to support hierarchy
- Flyway migration to add new columns and seed data for 5 languages with their sub-topics
- Frontend interview setup form: language dropdown loads from API, topics multi-select with search loads dynamically based on selected language
- Frontend coding question component: programming language dropdown loads from the same `/interview/languages` endpoint
- Results page: displays topic names instead of raw category IDs for evaluation details
- Auto-selected topics shown to user via toast notification when they submit without selecting specific topics

## Capabilities

### Modified Capabilities

- `interview-setup`: Setup page fields change — single category dropdown replaced with language dropdown + multi-select searchable topics dropdown. API contract for `/interview/start` changes from `categoryId: string` to `topicIds: string[]`. Results display changes from showing IDs to topic names.

## Impact

**Backend:**
- Categories entity and repository extended (new columns)
- New controller endpoints under `/interview/languages` and `/interview/topics`
- InterviewController.startInterview request/response contract change (**BREAKING**)
- AiGatewayService prompt generation updated to accept topics instead of single category
- Results endpoint returns topic names alongside IDs

**Frontend:**
- InterviewSetupComponent: complete form rewrite — language dropdown + searchable multi-select topics
- CodingQuestionComponent: hardcoded language dropdown replaced with dynamic source
- New DropdownDataService service for fetching languages and topics
- InterviewSignalService API method signatures updated (categoryId → topicIds)
- Results page: resolve category IDs to names before display

**Database:**
- Flyway migration required (V2__add_topic_hierarchy.sql)
- Seed data added for 5 languages with sub-topics (~30+ new rows)
