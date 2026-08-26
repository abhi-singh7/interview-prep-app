-- Store the AI-generated model answer per question for post-interview review
ALTER TABLE questions ADD COLUMN reference_answer TEXT;
