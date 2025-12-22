package com.demo.ec.auth;

import com.demo.ec.config.AuthSessionProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class AuthSessionFilter extends OncePerRequestFilter {

    public static final String REQ_ATTR_SESSION = "auth.session";
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/auth/session",
            "/auth/logout",
            "/health",
            "/actuator/health",
            "/swagger-ui",
            "/swagger-ui/",
            "/v3/api-docs",
            "/v3/api-docs/"
    );

    private final SessionService sessionService;
    private final AuthSessionProperties properties;
    private final Logger log = LoggerFactory.getLogger(AuthSessionFilter.class);

    public AuthSessionFilter(SessionService sessionService, AuthSessionProperties properties) {
        this.sessionService = sessionService;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (uri.startsWith("/public/")) {
            return true;
        }
        for (String excluded : EXCLUDED_PATHS) {
            if (uri.equals(excluded) || uri.startsWith(excluded + "/") || uri.startsWith("/api" + excluded)) {
                return true;
            }
        }
        return !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String sid = extractSid(request);
        if (sid == null || sid.isBlank()) {
            writeUnauthorized(response, "SESSION_MISSING");
            return;
        }

        SessionData session;
        try {
            session = sessionService.getSession(sid);
        } catch (DataAccessException ex) {
            log.error("Redis unavailable while fetching session sid={}", sid, ex);
            writeServiceUnavailable(response);
            return;
        }

        if (session == null) {
            writeUnauthorized(response, "SESSION_NOT_FOUND");
            return;
        }

        request.setAttribute(REQ_ATTR_SESSION, session);
        filterChain.doFilter(request, response);
    }

    private String extractSid(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (properties.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String code) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"" + code + "\"}");
    }

    private void writeServiceUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"REDIS_UNAVAILABLE\"}");
    }
}


