package com.demo.ec.controller;

import com.demo.ec.client.AccountServiceClient;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AccountServiceClient accountServiceClient;

    public AuthController(AccountServiceClient accountServiceClient) {
        this.accountServiceClient = accountServiceClient;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) LoginRequest body,
            HttpServletRequest request
    ) {
        try {
            String idToken = extractToken(authorizationHeader, body);
            if (idToken == null || idToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(LoginResponse.error("IDトークンが指定されていません。"));
            }

            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = (String) decodedToken.getClaims().getOrDefault("name", "");
            Map<String, Object> firebase = (Map<String, Object>) decodedToken.getClaims().get("firebase");
            String providerId = firebase != null ? (String) firebase.get("sign_in_provider") : null;

            log.info("Verified Firebase token: uid={}, email={}, provider={}", uid, email, providerId);

            Long internalUserId = accountServiceClient.syncUser(uid, email, name, providerId);
            if (internalUserId == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(LoginResponse.error("ユーザー同期に失敗しました。"));
            }

            HttpSession session = request.getSession(true);
            session.setAttribute("userId", internalUserId);
            session.setAttribute("firebaseUid", uid);
            session.setAttribute("email", email);

            LoginResponse response = LoginResponse.success(internalUserId, uid, email, name, providerId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Failed to verify Firebase ID token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.error("トークンの検証に失敗しました。"));
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

    public record LoginRequest(@JsonProperty("idToken") String idToken) {}

    public record LoginResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("message") String message,
            @JsonProperty("userId") Long userId,
            @JsonProperty("uid") String uid,
            @JsonProperty("email") String email,
            @JsonProperty("name") String name,
            @JsonProperty("providerId") String providerId
    ) {
        public static LoginResponse success(Long userId, String uid, String email, String name, String providerId) {
            return new LoginResponse(true, "ok", userId, uid, email, name, providerId);
        }

        public static LoginResponse error(String message) {
            return new LoginResponse(false, message, null, null, null, null, null);
        }
    }
}


