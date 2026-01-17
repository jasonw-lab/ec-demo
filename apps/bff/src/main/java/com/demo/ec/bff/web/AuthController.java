package com.demo.ec.bff.web;

import com.demo.ec.bff.application.auth.AuthSessionFilter;
import com.demo.ec.bff.application.auth.SessionData;
import com.demo.ec.bff.application.auth.SessionService;
import com.demo.ec.bff.gateway.client.AccountServiceClient;
import com.demo.ec.bff.config.AuthSessionProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AccountServiceClient accountServiceClient;
    private final SessionService sessionService;
    private final AuthSessionProperties sessionProperties;

    public AuthController(AccountServiceClient accountServiceClient,
                          SessionService sessionService,
                          AuthSessionProperties sessionProperties) {
        this.accountServiceClient = accountServiceClient;
        this.sessionService = sessionService;
        this.sessionProperties = sessionProperties;
    }

    @PostMapping({"/auth/session", "/api/auth/session"})
    public ResponseEntity<AuthStatusResponse> createSession(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) LoginRequest body,
            HttpServletRequest request
    ) {
        String idToken = extractToken(authorizationHeader, body);
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthStatusResponse.error("IDトークンが指定されていません。"));
        }

        FirebaseToken decodedToken;
        try {
            decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.warn("Failed to verify Firebase ID token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthStatusResponse.error("トークンの検証に失敗しました。"));
        }

        // FirebaseToken may not expose a direct getExpirationTimestamp() across SDK versions.
        // Use the 'exp' claim (JWT standard) which is epoch seconds.
        Object expClaim = decodedToken.getClaims().get("exp");
        long expSeconds;
        try {
            if (expClaim instanceof Number) {
                expSeconds = ((Number) expClaim).longValue();
            } else if (expClaim instanceof String) {
                expSeconds = Long.parseLong((String) expClaim);
            } else {
                throw new IllegalArgumentException("exp claim missing");
            }
        } catch (Exception e) {
            log.warn("Failed to parse token exp claim: {}", expClaim, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthStatusResponse.error("トークンの有効期限を取得できません。"));
        }

        Instant tokenExp = Instant.ofEpochSecond(expSeconds);
        if (tokenExp.isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthStatusResponse.error("トークンの有効期限が切れています。"));
        }

        String uid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        String name = (String) decodedToken.getClaims().getOrDefault("name", "");
        @SuppressWarnings("unchecked")
        Map<String, Object> firebase = (Map<String, Object>) decodedToken.getClaims().get("firebase");
        String providerId = firebase != null ? (String) firebase.get("sign_in_provider") : null;

        Long internalUserId = accountServiceClient.syncUser(uid, email, name, providerId);
        if (internalUserId == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthStatusResponse.error("ユーザー同期に失敗しました。"));
        }

        SessionData session;
        try {
            session = sessionService.createSession(uid, internalUserId, tokenExp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthStatusResponse.error("トークンの有効期限が切れています。"));
        } catch (DataAccessException ex) {
            log.error("Redis unavailable while creating session uid={}", uid, ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(AuthStatusResponse.error("セッションストアに接続できません。"));
        }

        ResponseCookie cookie = ResponseCookie.from(sessionProperties.getCookieName(), session.getSid())
                .httpOnly(true)
                .secure(sessionProperties.isSecure())
                .path("/")
                .sameSite(sessionProperties.getSameSite())
                .maxAge(java.time.Duration.between(Instant.now(), tokenExp))
                .build();

        log.info("session_created sid={} uid={} userId={} ip={} ua={}",
                session.getSid(), uid, internalUserId, request.getRemoteAddr(), request.getHeader("User-Agent"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(AuthStatusResponse.loggedIn(internalUserId, email, name));
    }

    @GetMapping({"/auth/status", "/api/auth/status"})
    public ResponseEntity<AuthStatusResponse> getAuthStatus(HttpServletRequest request) {
        String sid = extractSidFromCookie(request);
        if (sid == null || sid.isBlank()) {
            return ResponseEntity.ok(AuthStatusResponse.notLoggedIn());
        }
        SessionData session;
        try {
            session = sessionService.getSession(sid);
        } catch (DataAccessException ex) {
            log.error("Redis unavailable while checking status sid={}", sid, ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(AuthStatusResponse.error("セッションストアに接続できません。"));
        }

        if (session == null) {
            return ResponseEntity.ok(AuthStatusResponse.notLoggedIn());
        }
        return ResponseEntity.ok(AuthStatusResponse.loggedIn(session.getUserId(), null, null));
    }

    @PostMapping({"/auth/logout", "/api/auth/logout"})
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request) {
        String sid = extractSidFromCookie(request);
        try {
            sessionService.deleteSession(sid);
        } catch (DataAccessException ex) {
            log.error("Redis unavailable while deleting session sid={}", sid, ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(LogoutResponse.error("セッションストアに接続できません。"));
        }

        ResponseCookie expired = ResponseCookie.from(sessionProperties.getCookieName(), "")
                .httpOnly(true)
                .secure(sessionProperties.isSecure())
                .path("/")
                .sameSite(sessionProperties.getSameSite())
                .maxAge(0)
                .build();

        log.info("session_deleted sid={} ip={} ua={}", sid, request.getRemoteAddr(), request.getHeader("User-Agent"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .body(LogoutResponse.ok());
    }

    @PostMapping("/api/auth/personal-information")
    public ResponseEntity<PersonalInformationResponse> updatePersonalInformation(
            @RequestBody PersonalInformationRequest request,
            HttpServletRequest httpRequest
    ) {
        SessionData session = (SessionData) httpRequest.getAttribute(AuthSessionFilter.REQ_ATTR_SESSION);
        if (session == null || session.getUserId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(PersonalInformationResponse.error("ログインが必要です。"));
        }

        try {
            accountServiceClient.updatePersonalInformation(
                    session.getUserId(),
                    request.lastName(),
                    request.firstName(),
                    request.lastNameKana(),
                    request.firstNameKana(),
                    request.birthDate(),
                    request.gender()
            );
            return ResponseEntity.ok(PersonalInformationResponse.ok());
        } catch (Exception e) {
            log.error("Error updating personal information", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PersonalInformationResponse.error("本人情報の登録に失敗しました: " + e.getMessage()));
        }
    }

    private String extractToken(String authorizationHeader, LoginRequest body) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length()).trim();
        }
        if (body != null && body.idToken != null && !body.idToken.isBlank()) {
            return body.idToken;
        }
        return null;
    }

    private String extractSidFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (sessionProperties.getCookieName().equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    public record LoginRequest(@JsonProperty("idToken") String idToken) {}

    public record PersonalInformationRequest(
            @JsonProperty("lastName") String lastName,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastNameKana") String lastNameKana,
            @JsonProperty("firstNameKana") String firstNameKana,
            @JsonProperty("birthDate") String birthDate,
            @JsonProperty("gender") String gender
    ) {}

    public record PersonalInformationResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("message") String message
    ) {
        public static PersonalInformationResponse ok() {
            return new PersonalInformationResponse(true, "登録完了");
        }

        public static PersonalInformationResponse error(String message) {
            return new PersonalInformationResponse(false, message);
        }
    }

    public record AuthStatusResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("userId") Long userId,
            @JsonProperty("email") String email,
            @JsonProperty("name") String name,
            @JsonProperty("message") String message
    ) {
        public static AuthStatusResponse loggedIn(Long userId, String email, String name) {
            return new AuthStatusResponse(true, userId, email, name, null);
        }

        public static AuthStatusResponse notLoggedIn() {
            return new AuthStatusResponse(false, null, null, null, null);
        }

        public static AuthStatusResponse error(String message) {
            return new AuthStatusResponse(false, null, null, null, message);
        }
    }

    public record LogoutResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("message") String message
    ) {
        public static LogoutResponse ok() {
            return new LogoutResponse(true, "ログアウトしました");
        }

        public static LogoutResponse error(String message) {
            return new LogoutResponse(false, message);
        }
    }
}

