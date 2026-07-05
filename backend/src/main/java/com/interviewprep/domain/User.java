package com.interviewprep.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * JPA entity representing a registered user of the interview preparation platform.
 * 
 * <p>This entity stores core authentication and profile information for each user, including their name,
 * email (unique identifier), hashed password, and account creation timestamp.</p>
 * 
 * <h3>Persistence Details:</h3>
 * <ul>
 *   <li><b>Table:</b> {@code users}</li>
 *   <li><b>ID Strategy:</b> Auto-incrementing identity column ({@link GenerationType#IDENTITY}).</li>
 *   <li><b>Email Constraint:</b> Unique and non-null — used as the primary login credential.</li>
 *   <li><b>Password Storage:</b> Stored as BCrypt hash (never plaintext) via {@code PasswordEncoder}.</li>
 * </ul>
 * 
 * @see UserRepository
 */
@Entity
@Table(name = "users")
public class User {

    /** Auto-generated unique identifier for this user. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user's display name (e.g., "John Doe"). */
    private String name;

    /** Unique email address used as the primary login credential. Must be non-null and unique across all users. */
    @Column(unique = true, nullable = false)
    private String email;

    /** BCrypt-hashed password — never stored in plaintext. */
    @Column(nullable = false)
    private String password;

    /** Timestamp when this user account was created, automatically set by Hibernate on insert. */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Default no-arg constructor required by JPA providers (e.g., Hibernate). */
    public User() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
