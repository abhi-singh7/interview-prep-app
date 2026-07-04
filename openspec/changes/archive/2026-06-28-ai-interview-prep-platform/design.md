## Context

Current state: Empty workspace with no code. This change will build a full-stack interview preparation platform from scratch — Angular frontend + Spring Boot backend + PostgreSQL database. The project is greenfield, no migrations or existing dependencies to consider.

## Goals / Non-Goals

**Goals:**
- User authentication via JWT stored in httpOnly cookies (registered users only)
- Dynamic question generation by AI at interview session start based on topic, difficulty, and count
- Dual question types: theory (text answers) and coding challenges (code submissions)
- Server-side session persistence with configurable timeout auto-cleanup; resume option for interrupted sessions
- Post-interview batch evaluation of all answers via single OpenAI-compatible endpoint with language-specific deterministic prompts
- Retry logic for AI failures — 3 attempts with exponential backoff (5s, 10s, 20s); partial results viewable on failure
- End-of-session results display: overall score, per-question feedback, solutions for coding challenges
- Basic performance analytics dashboard: session history, average scores, category breakdown via bar charts

**Non-Goals:**
- Real-time code execution (no sandboxed JVM or test case runner — AI evaluates logic only)
- Multiple AI providers (single OpenAI-compatible endpoint only)
- Speech-to-text integration
- Adaptive/LLM-driven interview flow (fixed question count set at session start)
- Multi-language UI localization
- Mobile app
- Code editor with Monaco/Syntax highlighting — textarea for now

## Decisions

**Decision 1: httpOnly cookie JWT over localStorage**
- Rationale: Protects against XSS token theft. Requires CSRF handling but is significantly more secure.
- Alternative considered: localStorage (simpler but vulnerable to XSS).
- Chosen because auth security is foundational — better to handle CSRF complexity early than retrofit later.

**Decision 2: Angular Signals + RxJS over NgRx for state management**
- Rationale: Less boilerplate, sufficient for the app's state complexity. Signals provide reactive UI updates; RxJS handles HTTP streams and event orchestration during interviews.
- Alternative considered: NgRx (more structured but heavy boilerplate for a single-developer project).

**Decision 3: ng2-charts over @angular/material/charts for analytics**
- Rationale: Simpler integration — just npm install + module import, no additional Angular Material chart infrastructure needed.
- Alternative considered: @angular/material/charts (more integrated but requires more setup).

**Decision 4: PostgreSQL with pgvector extension over separate vector database**
- Rationale: Single service to manage instead of two (PostgreSQL + Pinecone/Weaviate). pgvector provides semantic search capability for questions without adding infrastructure complexity.
- Alternative considered: Separate vector store (Pinecone, Weaviate) — adds operational overhead and cost for MVP.

**Decision 5: Deterministic AI calls (temperature=0) only for evaluation, not question generation**
- Rationale: Evaluation consistency is critical — same answer must get the same score every time. Question generation benefits from randomness to provide variety across sessions.
- Language-specific deterministic prompts for coding challenges in Java, Python, and JavaScript/Angular.

**Decision 6: Textarea over Monaco editor for code input**
- Rationale: Simpler MVP path. Monaco adds syntax highlighting but also bundle size and complexity. Can be upgraded later.
- Alternative considered: Monaco Editor — better UX but significant additional dependency and maintenance burden.

**Decision 7: Session timeout as configurable constant (not DB-driven)**
- Rationale: Simple configuration via application.properties. No need for per-user customization at MVP stage. Scheduled task runs every 5 minutes to clean expired sessions.
- Alternative considered: Per-user timeout setting — adds complexity without proportional benefit for MVP.

**Decision 8: Batch evaluation after session end vs real-time per-question**
- Rationale: Simpler UX and implementation — user answers all questions, then clicks "Finish Interview" → receives all evaluations at once. No need to handle streaming responses or partial UI updates during the interview itself.
- Alternative considered: Real-time evaluation per question (like LeetCode) — requires streaming API support, more complex state management.

## Risks / Trade-offs

**Risk 1: AI API failures during batch evaluation**
→ Mitigation: Retry logic with exponential backoff (3 attempts). Partial results viewable on failure. User can retry failed evaluations manually after session ends.

**Risk 2: Single AI provider lock-in**
→ Mitigation: Encapsulate AI calls behind an `AiGatewayService` interface. Adding a new provider later is just implementing the same contract — no invasive changes needed.

**Risk 3: Textarea for coding challenges lacks syntax highlighting and IDE features**
→ Mitigation: Acceptable for MVP. Can upgrade to Monaco Editor in a future change without changing backend or data models.

**Risk 4: Deterministic evaluation may feel unfair if prompts are too rigid**
→ Mitigation: Carefully crafted deterministic prompts that explicitly instruct the AI to evaluate algorithmic thinking, not syntax/style. Language-specific templates ensure context-awareness for each language.

**Risk 5: Session persistence without WebSocket — user loses real-time status awareness**
→ Mitigation: On reconnect (navigating to Interview page), check for active session and show resume option. No need for WebSocket for MVP.

**Risk 6: JSONB analytics field may need restructuring if analytics requirements grow**
→ Mitigation: Keep it simple now — basic score + category breakdown fits in a single JSONB column. Restructuring later is straightforward migration.
