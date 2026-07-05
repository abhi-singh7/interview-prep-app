# API Reference

## Base URL

```
http://localhost:8080
```

All endpoints return JSON responses unless otherwise specified. Authentication is required for most endpoints via JWT token in HttpOnly cookie (`jwt_token`).

---

## Authentication Endpoints

### POST /auth/register

Register a new user account and receive authentication credentials.

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "userId": 42,
  "email": "john@example.com",
  "name": "John Doe"
}
```

**Headers:**
- `Set-Cookie: jwt_token=<token>; HttpOnly; Path=/; Max-Age=28800; SameSite=Strict`

**Error Responses:**
- `400 Bad Request`: Email already exists or invalid input format

---

### POST /auth/login

Authenticate an existing user and receive authentication credentials.

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "userId": 42,
  "email": "john@example.com",
  "name": "John Doe"
}
```

**Headers:**
- `Set-Cookie: jwt_token=<token>; HttpOnly; Path=/; Max-Age=28800; SameSite=Strict`

**Error Responses:**
- `400 Bad Request`: Invalid credentials or email not found

---

### POST /auth/logout

Logout the current user by clearing the JWT cookie.

**Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

**Headers:**
- `Set-Cookie: jwt_token=; HttpOnly; Path=/; Max-Age=0; SameSite=Strict`

---

### GET /auth/status

Check if the current request is authenticated.

**Response (200 OK):**
```json
{
  "authenticated": true
}
```

**Response (401 Unauthorized):**
```json
{
  "authenticated": false
}
```

---

## Interview Management Endpoints

### POST /interview/start

Start a new interview session with specified parameters.

**Request Body:**
```json
{
  "topicIds": ["11", "12", "13"],
  "difficulty": "medium",
  "count": 5,
  "languageId": "10"
}
```

**Parameters:**
- `topicIds` (optional): Array of topic IDs to include. If empty/null, auto-selects all topics for the language.
- `difficulty`: Required. Difficulty level: `"easy"`, `"medium"`, or `"hard"`.
- `count`: Required. Number of questions to generate (1-50).
- `languageId` (optional): Language ID for the interview. If null, auto-selects based on topics.

**Response (200 OK):**
```json
{
  "sessionId": 101,
  "questions": [
    {
      "id": 501,
      "type": "CODE",
      "categoryId": "11",
      "questionText": "Implement a binary search algorithm...",
      "title": "Binary Search Implementation",
      "description": "Write a function that performs binary search on a sorted array.",
      "codePrompt": "def binary_search(arr, target):\n    # Your code here\n    pass"
    },
    {
      "id": 502,
      "type": "THEORY",
      "categoryId": "12",
      "questionText": "Explain the difference between == and .equals() in Java.",
      "title": null,
      "description": null,
      "codePrompt": null
    }
  ]
}
```

**Error Responses:**
- `400 Bad Request`: Invalid parameters (difficulty missing, count out of range)

---

### GET /interview/resume

Resume an active interview session if one exists.

**Response (200 OK):**
```json
{
  "sessionId": 101,
  "categoryId": "10",
  "difficulty": "medium",
  "answeredQuestions": 3,
  "startedAt": "2024-01-15T10:30:00",
  "timeoutHours": 2
}
```

**Response (204 No Content):**
No active session found.

---

### POST /interview/{sessionId}/answer

Submit an answer for a question within an interview session.

**Path Parameters:**
- `sessionId`: The ID of the active interview session.

**Request Body:**
```json
{
  "questionId": 501,
  "answerText": "Here's my implementation:\n\ndef binary_search(arr, target):\n    left, right = 0, len(arr) - 1\n    while left <= right:\n        mid = (left + right) // 2\n        if arr[mid] == target:\n            return mid\n        elif arr[mid] < target:\n            left = mid + 1\n        else:\n            right = mid - 1\n    return -1",
  "languageSubmitted": "python"
}
```

**Parameters:**
- `questionId`: The ID of the question being answered.
- `answerText`: Required. The user's answer text (10-50,000 characters).
- `languageSubmitted` (optional): Programming language used (e.g., "java", "python"). Required for CODE questions.

**Response (200 OK):** Empty response body with 200 status code.

**Error Responses:**
- `400 Bad Request`: Session not found, session not active, question not found, or missing language for CODE question.

---

### POST /interview/finish

Finish an interview session and trigger evaluation of all answers.

**Request Body:**
```json
{
  "sessionId": 101
}
```

**Response (200 OK):** Empty response body with 200 status code.

**Error Responses:**
- `400 Bad Request`: Session not found or not owned by current user.

---

### GET /interview/session/{sessionId}/detail

Get detailed information about an interview session including all questions, answers, and evaluations.

**Path Parameters:**
- `sessionId`: The ID of the interview session.

**Response (200 OK):**
```json
{
  "sessionId": 101,
  "status": "COMPLETED",
  "languageId": "10",
  "languageName": "Java",
  "topicNames": ["Core Java", "Generics"],
  "categoryId": "10",
  "difficulty": "medium",
  "startedAt": "2024-01-15T10:30:00",
  "endedAt": "2024-01-15T11:30:00",
  "overallScore": 85.5,
  "totalEarned": 171,
  "questions": [
    {
      "questionId": 501,
      "type": "CODE",
      "title": "Binary Search Implementation",
      "questionText": "Implement a binary search algorithm...",
      "description": "Write a function that performs binary search on a sorted array.",
      "codePrompt": "def binary_search(arr, target):\n    # Your code here\n    pass",
      "answerStatus": "ANSWERED",
      "answer": {
        "answerId": 301,
        "answerText": "Here's my implementation...",
        "languageSubmitted": "python",
        "evaluation": {
          "evaluationId": 201,
          "score": 85,
          "status": "SUCCESS",
          "strengths": ["Correct algorithm choice", "Good edge case handling"],
          "weaknesses": ["Could optimize space complexity", "Missing input validation"],
          "improvedAnswer": "Here's an optimized version...",
          "isCorrect": true,
          "correctnessExplanation": "The implementation correctly handles all cases.",
          "timeComplexity": "O(log n)",
          "spaceComplexity": "O(1)"
        }
      }
    },
    {
      "questionId": 502,
      "type": "THEORY",
      "title": null,
      "questionText": "Explain the difference between == and .equals() in Java.",
      "description": null,
      "codePrompt": null,
      "answerStatus": "NOT_ANSWERED",
      "answer": null
    }
  ]
}
```

**Error Responses:**
- `404 Not Found`: Session not found or session is ABANDONED.
- `403 Forbidden`: Session does not belong to current user.

---

### GET /interview/session/{sessionId}/questions

Get all questions for a specific interview session.

**Path Parameters:**
- `sessionId`: The ID of the interview session.

**Response (200 OK):** Array of question objects (same structure as in `/interview/start` response).

**Response (204 No Content):** No questions found for the session.

---

## Evaluation Endpoints

### POST /evaluation/retry/{answerId}

Retry a failed evaluation for a specific answer.

**Path Parameters:**
- `answerId`: The ID of the answer whose evaluation should be retried.

**Response (200 OK):**
```json
{
  "id": 201,
  "answer": {
    "id": 301,
    "session": {"id": 101},
    "question": {"id": 501},
    "answerText": "...",
    "languageSubmitted": "python"
  },
  "score": 85,
  "strengthsJson": "[\"Correct algorithm choice\", \"Good edge case handling\"]",
  "weaknessesJson": "[\"Could optimize space complexity\", \"Missing input validation\"]",
  "improvedAnswer": "Here's an optimized version...",
  "isCorrect": true,
  "correctnessExplanation": "The implementation correctly handles all cases.",
  "timeComplexity": "O(log n)",
  "spaceComplexity": "O(1)",
  "status": "SUCCESS"
}
```

**Error Responses:**
- `400 Bad Request`: No failed evaluation found for the answer.
- `403 Forbidden`: Answer does not belong to current user's session.
- `500 Internal Server Error`: AI evaluation service error.

---

## Analytics Endpoints

### GET /performance

Get aggregated performance data for the current user.

**Response (200 OK):**
```json
{
  "totalSessions": 15,
  "avgScore": 78.5,
  "categoryBreakdown": {
    "10": 82.3,
    "20": 75.1,
    "30": 68.9
  },
  "sessions": [
    {
      "sessionId": 101,
      "endedAt": "2024-01-15T11:30:00",
      "categoryName": "Java",
      "difficulty": "medium",
      "score": 85,
      "questionCount": 5,
      "answeredCount": 5
    },
    {
      "sessionId": 98,
      "endedAt": "2024-01-14T16:45:00",
      "categoryName": "Python",
      "difficulty": "easy",
      "score": 72,
      "questionCount": 3,
      "answeredCount": 3
    }
  ]
}
```

**Parameters:**
- `categoryBreakdown`: Map of category ID to average score.
- `sessions`: Array of session summaries sorted by most recent first.

---

## Language & Topic Endpoints

### GET /interview/languages

Get all available programming languages for interviews.

**Response (200 OK):**
```json
[
  {
    "id": "10",
    "name": "Java"
  },
  {
    "id": "20",
    "name": "Python"
  },
  {
    "id": "30",
    "name": "SQL"
  }
]
```

---

### GET /interview/topics

Get topics for a specific language, optionally filtered by search text.

**Query Parameters:**
- `langId` (required): The ID of the language.
- `search` (optional): Search text to filter topics by name.

**Response (200 OK):**
```json
[
  {
    "id": "11",
    "name": "Core Java"
  },
  {
    "id": "12",
    "name": "Generics"
  },
  {
    "id": "13",
    "name": "Collections"
  }
]
```

**Error Responses:**
- `400 Bad Request`: Search text exceeds maximum length (200 characters).

---

## Error Response Format

All error responses follow a consistent format:

```json
{
  "error": "Error message describing what went wrong"
}
```

Common HTTP status codes:
- `400 Bad Request`: Invalid input parameters or business logic validation failure.
- `401 Unauthorized`: Missing or invalid JWT token.
- `403 Forbidden`: User does not have permission to access the resource.
- `404 Not Found`: Resource does not exist.
- `500 Internal Server Error`: Unexpected server error.

---

## Authentication Requirements

All endpoints except `/auth/*` require a valid JWT token in the `jwt_token` cookie. The token is automatically validated by the `JwtAuthenticationFilter` on each request.

**Token Structure:**
```json
{
  "sub": "user@example.com",
  "userId": 42,
  "iat": 1705312800,
  "exp": 1705341600
}
```

**Token Validation:**
- Signature verification using HMAC-SHA256 with configured secret.
- Expiration check (default: 8 hours).
- Subject claim must be a valid email format.

---

## Rate Limiting

Currently, no explicit rate limiting is implemented. Consider adding rate limiting in production to prevent abuse:

```properties
# Example with Spring Boot Actuator and custom filter
spring.boot.admin.client.url=http://localhost:8081
management.endpoints.web.exposure.include=health,info,metrics
```

---

## CORS Configuration

The application is configured to allow cross-origin requests from the frontend. Configure allowed origins in `CorsConfig.java`:

```java
corsRegistry.addMapping("/**")
    .allowedOrigins("http://localhost:4200")  // Frontend URL
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    .allowedHeaders("*")
    .allowCredentials(true);
```

---

## Testing Endpoints

Use tools like Postman, curl, or Insomnia to test the API:

**Example with curl:**

```bash
# Register a new user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","password":"securePassword123"}' \
  -c cookies.txt

# Login (replaces cookie)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"securePassword123"}' \
  -c cookies.txt

# Start an interview session
curl -X POST http://localhost:8080/interview/start \
  -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"topicIds":["11","12"],"difficulty":"medium","count":3,"languageId":"10"}'

# Submit an answer
curl -X POST http://localhost:8080/interview/101/answer \
  -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"questionId":501,"answerText":"def binary_search(arr, target):\n    left, right = 0, len(arr) - 1\n    while left <= right:\n        mid = (left + right) // 2\n        if arr[mid] == target:\n            return mid\n        elif arr[mid] < target:\n            left = mid + 1\n        else:\n            right = mid - 1\n    return -1","languageSubmitted":"python"}'

# Finish the interview
curl -X POST http://localhost:8080/interview/finish \
  -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"sessionId":101}'

# Get session details
curl -X GET http://localhost:8080/interview/session/101/detail \
  -b cookies.txt

# Get performance analytics
curl -X GET http://localhost:8080/performance \
  -b cookies.txt
```
