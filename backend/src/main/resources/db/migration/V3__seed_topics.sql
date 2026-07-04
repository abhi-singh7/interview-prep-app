-- Seed data for languages (type = LANGUAGE) — parent_id is NULL
INSERT INTO categories (id, name, type, parent_id) VALUES
(10, 'Languages', 'LANGUAGE', NULL),
(11, 'Core Java', 'TOPIC', 10),
(12, 'Generics', 'TOPIC', 10),
(13, 'Collections', 'TOPIC', 10),
(14, 'Streams', 'TOPIC', 10),
(15, 'OOP', 'TOPIC', 10),
(16, 'Concurrency', 'TOPIC', 10)
ON CONFLICT DO NOTHING;

-- Re-seed categories with proper hierarchy: each language gets a LANGUAGE category and its TOPIC sub-categories
DELETE FROM categories WHERE type = 'LANGUAGE';
DELETE FROM categories WHERE parent_id IS NOT NULL;

-- Insert Languages (type = LANGUAGE, parent_id = NULL)
INSERT INTO categories (id, name, type, parent_id) VALUES
(10, 'Java', 'LANGUAGE', NULL),
(20, 'Python', 'LANGUAGE', NULL),
(30, 'SQL', 'LANGUAGE', NULL),
(40, 'Angular', 'LANGUAGE', NULL),
(50, 'Spring Boot', 'LANGUAGE', NULL)
ON CONFLICT DO NOTHING;

-- Insert Topics for Java (type = TOPIC, parent_id = 10)
INSERT INTO categories (id, name, type, parent_id) VALUES
(11, 'Core Java', 'TOPIC', 10),
(12, 'Generics', 'TOPIC', 10),
(13, 'Collections', 'TOPIC', 10),
(14, 'Streams', 'TOPIC', 10),
(15, 'OOP', 'TOPIC', 10),
(16, 'Concurrency', 'TOPIC', 10)
ON CONFLICT DO NOTHING;

-- Insert Topics for Python (type = TOPIC, parent_id = 20)
INSERT INTO categories (id, name, type, parent_id) VALUES
(21, 'Core Python', 'TOPIC', 20),
(22, 'Data Structures', 'TOPIC', 20),
(23, 'Web Frameworks', 'TOPIC', 20),
(24, 'Decorators', 'TOPIC', 20)
ON CONFLICT DO NOTHING;

-- Insert Topics for SQL (type = TOPIC, parent_id = 30)
INSERT INTO categories (id, name, type, parent_id) VALUES
(31, 'Joins', 'TOPIC', 30),
(32, 'Indexes', 'TOPIC', 30),
(33, 'Normalization', 'TOPIC', 30),
(34, 'Window Functions', 'TOPIC', 30),
(35, 'Query Optimization', 'TOPIC', 30)
ON CONFLICT DO NOTHING;

-- Insert Topics for Angular (type = TOPIC, parent_id = 40)
INSERT INTO categories (id, name, type, parent_id) VALUES
(41, 'Components', 'TOPIC', 40),
(42, 'Services', 'TOPIC', 40),
(43, 'RxJS', 'TOPIC', 40),
(44, 'Forms', 'TOPIC', 40),
(45, 'Routing', 'TOPIC', 40)
ON CONFLICT DO NOTHING;

-- Insert Topics for Spring Boot (type = TOPIC, parent_id = 50)
INSERT INTO categories (id, name, type, parent_id) VALUES
(51, 'REST APIs', 'TOPIC', 50),
(52, 'Security', 'TOPIC', 50),
(53, 'JPA/Hibernate', 'TOPIC', 50),
(54, 'Actuator', 'TOPIC', 50)
ON CONFLICT DO NOTHING;
