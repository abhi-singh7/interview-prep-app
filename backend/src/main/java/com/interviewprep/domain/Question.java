package com.interviewprep.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(name = "category_id")
    private String categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(columnDefinition = "text", nullable = false)
    private String questionText;

    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "code_prompt", columnDefinition = "text")
    private String codePrompt;

    // Vector embedding for semantic search capability (Hibernate 7 native VECTOR type)
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Convert(converter = Question.VectorEmbeddingConverter.class)
    @Column(name = "vector_embedding", columnDefinition = "vector(768)", length = 768)
    private double[] vectorEmbedding;

    public Question() {}

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

    // Minimal converter required for Hibernate 7 to read VECTOR type from database
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
