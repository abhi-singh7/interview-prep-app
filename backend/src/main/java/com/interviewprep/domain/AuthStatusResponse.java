package com.interviewprep.domain;

/**
 * Response object representing the authentication status of a user.
 * 
 * <p>This DTO is returned by the {@code /auth/status} endpoint to indicate whether
 * the current request contains a valid JWT token for authentication.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // AuthController.getAuthStatus() returns this response
 * public ResponseEntity<AuthStatusResponse> getAuthStatus(HttpServletRequest request) {
 *     String token = extractJwtToken(request);
 *     boolean authenticated = token != null && jwtService.isTokenValid(token);
 *     return ResponseEntity.ok(new AuthStatusResponse(authenticated));
 * }
 * }</pre>
 */
public record AuthStatusResponse(boolean authenticated) {

    /**
     * Creates an authenticated status response.
     * 
     * @param authenticated {@code true} if a valid JWT token was found, {@code false} otherwise
     */
    public AuthStatusResponse {
        // Record validation - authenticated is boolean primitive, always valid
    }
}
