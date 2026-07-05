package com.interviewprep.domain;

import lombok.Data;

/**
 * DTO representing the response payload after successful user authentication.
 * <p>
 * This object is returned by the {@link com.interviewprep.auth.AuthController} upon successful login or registration,
 * containing the user's unique identifier, email address, and display name. The frontend uses this information
 * to populate user profile displays and maintain session state.
 * </p>
 * <p>
 * Authentication is handled via JWT tokens stored in HttpOnly cookies, so this response contains only
 * the essential user identity data needed by the client-side application.
 * </p>
 */
@Data
public class AuthResponse {
    /** Unique database identifier for the authenticated user. */
    private Long userId;

    /** Email address associated with the user account, also used as the JWT subject claim. */
    private String email;

    /** Display name of the user, shown in the UI for profile identification. */
    private String name;
}
