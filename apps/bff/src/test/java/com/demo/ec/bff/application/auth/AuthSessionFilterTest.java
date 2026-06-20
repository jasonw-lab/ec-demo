package com.demo.ec.bff.application.auth;

import com.demo.ec.bff.config.AuthSessionProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthSessionFilterTest {

    private static final String COOKIE_NAME = "ec_session";

    private SessionService sessionService;
    private AuthSessionProperties properties;
    private AuthSessionFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        sessionService = mock(SessionService.class);
        properties = new AuthSessionProperties();
        properties.setCookieName(COOKIE_NAME);
        filter = new AuthSessionFilter(sessionService, properties);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void shouldSkipPublicPaths() throws Exception {
        request.setRequestURI("/api/categories");
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldSkipOptionsRequests() throws Exception {
        request.setMethod("OPTIONS");
        request.setRequestURI("/api/orders/purchase");
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldReturnUnauthorizedWhenCookieMissing() throws Exception {
        request.setRequestURI("/api/orders/purchase");
        filter.doFilter(request, response, filterChain);
        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldReturnUnauthorizedWhenSessionNotFound() throws Exception {
        request.setRequestURI("/api/orders/purchase");
        request.setCookies(new Cookie(COOKIE_NAME, "invalid-sid"));
        when(sessionService.getSession("invalid-sid")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertEquals("{\"code\":\"SESSION_NOT_FOUND\"}", response.getContentAsString());
    }

    @Test
    void shouldAttachSessionAndContinue() throws Exception {
        request.setRequestURI("/api/orders/purchase");
        request.setCookies(new Cookie(COOKIE_NAME, "valid-sid"));
        SessionData session = new SessionData("valid-sid", "uid", 1L, List.of("USER"),
                Instant.now(), Instant.now().plusSeconds(3600));
        when(sessionService.getSession("valid-sid")).thenReturn(session);

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
        Object attached = request.getAttribute(AuthSessionFilter.REQ_ATTR_SESSION);
        assertNotNull(attached);
        assertEquals(1L, ((SessionData) attached).getUserId());
    }

    @Test
    void shouldReturnServiceUnavailableWhenRedisDown() throws Exception {
        request.setRequestURI("/api/orders/purchase");
        request.setCookies(new Cookie(COOKIE_NAME, "sid"));
        when(sessionService.getSession("sid"))
                .thenThrow(new org.springframework.dao.DataAccessException("Redis down") {});

        filter.doFilter(request, response, filterChain);

        assertEquals(503, response.getStatus());
        assertEquals("{\"code\":\"REDIS_UNAVAILABLE\"}", response.getContentAsString());
    }
}
