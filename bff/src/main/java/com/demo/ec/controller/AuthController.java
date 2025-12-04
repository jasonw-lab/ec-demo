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
import org.springframework.web.bind.annotation.GetMapping;
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
            @SuppressWarnings("unchecked")
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
            session.setAttribute("name", name);

            LoginResponse response = LoginResponse.success(internalUserId, uid, email, name, providerId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.error("Firebase not initialized. Please check firebase-service-account.json configuration.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.error("Firebaseが初期化されていません。設定を確認してください。"));
        } catch (com.google.firebase.auth.FirebaseAuthException e) {
            log.warn("Failed to verify Firebase ID token: {}", e.getMessage(), e);
            String errorMessage = "トークンの検証に失敗しました。";
            if (e.getErrorCode() != null) {
                String errorCode = e.getErrorCode().name();
                switch (errorCode) {
                    case "INVALID_ID_TOKEN":
                        errorMessage = "無効なトークンです。再度ログインしてください。";
                        break;
                    case "EXPIRED_ID_TOKEN":
                        errorMessage = "トークンの有効期限が切れています。再度ログインしてください。";
                        break;
                    case "REVOKED_ID_TOKEN":
                        errorMessage = "トークンが無効化されています。再度ログインしてください。";
                        break;
                    default:
                        errorMessage = "トークンの検証に失敗しました: " + errorCode;
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.error(errorMessage));
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.error("ログイン処理中にエラーが発生しました: " + e.getMessage()));
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

    @GetMapping("/status")
    public ResponseEntity<AuthStatusResponse> getAuthStatus(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.ok(AuthStatusResponse.notLoggedIn());
        }

        Long userId = (Long) session.getAttribute("userId");
        String email = (String) session.getAttribute("email");
        String name = (String) session.getAttribute("name");
        
        if (userId == null) {
            return ResponseEntity.ok(AuthStatusResponse.notLoggedIn());
        }

        return ResponseEntity.ok(AuthStatusResponse.loggedIn(userId, email, name));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
                log.info("User logged out, session invalidated");
            }
            return ResponseEntity.ok(LogoutResponse.ok());
        } catch (Exception e) {
            log.error("Error during logout", e);
            return ResponseEntity.ok(LogoutResponse.ok()); // エラーでも成功として返す
        }
    }

    @PostMapping("/personal-information")
    public ResponseEntity<PersonalInformationResponse> updatePersonalInformation(
            @RequestBody PersonalInformationRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(PersonalInformationResponse.error("ログインが必要です。"));
            }

            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(PersonalInformationResponse.error("ユーザーIDが見つかりません。"));
            }

            accountServiceClient.updatePersonalInformation(
                    userId,
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
            @JsonProperty("name") String name
    ) {
        public static AuthStatusResponse loggedIn(Long userId, String email, String name) {
            return new AuthStatusResponse(true, userId, email, name);
        }

        public static AuthStatusResponse notLoggedIn() {
            return new AuthStatusResponse(false, null, null, null);
        }
    }

    public record LogoutResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("message") String message
    ) {
        public static LogoutResponse ok() {
            return new LogoutResponse(true, "ログアウトしました");
        }
    }

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


