## ADDED Requirements

### Requirement: User performance overview shows basic metrics
The system SHALL display a user's overall performance metrics including total sessions completed and average score across all sessions.

#### Scenario: Display basic metrics on dashboard
- **WHEN** user navigates to the analytics dashboard as an authenticated user
- **THEN** system displays total number of completed interview sessions and their average score

### Requirement: Category breakdown is displayed per topic
The system SHALL show a performance breakdown by category (topic) — displaying the average score achieved in each topic area across all completed interviews.

#### Scenario: Display category-level scores
- **WHEN** user views analytics dashboard
- **THEN** system displays average scores for each interview topic/category the user has been assessed on, using bar chart visualization

### Requirement: Score history is displayed chronologically
The system SHALL display a chronological record of completed interview sessions with their scores and topics, sorted by date (most recent first).

#### Scenario: Display score history list
- **WHEN** user views analytics dashboard
- **THEN** system displays a table or list showing each completed session's date, topic, difficulty level, and score
