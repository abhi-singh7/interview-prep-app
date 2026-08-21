package com.interviewprep.config;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class AuthenticationContext {

    private static final String USER_ID_ATTRIBUTE = "currentUserId";
    private static final String EMAIL_ATTRIBUTE = "currentEmail";

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
