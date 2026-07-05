package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entity operations.
 * 
 * <p>This repository provides CRUD operations and custom query methods for managing user accounts in the database.</p>
 * 
 * <h3>Key Operations:</h3>
 * <ul>
 *   <li>{@code existsByEmail(String)} — Check if a user account already exists with the given email (used during registration).</li>
 *   <li>{@code findByEmail(String)} — Retrieve a user by their unique email address (used during login).</li>
 * </ul>
 * 
 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Checks whether a user account already exists with the given email address.
     * 
     * <p>This method is used during registration to prevent duplicate accounts.</p>
     * 
     * @param email The email address to check for uniqueness.
     * @return {@code true} if a user with this email already exists; {@code false} otherwise.
     */
    boolean existsByEmail(String email);
    
    /**
     * Finds a user by their unique email address.
     * 
     * <p>This method is used during login to retrieve the user account for password verification.</p>
     * 
     * @param email The email address of the user to find.
     * @return An {@link Optional} containing the user if found, or empty if no user exists with that email.
     */
    Optional<User> findByEmail(String email);
}
