# Interview Prep Platform — Testing Guide

Complete step-by-step guide for testing both Backend API and Frontend.

---

## Prerequisites

- **Java 25** installed (`java -version` should show Java 25)
- **Maven 3.9+** installed (`mvn --version`)
- **PostgreSQL 15+** running (default port `5432`)
- **Node.js 18+** and **npm** installed
- **ng2-charts** peer dependency: `Chart.js`

---

## Part A — Backend Setup & Testing

### Step 1: Clone the project

```bash
cd /run/media/abhi/Work/DEV-WORK/AI-PROJECTS/interview-app
```

### Step 2: Configure PostgreSQL

Create a database for the app:

```sql
CREATE DATABASE interview_prep;
```

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/interview_prep
spring.datasource.username=your_pg_username
spring.datasource.password=your_pg_password
```

### Step 3: Install pgvector extension

```sql
-- Connect to your PostgreSQL and run:
CREATE EXTENSION IF NOT EXISTS vector;
```

Verify the extension is installed:

```sql
SELECT * FROM pg_extension WHERE extname = 'vector';
```

**Important:** If you get `ERROR: type "vector" does not exist` when starting the app, this means the extension wasn't properly created in your database. Run the above SQL again before restarting.

### Step 4: Configure AI API endpoint

In `application.properties`, set the OpenAI-compatible endpoint:

```properties
ai.gateway.endpoint=https://api.openai.com/v1/chat/completions
ai.gateway.api-key=your-api-key-here
ai.gateway.model=gpt-4o-mini
```

### Step 5: Build and run the backend

**Note:** The project uses Spring Boot **4.0.0 GA** (stable release), not the milestone version. No milestone repository is needed.

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

The API will start at `http://localhost:8080`.

Verify it's running:

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"} (if actuator is configured)
```

---

## Part B — Backend API Testing with curl

### 1. Authentication Endpoints

#### Register a new user

**Request:**
```bash
curl -v -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
```

**Expected Response (200 OK):**
```json
{
  "userId": 1,
  "email": "john@example.com"
}
```

Response includes a `Set-Cookie` header with the JWT token:
```
Set-Cookie: jwt_token=eyJhbGc...; Path=/; HttpOnly; Max-Age=3600; SameSite=Strict
```

**Duplicate email test:**
```bash
curl -v -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "error": "Email already in use"
}
```

#### Login

**Request:**
```bash
curl -v -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
```

**Expected Response (200 OK):**
```json
{
  "userId": 1,
  "email": "john@example.com"
}
```

Response includes a `Set-Cookie` header with the JWT token.

#### Logout

**Request:**
```bash
curl -v -X POST http://localhost:8080/auth/logout \
  -H "Content-Type: application/json"
```

**Expected Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

Response includes a `Set-Cookie` header that expires the JWT cookie.

---

### 2. Interview Session Endpoints

#### Start an interview

**Request:**
```bash
curl -v -X POST http://localhost:8080/interview/start \
  -H "Content-Type: application/json" \
  --cookie "jwt_token=eyJhbGc..." \
  -d '{
    "topicIds": ["4", "7"],
    "difficulty": "Medium",
    "count": 3,
    "languageId": "java"
  }'
```

**Expected Response (200 OK):**
```json
{
  "sessionId": 1,
  "questions": [
    {
      "id": 1,
      "type": "THEORY",
      "questionText": "...",
      "title": "...",
      "description": null,
      "codePrompt": null
    },
    {
      "id": 2,
      "type": "CODE",
      "questionText": "...",
      "title": "...",
      "description": "...",
      "codePrompt": "// public class Solution {\n//     ...\n// }"
    }
  ]
}
```

**Validation test — missing fields:**
```bash
curl -v -X POST http://localhost:8080/interview/start \
  -H "Content-Type: application/json" \
  --cookie "jwt_token=eyJhbGc..." \
  -d '{
    "topicIds": null,
    "difficulty": "Medium",
    "count": 3,
    "languageId": null
  }'
```

**Expected Response (400 Bad Request):** `null` body.

---

#### Check for resume option (active session)

**Request:**
```bash
curl -v http://localhost:8080/interview/resume \
  --cookie "jwt_token=eyJhbGc..."
```

**Expected Response if active session exists (200 OK):**
```json
{
  "sessionId": 1,
  "categoryName": "Java Core",
  "difficulty": "Medium",
  "score": null,
  "questionCount": 5,
  "answeredCount": 2
}
```

**Expected Response if no active session (204 No Content).**

---

#### Submit an answer during interview

**Request:**
```bash
curl -v -X POST http://localhost:8080/interview/{sessionId}/answer \
  -H "Content-Type: application/json" \
  --cookie "jwt_token=eyJhbGc..." \
  -d '{
    "questionId": 1,
    "answerText": "This is my answer text...",
    "languageSubmitted": null
  }'
```

**Expected Response (200 OK):** Empty body.

---

#### Finish the interview and get results

**Request:**
```bash
curl -v -X POST http://localhost:8080/interview/finish \
  -H "Content-Type: application/json" \
  --cookie "jwt_token=eyJhbGc..." \
  -d '{
    "sessionId": 1
  }'
```

**Expected Response (200 OK):**
```json
{
  "sessionId": 1,
  "status": "COMPLETED",
  "overallScore": 7.5,
  "evaluations": [
    {
      "evaluationId": 1,
      "questionType": "THEORY",
      "questionText": "...",
      "score": 8,
      "strengths": ["Good understanding of concepts"],
      "weaknesses": ["Could mention more details"],
      "improvedAnswer": "...",
      "isCorrect": null,
      "correctnessExplanation": null,
      "timeComplexity": null,
      "spaceComplexity": null,
      "status": "SUCCESS",
      "submittedCode": null,
      "userAnswerText": "This is my answer text..."
    },
    {
      "evaluationId": 2,
      "questionType": "CODE",
      "questionText": "...",
      "score": 7,
      "strengths": ["Correct algorithm"],
      "weaknesses": ["Could optimize space complexity"],
      "improvedAnswer": "// Improved solution...",
      "isCorrect": true,
      "correctnessExplanation": "The solution is correct and produces the expected output.",
      "timeComplexity": "O(n log n)",
      "spaceComplexity": "O(1)",
      "status": "SUCCESS",
      "submittedCode": "// user's code...",
      "userAnswerText": null
    }
  ]
}
```

---

#### Get results for a completed session (by ID)

**Request:**
```bash
curl -v http://localhost:8080/interview/results/1 \
  --cookie "jwt_token=eyJhbGc..."
```

**Expected Response:** Same format as the finish interview response.

---

### 3. Evaluation Retry Endpoint

#### Retry a failed evaluation

**Request:**
```bash
curl -v -X POST http://localhost:8080/evaluation/retry/{answerId} \
  --cookie "jwt_token=eyJhbGc..."
```

Where `{answerId}` is the ID of the answer with the failed evaluation.

**Expected Response (200 OK):** The newly evaluated result for that specific question, including score, strengths, weaknesses, and improved answer.

**Expected Response if no failed evaluation exists (400 Bad Request).**

---

### 4. Analytics Endpoint

#### Get performance data

**Request:**
```bash
curl -v http://localhost:8080/analytics/performance \
  --cookie "jwt_token=eyJhbGc..."
```

**Expected Response (200 OK):**
```json
{
  "totalSessions": 5,
  "avgScore": 7.3,
  "categoryBreakdown": {
    "System Design": 8.1,
    "Java": 6.9,
    "Spring Boot": 7.2
  },
  "sessions": [
    {
      "sessionId": 5,
      "endedAt": "2025-06-15T14:30:00",
      "categoryId": "System Design",
      "difficulty": "Hard",
      "score": 8
    },
    {
      "sessionId": 4,
      "endedAt": "2025-06-14T10:15:00",
      "categoryId": "Java",
      "difficulty": "Medium",
      "score": 6
    }
  ]
}
```

---

### 5. Session Detail & List Endpoints

#### Get full session details (with questions and answer status)

**Request:**
```bash
curl -v http://localhost:8080/interview/{sessionId}/detail \
  --cookie "jwt_token=eyJhbGc..."
```

**Expected Response (200 OK):** Session info with question list, answer statuses, and overall score.

#### Get answers for a session

**Request:**
```bash
curl -v http://localhost:8080/interview/{sessionId}/answers \
  --cookie "jwt_token=eyJhbGc..."
```

**Expected Response (200 OK):** List of submitted answers with their content and question references.

#### Get list of all user sessions

**Request:**
```bash
curl -v http://localhost:8080/interview/list \
  --cookie "jwt_token=eyJhbGc..."
```

**Expected Response (200 OK):** Array of session summaries (sessionId, status, difficulty, startedAt, etc.)

---

### 6. Language & Topics Endpoints

#### Get available programming languages

**Request:**
```bash
curl -v http://localhost:8080/languages \
  --cookie "jwt_token=eyJhbGc..."
```

**Expected Response (200 OK):** Array of `{id, name}` objects for each language.

#### Get topics for a specific language

**Request:**
```bash
curl -v http://localhost:8080/topics/{languageId} \
  --cookie "jwt_token=eyJhbGc..."
```

**Expected Response (200 OK):** Array of topic objects with id, name, and parentId.

#### Search topics by partial name

**Request:**
```bash
curl -v http://localhost:8080/topics/search/{languageId}/{query} \
  --cookie "jwt_token=eyJhbGc..."
```

Example:
```bash
curl -v http://localhost:8080/topics/search/java/str \
  --cookie "jwt_token=eyJhbGc..."
```

**Expected Response (200 OK):** Array of matching topics.

---

## Part C — Frontend Setup & Testing

### Step 1: Install frontend dependencies

```bash
cd frontend
npm install
```

If ng2-charts shows peer dependency errors, install Chart.js:

```bash
npm install chart.js --save-dev
```

### Step 2: Configure the backend URL

Open `frontend/src/app/services/interview-signal.service.ts` (or wherever the HTTP client is configured) and set your backend URL. If running locally, it should already point to `http://localhost:8080`.

If CORS issues occur, update `CorsConfig.java`:

```java
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:4200")); // Add your frontend URL
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowCredentials(true);
    config.setAllowedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
}
```

### Step 3: Start the frontend dev server

```bash
npm start
```

The app will open at `http://localhost:4200`.

---

## Part D — Frontend Testing Walkthrough

### Test Scenario 1: Full Interview Flow (Happy Path)

#### Step D.1: Register a user

1. Navigate to the **Register** page
2. Fill in:
   - Name: `Test User`
   - Email: `test@example.com`
   - Password: `TestPass123!`
   - Confirm Password: `TestPass123!`
3. Click **Register**

#### Step D.2: Start an interview

1. Navigate to the **Interview Setup** page (or auto-navigate after login)
2. Fill in:
   - Category/Topic: Select from dropdown
   - Difficulty: Select Easy/Medium/Hard
   - Number of Questions: Enter `3`
   - Programming Language: Select Java/Python/Angular
3. Click **Start Interview**

#### Step D.3: Answer questions

1. For each question displayed:
   - Read the question text/description
   - Type your answer in the textarea (theory) or write code + select language (coding)
   - Click **Next** to move to the next question
2. On the last question, click **Finish Interview**

#### Step D.4: View results

1. After a loading spinner ("Evaluating your answers..."), the Results page loads
2. Verify you see:
   - Overall score prominently displayed (e.g., `7.5/10`)
   - Per-question feedback with strengths, weaknesses, and improved answer
   - For coding questions: submitted code block, correctness assessment, time/space complexity

#### Step D.5: Start a new interview

1. Click **Start New Interview** on the results page
2. Verify you return to the setup screen

---

### Test Scenario 2: Session Resume

1. Start an interview but don't finish it (close browser tab or navigate away)
2. Open the app again in a new tab
3. Navigate to the Interview page
4. Verify you see: **"You have an ongoing interview"** with the category name and question count
5. Click **Resume Interview** — verify questions load from where you left off

---

### Test Scenario 3: Failed Evaluation Retry

1. If any evaluation returns status `FAILED` in the results page:
2. Verify a **Retry Evaluation** button appears on that card (red border)
3. Click **Retry Evaluation**
4. Verify the result updates with new feedback or a success message

---

### Test Scenario 4: Analytics View

1. Complete at least 2-3 interviews across different categories
2. Navigate to the **Analytics** page
3. Verify you see:
   - Total Sessions completed count
   - Average Score across all sessions
   - Bar chart showing category breakdown (each category as a bar with its average score)
   - Score history table sorted by date descending

---

## Part E — Quick Test Checklist

| # | Test | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 1 | Register with valid data | User created, JWT cookie set | |
| 2 | Login with valid credentials | User logged in, JWT cookie set | |
| 3 | Logout | Cookie cleared, message shown | |
| 4 | Duplicate email registration | Error: "Email already in use" | |
| 5 | Wrong password login | Error: "Invalid credentials" | |
| 6 | Start interview with valid params | Questions returned, session created | |
| 7 | Start interview with missing fields | 400 Bad Request | |
| 8 | Submit answer during interview | Answer saved successfully | |
| 9 | Finish interview after answering | Batch evaluation results returned | |
| 10 | Retry failed evaluation | Evaluation re-run, updated result shown | |
| 11 | Get analytics data | Performance data with breakdown and history | |
| 12 | Resume active session | Session found, resume option displayed | |
| 13 | No active session check | No resume card, setup screen shown | |
