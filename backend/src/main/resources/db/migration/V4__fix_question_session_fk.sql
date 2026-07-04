-- Backfill questions.session_id from the answer chain.
-- Questions were persisted without a session reference (Bug in QuestionGenerationService),
-- leaving every Question row with session_id = NULL despite Answer entities having correct FKs.
-- This migration bridges existing orphans via answers.a.question_id = q.id and is idempotent:
-- re-running produces the same result (no-op on already-correct rows, no error on empty tables).

UPDATE questions q
SET session_id = a.session_id
FROM answers a
WHERE a.question_id = q.id;
