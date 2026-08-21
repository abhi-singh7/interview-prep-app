-- Add type and parent_id columns to categories table for hierarchy support
ALTER TABLE categories ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'TOPIC';
ALTER TABLE categories ALTER COLUMN type DROP DEFAULT;
ALTER TABLE categories ADD CONSTRAINT chk_categories_type CHECK (type IN ('LANGUAGE', 'TOPIC'));

ALTER TABLE categories ADD COLUMN parent_id BIGINT;
ALTER TABLE categories ADD CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id);

-- Add index for efficient topic lookup by parent language
CREATE INDEX idx_categories_parent_id ON categories(parent_id);
