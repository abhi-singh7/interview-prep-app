## Why

After login, users are redirected to `/interview` but see a blank page — nothing loads or renders. The root cause is that the interview setup component is never rendered (no child routes defined), evaluations from AI responses are created in-memory but never persisted to the database, and several other bugs compound the problem making the core interview flow unusable.

## What Changes

- Add child routes under `/interview` so the setup page renders when no active session exists
- Fix evaluation persistence — save Evaluation entities to the database during `finishInterview()` 
- Implement question loading for the resume scenario (currently a stub)
- Add missing `/auth/status` endpoint for auth verification after browser restarts
- Remove duplicate authentication filter in SecurityConfig that can overwrite user context
- Fix analytics native queries that fail when sessions contain no CODE questions
- Wire up coding question component to emit submit events properly

## Capabilities

### New Capabilities

- `interview-setup`: Interview setup and display flow — renders the setup page, loads questions from AI backend, supports resume of active sessions
- `evaluation-persistence`: Evaluation entity persistence — evaluations created by AI during interview finish are saved to the database so results persist across navigation

## Impact

**Frontend:**
- `frontend/src/app/app.routes.ts` — add child routes under `/interview`
- `frontend/src/app/interview/interview-page/interview-page.component.ts` — implement question loading, fix resume flow
- `frontend/src/app/auth/register/register.component.ts` — add password match validation return value
- `frontend/src/app/interview/components/coding-question.component.ts` — add submit event output

**Backend:**
- `backend/src/main/java/com/interviewprep/interview/InterviewController.java` — persist evaluations during finish, fix session query for resume
- `backend/src/main/java/com/interviewprep/auth/AuthController.java` — add `/status` endpoint
- `backend/src/main/java/com/interviewprep/config/SecurityConfig.java` — remove duplicate filter

**Database:**
- No schema changes needed (Evaluations already have the right entity model, just not being saved)
