# Database Schema Documentation

## Overview

The AI Interview Prep Platform uses PostgreSQL with pgvector extension for semantic search capabilities. The database schema is managed through Flyway migrations and includes tables for users, categories (languages/topics), interview sessions, questions, answers, evaluations, and user performance metrics.

---

## Entity Relationship Diagram

```
┌─────────────┐       ┌──────────────────┐       ┌─────────────┐
│   users     │       │interview_sessions│       │ categories  │
├─────────────┤       ├──────────────────┤       ├─────────────┤
│ id (PK)     │◀──────│ user_id (FK)     │       │ id (PK)     │
│ email       │       │ category_id (FK) │       │ name        │
│ password    │       │ topic_ids_json   │       │ type        │
│ name        │       │ difficulty       │       │parent_id(FK)│
│ created_at  │       │ language_id      │       └──────┬──────┘
│ updated_at  │       │ status           │              │
└─────────────┘       │ started_at       │     ┌────────▼────────┐
                      │ ended_at         │     │ categories      │
                      │ timeout_hours    │     │ (self-reference)│
                      └────────┬─────────┘     └─────────────────┘
                               │
                               ▼
┌─────────────┐       ┌──────────────────┐       ┌─────────────┐
│  questions  │       │    answers        │      │evaluations  │
├─────────────┤       ├──────────────────┤       ├─────────────┤
│ id (PK)     │◀──────│ session_id (FK)  │       │ id (PK)     │
│ session_id  │       │ question_id (FK) │       │answer_id(FK)│
│ type        │       │ answer_text      │       │ score       │
│ category_id │       │language_submitted│      │strengths_json│
│question_text│       └────────┬─────────┘       │ weaknesses_json│
│ title       │                │                 │ improved_answer│
│ description │                ▼                 │ is_correct    │
│ code_prompt │       ┌──────────────┐          │ correctness_explanation│
│ embedding   │       │ evaluations  │          │ time_complexity     │
│ created_at  │◀──────│ answer_id(FK)│          │ space_complexity    │
└─────────────┘       │ score        │          │ status              │
                      │ strengths_json│         └─────────────────────┘
                      │ weaknesses_json│
                      │ improved_answer│
                      └────────────────┘

┌──────────────────┐
│ user_performance │
├──────────────────┤
│ id (PK)          │
│ user_id (FK)     │
│ total_sessions   │
│ avg_score        │
│ category_breakdown_json│
│ updated_at       │
└──────────────────┘
```

---

## Table Definitions

### users

Stores user account information and authentication credentials.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique user identifier |
| email | VARCHAR(255) | UNIQUE, NOT NULL | User's email address (used as JWT subject) |
| password | VARCHAR(255) | NOT NULL | BCrypt-hashed password |
| name | VARCHAR(100) | NOT NULL | User's display name |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Account creation timestamp |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last update timestamp |

**Indexes:**
- `idx_users_email` on `email` (UNIQUE)

---

### categories

Hierarchical category structure for languages and topics. Supports parent-child relationships through self-referencing foreign key.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY | Unique category identifier |
| name | VARCHAR(100) | NOT NULL, UNIQUE | Category display name (e.g., "Java", "Core Java") |
| type | ENUM('LANGUAGE', 'TOPIC') | NOT NULL | Category classification: LANGUAGE for programming languages, TOPIC for subcategories |
| parent_id | BIGINT | FOREIGN KEY → categories.id, ON DELETE SET NULL | Parent category ID (NULL for top-level languages) |

**Indexes:**
- `idx_categories_type` on `type`
- `idx_categories_parent_id` on `parent_id`
- `idx_categories_name` on `name`

**Constraints:**
- `chk_categories_type`: Ensures type is either 'LANGUAGE' or 'TOPIC'
- `fk_categories_parent`: Self-referencing foreign key for hierarchy

**Example Data:**
```sql
-- Languages (parent_id = NULL)
(10, 'Java', 'LANGUAGE', NULL)
(20, 'Python', 'LANGUAGE', NULL)
(30, 'SQL', 'LANGUAGE', NULL)

-- Topics (parent_id references language)
(11, 'Core Java', 'TOPIC', 10)
(12, 'Generics', 'TOPIC', 10)
(21, 'Core Python', 'TOPIC', 20)
```

---

### interview_sessions

Tracks individual interview sessions with metadata and status.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique session identifier |
| user_id | BIGINT | FOREIGN KEY → users.id, NOT NULL | User who owns the session |
| category_id | VARCHAR(50) | Foreign key to categories.id | Selected language/category ID |
| topic_ids_json | TEXT | JSON string of topic IDs | Serialized list of selected topic IDs |
| difficulty | ENUM('easy', 'medium', 'hard') | NOT NULL | Interview difficulty level |
| language_id | VARCHAR(50) | Foreign key to categories.id | Programming language for the session |
| status | ENUM('ACTIVE', 'COMPLETED', 'ABANDONED') | DEFAULT 'ACTIVE' | Session lifecycle state |
| started_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Session start time |
| ended_at | TIMESTAMP | Nullable, updated on completion | Session end time |
| timeout_hours | INT | DEFAULT 2 | Hours before session auto-abandons |

**Indexes:**
- `idx_sessions_user_id` on `user_id`
- `idx_sessions_status` on `status`
- `idx_sessions_started_at` on `started_at`

**Session Lifecycle:**
1. **ACTIVE**: Session is in progress, user can submit answers
2. **COMPLETED**: User finished the session or timeout expired
3. **ABANDONED**: Session exceeded timeout without completion (marked by background cleanup task)

---

### questions

Stores generated interview questions with vector embeddings for semantic search.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique question identifier |
| session_id | BIGINT | FOREIGN KEY → interview_sessions.id | Session this question belongs to |
| type | ENUM('THEORY', 'CODE') | NOT NULL | Question classification |
| category_id | VARCHAR(50) | Foreign key to categories.id | Topic/category ID |
| question_text | TEXT | NOT NULL | Full question text |
| title | VARCHAR(200) | Nullable | Short title (for CODE questions) |
| description | TEXT | Nullable | Detailed problem description (CODE only) |
| code_prompt | TEXT | Nullable | Starter code with method signature (CODE only) |
| embedding | vector(1536) | pgvector column | Vector embedding for semantic search |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Question generation timestamp |

**Indexes:**
- `idx_questions_session_id` on `session_id`
- `idx_questions_type` on `type`
- `idx_questions_category_id` on `category_id`
- `idx_questions_embedding` using ivfflat (for vector similarity search)

**Vector Embedding Configuration:**
```sql
-- Create index for fast vector similarity search
CREATE INDEX idx_questions_embedding ON questions 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

---

### answers

Records user responses to interview questions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique answer identifier |
| session_id | BIGINT | FOREIGN KEY → interview_sessions.id, NOT NULL | Session this answer belongs to |
| question_id | BIGINT | FOREIGN KEY → questions.id, NOT NULL | Question being answered |
| answer_text | TEXT | NOT NULL | User's submitted answer text |
| language_submitted | VARCHAR(50) | Nullable | Programming language used (required for CODE questions) |

**Indexes:**
- `idx_answers_session_id` on `session_id`
- `idx_answers_question_id` on `question_id`
- `idx_answers_user_session` on `(user_id, session_id)` (via JOIN with sessions table)

---

### evaluations

AI-generated evaluations of user answers with scoring and feedback.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique evaluation identifier |
| answer_id | BIGINT | FOREIGN KEY → answers.id, NOT NULL | Answer being evaluated |
| score | INT | CHECK (score BETWEEN 0 AND 100) | Numeric score (0-100 scale) |
| strengths_json | TEXT | JSON string | Array of positive aspects identified by AI |
| weaknesses_json | TEXT | JSON string | Array of areas for improvement identified by AI |
| improved_answer | TEXT | Nullable | AI-generated improved version of the answer |
| is_correct | BOOLEAN | Nullable | Whether the solution is algorithmically correct (null for THEORY) |
| correctness_explanation | TEXT | Nullable | Explanation for correctness assessment |
| time_complexity | VARCHAR(50) | Nullable | Big-O time complexity notation (e.g., "O(n log n)") |
| space_complexity | VARCHAR(50) | Nullable | Big-O space complexity notation (e.g., "O(1)") |
| status | ENUM('SUCCESS', 'FAILED') | DEFAULT 'FAILED' | Evaluation generation status |

**Indexes:**
- `idx_evaluations_answer_id` on `answer_id`
- `idx_evaluations_status` on `status`
- `idx_evaluations_score` on `score` (for ranking)

**JSONB Fields Structure:**
```json
{
  "strengths_json": "[\"Correct algorithm choice\", \"Good edge case handling\"]",
  "weaknesses_json": "[\"Could optimize space complexity\", \"Missing input validation\"]"
}
```

---

### user_performance

Aggregated performance metrics for users across all interview sessions. Updated by analytics service.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique performance record identifier |
| user_id | BIGINT | FOREIGN KEY → users.id, UNIQUE | User whose metrics are stored |
| total_sessions | INT | DEFAULT 0 | Total number of completed sessions |
| avg_score | DECIMAL(5,2) | Nullable | Average score across all sessions (0-100) |
| category_breakdown_json | TEXT | JSON string | Per-category average scores |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last update timestamp |

**Indexes:**
- `idx_user_performance_user_id` on `user_id` (UNIQUE)

**JSONB Structure:**
```json
{
  "category_breakdown_json": {
    "10": 82.3,
    "20": 75.1,
    "30": 68.9
  }
}
```

---

## Database Migrations

### V1__create_pgvector_extension.sql

Enables pgvector extension for vector similarity search:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### V2__add_topic_hierarchy.sql

Adds type and parent_id columns to categories table for hierarchical structure:
```sql
ALTER TABLE categories ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'TOPIC';
ALTER TABLE categories ALTER COLUMN type DROP DEFAULT;
ALTER TABLE categories ADD CONSTRAINT chk_categories_type CHECK (type IN ('LANGUAGE', 'TOPIC'));

ALTER TABLE categories ADD COLUMN parent_id BIGINT;
ALTER TABLE categories ADD CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id);

CREATE INDEX idx_categories_parent_id ON categories(parent_id);
```

### V3__seed_topics.sql

Seeds initial language and topic data:
```sql
-- Insert Languages (type = LANGUAGE, parent_id = NULL)
INSERT INTO categories (id, name, type, parent_id) VALUES
(10, 'Java', 'LANGUAGE', NULL),
(20, 'Python', 'LANGUAGE', NULL),
(30, 'SQL', 'LANGUAGE', NULL);

-- Insert Topics for Java (type = TOPIC, parent_id = 10)
INSERT INTO categories (id, name, type, parent_id) VALUES
(11, 'Core Java', 'TOPIC', 10),
(12, 'Generics', 'TOPIC', 10),
(13, 'Collections', 'TOPIC', 10);
```

### V4__fix_question_session_fk.sql

Backfills question.session_id from answer chain (idempotent migration):
```sql
UPDATE questions q
SET session_id = a.session_id
FROM answers a
WHERE a.question_id = q.id;
```

---

## Vector Search Queries

### Semantic Similarity Search

Find questions similar to a given text:

```sql
-- Generate embedding for query text (using nomic-embed-text model)
WITH query_embedding AS (
  SELECT embedding_service.embed('binary search algorithm') as vec
)
SELECT 
  q.id,
  q.question_text,
  q.type,
  q.category_id,
  1 - (q.embedding <=> qe.vec) as similarity
FROM questions q, query_embedding qe
ORDER BY q.embedding <=> qe.vec
LIMIT 10;
```

### Category-Based Question Retrieval

Get all questions for a specific category:

```sql
SELECT 
  q.id,
  q.question_text,
  q.type,
  c.name as category_name
FROM questions q
JOIN categories c ON q.category_id = c.id
WHERE c.parent_id = 10  -- Java topics
ORDER BY q.created_at DESC;
```

---

## Performance Considerations

### Indexing Strategy

1. **Foreign Key Columns**: Indexed for JOIN performance
2. **Status/Type Columns**: Indexed for filtering queries
3. **Vector Column**: Uses ivfflat index with cosine distance for similarity search
4. **Timestamp Columns**: Indexed for time-range queries and sorting

### Query Optimization

- Use `EXPLAIN ANALYZE` to identify slow queries
- Monitor pgvector index performance with `pg_stat_user_indexes`
- Consider partitioning large tables (questions, answers) by session_id or created_at

### Connection Pooling

Configure HikariCP connection pool in application.properties:

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

---

## Backup and Recovery

### Database Backup

```bash
# Full database backup
pg_dump -U postgres -d interview_prep > backup_$(date +%Y%m%d).sql

# Compressed backup
pg_dump -U postgres -d interview_prep | gzip > backup_$(date +%Y%m%d).sql.gz

# Schema-only backup
pg_dump -U postgres -d interview_prep --schema-only > schema_backup.sql
```

### Database Restore

```bash
# Restore from full backup
psql -U postgres -d interview_prep < backup_YYYYMMDD.sql

# Restore from compressed backup
gunzip -c backup_YYYYMMDD.sql.gz | psql -U postgres -d interview_prep
```

### Migration Rollback

Flyway migrations are forward-only. To rollback:

1. Delete the migration file or mark it as undone in `flyway_schema_history`
2. Run `DROP TABLE IF EXISTS <table_name>;` for each table created by that migration
3. Re-run application to recreate schema

---

## Monitoring

### Database Statistics

```sql
-- Table sizes
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Index usage statistics
SELECT 
  indexrelname as index_name,
  idx_scan as scans,
  idx_tup_read as tuples_read,
  idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Active queries
SELECT 
  pid,
  usename,
  application_name,
  state,
  query_start,
  now() - query_start as duration,
  query
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY query_start;
```

### Performance Metrics

Monitor these key metrics:
- **Query latency**: Average and p95/p99 response times
- **Connection pool utilization**: Active vs idle connections
- **Vector search performance**: Similarity search query time
- **Table bloat**: Dead tuples and table size growth
- **Index efficiency**: Index scan ratio vs sequential scans
