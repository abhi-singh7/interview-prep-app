package com.interviewprep.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationContextTest {

    private AuthenticationContext authContext;

    @BeforeEach
    void setUp() {
        authContext = new AuthenticationContext();
    }

    @Test
    void setCurrentUserId_stores_user_id_in_request_attributes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attrs);

        try {
            authContext.setCurrentUserId(42L);

            Object storedValue = request.getAttribute("currentUserId");
            assertThat(storedValue).isEqualTo(42L);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void setCurrentEmail_stores_email_in_request_attributes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attrs);

        try {
            authContext.setCurrentEmail("test@example.com");

            Object storedValue = request.getAttribute("currentEmail");
            assertThat(storedValue).isEqualTo("test@example.com");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void getCurrentUserId_returns_stored_user_id() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attrs);

        try {
            authContext.setCurrentUserId(99L);

            Long userId = authContext.getCurrentUserId();

            assertThat(userId).isEqualTo(99L);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void getCurrentEmail_returns_stored_email() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attrs);

        try {
            authContext.setCurrentEmail("user@example.com");

            String email = authContext.getCurrentEmail();

            assertThat(email).isEqualTo("user@example.com");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void getCurrentUserId_throws_when_no_request_context() {
        RequestContextHolder.resetRequestAttributes();

        assertThatThrownBy(() -> authContext.getCurrentUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated user");
    }

    @Test
    void getCurrentEmail_throws_when_no_request_context() {
        RequestContextHolder.resetRequestAttributes();

        assertThatThrownBy(() -> authContext.getCurrentEmail())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated user");
    }
}
