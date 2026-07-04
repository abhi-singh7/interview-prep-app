package com.interviewprep.config;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Provides access to the currently authenticated user's identity within a request scope.
 * 
 * <h2>Usage Pattern</h2>
 * <p>This component stores and retrieves user authentication data as request attributes, populated by
 * {@link JwtAuthenticationFilter} during the security filter chain. Controllers call
 * {@code getCurrentUserId()} or {@code getCurrentEmail()} to access identity without re-parsing JWT tokens.</p>
 * 
 * <h2>Request Attribute Storage</h2>
 * <p>The filter sets two attributes on each authenticated request:</p>
 * <ul>
 *   <li>{@code "currentUserId"} (Long) — the JPA entity ID of the authenticated user</li>
 *   <li>{@code "currentEmail"} (String) — the email address used as JWT subject claim</li>
 * </ul>
 * 
 * <p>These attributes are scoped to {@link RequestAttributes#SCOPE_REQUEST}, meaning they exist only
 * for the duration of a single HTTP request and are automatically cleaned up by Spring's dispatcher.</p>
 * 
 * <h2>Thread Safety</h2>
 * <p>{@code RequestContextHolder} uses ThreadLocal internally, so this component is safe to use in
 * multi-threaded servlet containers. However, it MUST NOT be called from background threads or async
 * contexts outside of an active HTTP request — doing so will throw {@link IllegalStateException}.</p>
 */
@Component
public class AuthenticationContext {

    /** Request attribute key for the authenticated user's JPA entity ID. Set by JwtAuthenticationFilter. */
    private static final String USER_ID_ATTRIBUTE = "currentUserId";

    /** Request attribute key for the authenticated user's email (JWT subject claim). Set by JwtAuthenticationFilter. */
    private static final String EMAIL_ATTRIBUTE = "currentEmail";

    /**
     * Returns the authenticated user's JPA entity ID from the current request attributes.
     * 
     * @return the Long user ID stored in request scope
     * @throws IllegalStateException if no authenticated user is present (filter has not run or token invalid)
     */
    public Long getCurrentUserId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            Object userId = attrs.getAttribute(USER_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (userId instanceof Number num) {
                return num.longValue();
            }
        }
        throw new IllegalStateException("No authenticated user — currentUserId not found");
    }

    public String getCurrentEmail() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            Object email = attrs.getAttribute(EMAIL_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (email instanceof String s) {
                return s;
            }
        }
        throw new IllegalStateException("No authenticated user — currentEmail not found");
    }

    public void setCurrentUserId(Long userId) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.setAttribute(USER_ID_ATTRIBUTE, userId, RequestAttributes.SCOPE_REQUEST);
        }
    }

    public void setCurrentEmail(String email) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.setAttribute(EMAIL_ATTRIBUTE, email, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
