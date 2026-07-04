## Why

Developers need a platform to practice technical interviews across multiple topics and difficulty levels with AI-powered feedback. Current tools require separate subscriptions for question banks, coding evaluation, and personalized assessment. This project builds an integrated interview preparation app that generates questions dynamically via AI, evaluates answers in real-time, and tracks performance analytics over time — all using a single OpenAI-compatible endpoint.

## What Changes

- User authentication (register/login) with JWT stored in httpOnly cookies
- Interview session management: user selects topic, difficulty, question count, and programming language; questions are generated dynamically by AI at session start
- Dual question types: theory questions (text answers) and coding challenges (code submissions), both evaluated by AI after session completion
- Server-side session persistence with auto-termination via configurable timeout; resume option for interrupted sessions
- Post-interview results display showing overall score, per-question feedback, and solution details
- Performance analytics dashboard tracking basic score history and category breakdown over time
- Multi-language support: users select one language per session (Java, Python, JavaScript/Angular); AI evaluation uses language-specific deterministic prompts

## Capabilities

### New Capabilities

- `auth`: User registration and login with JWT-based authentication; httpOnly cookie storage for security
- `interview-session`: Interview setup, dynamic question generation, server-side session persistence, resume capability, and timeout-based auto-cleanup
- `question-management`: Dynamic AI-generated questions (theory + coding), language-specific deterministic evaluation prompts, and retry logic for AI failures with exponential backoff
- `evaluation`: Post-interview batch AI evaluation of answers and code submissions; deterministic scoring; partial results on failure
- `results-display`: End-of-session summary showing overall score, per-question feedback, solutions, and coding challenge evaluations
- `analytics`: Basic performance tracking — session history, average scores, category breakdown via bar charts

### Modified Capabilities

<!-- No existing capabilities to modify -->

## Impact

- **New project structure**: Angular 18+ frontend (with Material + ng2-charts) + Spring Boot 4 backend (Java 25)
- **Database**: PostgreSQL with pgvector extension for semantic question search capability
- **External dependency**: Single OpenAI-compatible API endpoint for both question generation and evaluation
- **Security**: httpOnly cookie-based JWT auth, CSRF protection for cross-origin requests
- **No breaking changes** — this is a greenfield project
