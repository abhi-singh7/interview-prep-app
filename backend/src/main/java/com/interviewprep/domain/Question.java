package com.interviewprep.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity representing a technical interview question generated during an interview session.
 * 
 * <p>Each {@code Question} is associated with a specific {@link InterviewSession} and belongs to a category
 * (e.g., Data Structures, System Design). Questions can be of various types ({@link QuestionType}) including
 * multiple choice, coding prompts, and conceptual explanations.</p>
 * 
 * <h3>Persistence Details:</h3>
 * <ul>
 *   <li><b>Table:</b> {@code questions}</li>
 *   <li><b>ID Strategy:</b> Auto-incrementing identity column ({@link GenerationType#IDENTITY}).</li>
 *   <li><b>Session Relationship:</b> Many-to-one with {@link InterviewSession} — each question belongs to one session.</li>
 *   <li><b>Vector Embedding:</b> Stored as a 768-dimensional vector for semantic search capabilities (requires pgvector extension).</li>
 * </ul>
 * 
 * <h3>Vector Embedding:</h3>
 * <p>The {@code vectorEmbedding} field stores a 768-dimensional float array used for semantic similarity searches.
 * This enables features like finding similar questions or recommending practice topics based on embedding proximity.</p>
 * 
 * @see QuestionType
 * @see InterviewSession
 */
@Entity
@Table(name = "questions")
public class Question {

    /** Auto-generated unique identifier for this question. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The interview session this question belongs to (lazy-loaded). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    /** The category ID for this question (e.g., "data-structures", "system-design"). */
    @Column(name = "category_id")
    private String categoryId;

    /** The type of question (multiple choice, coding prompt, etc.) as defined by {@link QuestionType}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    /** The full text content of the question. Stored as TEXT to support long-form questions. */
    @Column(columnDefinition = "text", nullable = false)
    private String questionText;

    /** A short title or summary for this question (e.g., "Binary Search Basics"). */
    private String title;

    /** Optional description providing additional context or constraints for the question. */
    @Column(columnDefinition = "text")
    private String description;

    /** Optional code prompt text for coding questions that require writing a function. */
    @Column(name = "code_prompt", columnDefinition = "text")
    private String codePrompt;

    // Vector embedding for semantic search capability (Hibernate 7 native VECTOR type)
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Convert(converter = Question.VectorEmbeddingConverter.class)
    @Column(name = "vector_embedding", columnDefinition = "vector(768)", length = 768)
    private double[] vectorEmbedding;

    /** Default no-arg constructor required by JPA providers (e.g., Hibernate). */
    public Question() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public InterviewSession getSession() { return session; }
    public void setSession(InterviewSession session) { this.session = session; }
    
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    
    public QuestionType getType() { return type; }
    public void setType(QuestionType type) { this.type = type; }
    
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCodePrompt() { return codePrompt; }
    public void setCodePrompt(String codePrompt) { this.codePrompt = codePrompt; }
    
    public double[] getVectorEmbedding() { return vectorEmbedding; }
    public void setVectorEmbedding(double[] vectorEmbedding) { this.vectorEmbedding = vectorEmbedding; }

    /**
     * JPA AttributeConverter for handling the 768-dimensional vector embedding.
     * 
     * <p>This converter is required by Hibernate 7 to properly read and write the VECTOR type from/to the database.
     * It delegates the actual conversion to Hibernate's built-in JdbcTypeRegistry.</p>
     */
    public static class VectorEmbeddingConverter implements AttributeConverter<double[], double[]> {
        @Override
        public double[] convertToDatabaseColumn(double[] attribute) {
            return attribute; // Hibernate handles the conversion via JdbcTypeRegistry
        }

        @Override
        public double[] convertToEntityAttribute(double[] dbData) {
            return dbData; // Used directly by Hibernate
        }
    }
}
