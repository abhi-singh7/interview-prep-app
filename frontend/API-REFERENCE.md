# API Reference — AI Interview Prep Frontend

## Overview

This document lists all HTTP endpoints called by the frontend, their request/response formats, and which service handles each call. The backend is a Spring Boot application that serves both REST APIs and static files in production.

---

## Authentication Endpoints

### POST `/api/auth/register`

Register a new user account.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | User's display name |
| `email` | string | Yes | User's email address |
| `password` | string | Yes | User's password (min length enforced by backend) |

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securePassword123"
}
```

**Response (Success — 200):** Sets HttpOnly JWT cookie (`jwt_token`) and sets `isAuthenticated` flag in sessionStorage.

**Response (Error — 400/409):** Validation error or duplicate email.

---

### POST `/api/auth/login`

Authenticate a user with credentials.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | string | Yes | User's email address |
| `password` | string | Yes | User's password |

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "securePassword123"
}
```

**Response (Success — 200):** Sets HttpOnly JWT cookie (`jwt_token`) and sets `isAuthenticated` flag in sessionStorage.

**Response (Error — 401):** Invalid credentials.

---

### POST `/api/auth/logout`

Log out the current user. Clears the session.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| *(none)* | — | — | No body required |

**Request Body:** `{}`

**Response (Success — 200):** Session cleared, JWT cookie invalidated.

---

### GET `/api/auth/status`

Verify the current authentication status by checking if the HttpOnly JWT cookie is valid.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| *(none)* | — | — | No parameters required |

**Response (Success — 200):** `{"authenticated": true}` or similar status indicator.

**Response (Error — 401):** Invalid/expired JWT cookie.

---

## Interview Endpoints

### POST `/api/interview/start`

Start a new interview session with the given configuration. The backend generates AI questions based on the parameters.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `topicIds` | string[] | Yes | Array of topic IDs to include in the interview |
| `difficulty` | string | Yes | Difficulty level (e.g., "EASY", "MEDIUM", "HARD") |
| `count` | number | Yes | Number of questions to generate |
| `languageId` | string | Yes | Programming language ID for coding questions |

**Request Body:**
```json
{
  "topicIds": ["1", "2"],
  "difficulty": "MEDIUM",
  "count": 5,
  "languageId": "java"
}
```

**Response (Success — 200):** Returns the generated interview session with questions.

---

### GET `/api/interview/resume`

Resume a previously interrupted interview session.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| *(none)* | — | — | No parameters required (session ID from sessionStorage) |

**Response (Success — 200):** Returns the existing session with questions and any already-submitted answers.

---

### GET `/api/interview/session/{sessionId}/questions`

Get all questions for a specific interview session.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sessionId` | number (path param) | Yes | The interview session ID |

**Response (Success — 200):** Array of question objects:
```json
[
  {
    "id": 1,
    "type": "THEORY",
    "title": "Explain polymorphism",
    "description": "What is polymorphism in OOP?",
    "codePrompt": null
  },
  {
    "id": 2,
    "type": "CODE",
    "title": "Two Sum",
    "description": "Find two numbers that add up to a target.",
    "codePrompt": "public int[] twoSum(int[] nums, int target) {\n    // Your code here\n}"
  }
]
```

---

### POST `/api/interview/{sessionId}/answer`

Submit an answer for a question in the current session.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sessionId` | number (path param) | Yes | The interview session ID |
| `questionId` | number | Yes | The question being answered |
| `answerText` | string | Yes | The answer text (or code for coding questions) |
| `languageSubmitted` | string | No | Programming language used for the submission (for coding questions) |

**Request Body:**
```json
{
  "questionId": 2,
  "answerText": "public int[] twoSum(int[] nums, int target) {\n    Map<Integer, Integer> map = new HashMap<>();\n    for (int i = 0; i < nums.length; i++) {\n        int complement = target - nums[i];\n        if (map.containsKey(complement)) return new int[]{map.get(complement), i};\n        map.put(nums[i], i);\n    }\n    return null;\n}",
  "languageSubmitted": "java"
}
```

**Response (Success — 200):** Confirmation of submission.

---

### POST `/api/interview/finish`

Finish the interview session and trigger evaluation.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sessionId` | number | Yes | The interview session ID to finish |

**Request Body:**
```json
{
  "sessionId": 42
}
```

**Response (Success — 200):** Returns evaluation results.

---

### GET `/api/interview/results/{sessionId}`

Get the evaluation results for a completed interview session.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sessionId` | number (path param) | Yes | The interview session ID |

**Response (Success — 200):** Evaluation result object:
```json
{
  "sessionId": 42,
  "overallScore": 85.5,
  "answers": [
    {
      "questionId": 1,
      "score": 90,
      "feedback": "Good explanation of polymorphism",
      "isCorrect": true
    },
    {
      "questionId": 2,
      "score": 80,
      "feedback": "Correct solution but could optimize space complexity",
      "isCorrect": true
    }
  ]
}
```

---

### GET `/api/interview/session/list`

Get a list of all interview sessions for the current user.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| *(none)* | — | — | No parameters required |

**Response (Success — 200):** Array of session objects:
```json
[
  {
    "sessionId": 42,
    "date": "2026-07-01T10:30:00",
    "topicIds": ["1", "2"],
    "difficulty": "MEDIUM",
    "languageId": "java",
    "status": "COMPLETED"
  }
]
```

---

## Dropdown Data Endpoints

### GET `/api/interview/languages`

Get all available programming languages for interview questions.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| *(none)* | — | — | No parameters required |

**Response (Success — 200):** Array of language options:
```json
[
  { "id": "java", "name": "Java" },
  { "id": "python", "name": "Python" },
  { "id": "typescript", "name": "TypeScript" }
]
```

---

### GET `/api/interview/topics`

Get topics for a specific language. Supports filtering by search text.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `langId` | string (query param) | Yes | Language ID to filter topics by |
| `search` | string (query param) | No | Search text for topic filtering |

**Request:** `GET /api/interview/topics?langId=java&search=array`

**Response (Success — 200):** Array of topic options:
```json
[
  { "id": "1", "name": "Arrays" },
  { "id": "5", "name": "Array Rotation" }
]
```

---

## Evaluation Endpoints

### POST `/api/evaluation/retry/{answerId}`

Retry the evaluation for a specific answer. Used when the user wants to re-evaluate their response after reviewing feedback.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `answerId` | number (path param) | Yes | The answer ID to retry evaluation for |

**Request Body:** `{}`

**Response (Success — 200):** Updated evaluation result.

---

## Analytics Endpoints

### GET `/api/analytics/performance`

Get performance analytics data for the current user across all sessions.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| *(none)* | — | — | No parameters required |

**Response (Success — 200):** Performance metrics:
```json
{
  "totalSessions": 15,
  "averageScore": 78.3,
  "scoreTrend": [65, 70, 72, 75, 78, 80, 82],
  "topicPerformance": {
    "Arrays": 85,
    "Linked Lists": 72,
    "Trees": 68
  },
  "difficultyBreakdown": {
    "EASY": 90,
    "MEDIUM": 75,
    "HARD": 60
  }
}
```

---

## Request/Response Flow Summary

### Authentication Flow

```
1. POST /api/auth/login → Sets HttpOnly JWT cookie + sessionStorage flag
2. Subsequent requests → Browser auto-sends JWT cookie with every request
3. GET /api/auth/status → Verifies JWT is still valid (used by AuthService)
4. POST /api/auth/logout → Invalidates session, clears cookie
```

### Interview Flow

```
1. POST /api/interview/start → Generate questions for new interview
2. GET /api/interview/session/{id}/questions → Fetch questions to display
3. POST /api/interview/{sessionId}/answer → Submit each answer as user types
4. POST /api/interview/finish → Finish session, trigger evaluation
5. GET /api/interview/results/{sessionId} → Display results
```

### Analytics Flow

```
1. GET /api/analytics/performance → Fetch performance data for charts
2. GET /api/interview/session/list → Fetch session history for analytics context
```

---

## Error Handling

All endpoints return standard HTTP status codes:

| Status Code | Meaning | Typical Cause |
|-------------|---------|---------------|
| 200 | Success | Request processed successfully |
| 400 | Bad Request | Invalid input data, validation error |
| 401 | Unauthorized | Missing or invalid JWT cookie |
| 403 | Forbidden | CSRF token mismatch (handled by interceptor) |
| 500 | Internal Server Error | Backend processing error |

The frontend's `CsrfTokenInterceptor` automatically handles 403 responses by re-reading the CSRF cookie and retrying. In production, this should include an automatic request retry mechanism.

---

## CORS Configuration

During development, the Angular dev server proxies `/api/*` requests to the backend via `proxy.conf.json`. This avoids CORS issues entirely since both the frontend and API share the same origin during development.

In production, the Spring Boot backend serves static files directly from `src/main/resources/static/browser/`, so there are no CORS concerns — everything runs on the same server.
