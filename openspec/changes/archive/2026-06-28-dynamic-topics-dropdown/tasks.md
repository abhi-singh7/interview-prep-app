## 1. Database Migration — Categories Hierarchy

- [x] 1.1 Create Flyway migration V2__add_topic_hierarchy.sql to add `type` (VARCHAR(10)) and `parent_id` (BIGINT, nullable FK → categories.id) columns to the categories table with CHECK constraint on type values ('LANGUAGE', 'TOPIC')
- [x] 1.2 Add database index idx_categories_parent_id ON categories(parent_id) for efficient topic lookup by parent language
- [x] 1.3 Create Flyway migration V3__seed_topics.sql to populate seed data: Java (Core Java, Generics, Collections, Streams, OOP, Concurrency), Python (Core Python, Data Structures, Web Frameworks, Decorators), SQL (Joins, Indexes, Normalization, Window Functions, Query Optimization), Angular (Components, Services, RxJS, Forms, Routing), Spring Boot (REST APIs, Security, JPA/Hibernate, Actuator)
- [x] 1.4 Verify migration runs cleanly with `mvn flyway:migrate` and all seed data is present

## 2. Backend — Entity & Repository Updates

- [x] 2.1 Update Category entity: add `type` field (String) and `parentId` field (Long, nullable) with JPA annotations
- [x] 2.2 Add getCategoryHierarchy() method to CategoryRepository returning categories ordered by type then name for dropdown display
- [x] 2.3 Add findByType(String type) method to CategoryRepository — returns all categories of a given type (LANGUAGE or TOPIC)
- [x] 2.4 Add findByParentIdAndNameContainingIgnoreCase(Long parentId, String search) method to CategoryRepository — supports debounced topic search filtered by parent language

## 3. Backend — Service Layer: Topic Auto-Selection

- [x] 3.1 Create `TopicAutoSelectionService` class with method autoSelectTopics(String languageId) that returns all TOPIC-type categories whose parent_id matches the given language ID
- [x] 3.2 Create `LanguageTopicsService` class with methods getLanguages() and getTopicsByLanguageAndSearch(languageId, searchText) — wraps CategoryRepository queries for API consumption

## 4. Backend — Controller: New API Endpoints

- [x] 4.1 Add `GET /interview/languages` endpoint returning list of {id, name} objects where category type = LANGUAGE (use LanguageTopicsService)
- [x] 4.2 Add `GET /interview/topics?langId={id}&search={text}` endpoint returning filtered topics — langId filters by parent_id, search is optional substring match on topic name

## 5. Backend — TDD Tests for New Services & Endpoints

- [x] 5.1 Create LanguageTopicsServiceTest with unit tests: getLanguages() returns only LANGUAGE type; getTopicsByLanguageAndSearch() filters correctly; autoSelectTopics() returns all child topics of a language
- [x] 5.2 Create InterviewStartRequestValidatorIntegrationTest with integration tests: valid topicIds in selected language → pass; empty topicIds → allow auto-select; invalid topicIds not belonging to selected language → return 400

## 6. Backend — Update Existing InterviewController for topicIds

- [x] 6.1 Change InterviewStartRequest DTO: replace `categoryId` field with `topicIds` (List<String>, nullable)
- [x] 6.2 Update InterviewController.startInterview(): validate that all provided topicIds exist and belong to the selected language; if topicIds is empty/null, call TopicAutoSelectionService.autoSelectTopics()
- [x] 6.3 Update AiGatewayService.buildGenerationPrompt(): change prompt template to receive topic names (from Category entities) instead of categoryId string — format as "Topics: {topic1}, {topic2}, ..." when multiple topics are provided

## 7. Backend — Update Results Endpoint for Topic Names

- [x] 7.1 Update InterviewController.getResults() or results response DTO to include resolved topic names alongside category IDs
- [x] 7.2 Add CategoryRepository method findByIdIn(List<Long> ids) and findByTypeAndIdList(String type, List<Long> ids) to efficiently resolve category names for display

## 8. Frontend — New DropdownDataService

- [x] 8.1 Create `DropdownDataService` service with methods: getLanguages() → Observable<{id: number, name: string}[]>; getTopicsByLanguage(langId) → Observable<{id: number, name: string}[]>; searchTopics(langId, searchText) → Observable<{id: number, name: string}[]>
- [x] 8.2 Add Angular Material modules for multi-select dropdowns (MatChipsModule, MatAutocompleteModule) to app imports

## 9. Frontend — Update Interview Setup Form

- [x] 9.1 Replace hardcoded category dropdown with language dropdown bound to DropdownDataService.getLanguages()
- [x] 9.2 Add searchable multi-select topics dropdown: when language changes → call getTopicsByLanguage(); on input change after 500ms debounce → call searchTopics() if ≥2 characters typed; display selected topics as Angular Material chips with remove button
- [x] 9.3 Update form group: replace `categoryId` FormControl with `languageId` (string) and `topicIds` (array<string>, nullable/empty = auto-select)
- [x] 9.4 Update onStartInterview(): send topicIds array instead of categoryId in API call; show toast notification when auto-selected topics are used

## 10. Frontend — Update Coding Question Component

- [x] 10.1 Replace hardcoded language dropdown with dynamic source from DropdownDataService.getLanguages()
- [x] 10.2 Set default selectedLanguage to first available language from API response instead of hardcoded 'Java'

## 11. Frontend — Update InterviewSignalService

- [x] 11.1 Change startInterview method signature: replace categoryId parameter with topicIds (string[])
- [x] 11.2 Update HTTP request body to include topicIds array alongside languageId, difficulty, and count

## 12. Frontend — Update Results Page for Topic Names

- [x] 12.1 Add CategoryService or use existing service method to resolve category IDs to names after fetching results
- [x] 12.2 Update results display template: show resolved topic name alongside or instead of categoryId in evaluation details

## 13. Verification — Run Both Frontend and Backend

- [ ] 13.1 Start backend with `cd backend && mvn spring-boot:run` — verify Flyway migrations run, endpoints respond correctly
- [ ] 13.2 Start frontend with `npm start` — verify all dropdowns load dynamically from APIs
- [ ] 13.3 Test full flow: select language → topics appear → search for specific topic → multi-select multiple topics → submit interview → verify topics shown in results
