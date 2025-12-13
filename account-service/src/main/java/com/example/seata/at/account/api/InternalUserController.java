package com.example.seata.at.account.api;

import com.example.seata.at.account.api.dto.CommonResponse;
import com.example.seata.at.account.service.UserService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Firebase UID ベースでユーザーを同期する内部用API。
 * BFF からのみ呼び出されることを想定している。
 */
@RestController
@RequestMapping("/api/account/internal/users")
public class InternalUserController {

    private static final Logger log = LoggerFactory.getLogger(InternalUserController.class);

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    public record UserSyncRequest(
            @JsonProperty("firebaseUid") String firebaseUid,
            @JsonProperty("email") String email,
            @JsonProperty("name") String name,
            @JsonProperty("providerId") String providerId
    ) {}

    public record UserSyncResponse(
            Long id
    ) {}

    /**
     * Firebase UID を基準にユーザーを同期し、内部ユーザーIDを返却する。
     */
    @PostMapping("/sync")
    public CommonResponse<UserSyncResponse> syncUser(@Valid @RequestBody UserSyncRequest req) {
        try {
            log.info("Syncing user: firebaseUid={}, email={}, name={}, providerId={}", 
                    req.firebaseUid(), req.email(), req.name(), req.providerId());
            Long id = userService.syncUser(req.firebaseUid(), req.email(), req.name(), req.providerId());
            if (id == null) {
                log.error("UserService.syncUser returned null for firebaseUid={}", req.firebaseUid());
                return CommonResponse.fail("Failed to sync user: service returned null");
            }
            log.info("User synced successfully: firebaseUid={}, userId={}", req.firebaseUid(), id);
            return CommonResponse.ok(new UserSyncResponse(id));
        } catch (Exception ex) {
            log.error("Error syncing user: firebaseUid={}, email={}", req.firebaseUid(), req.email(), ex);
            throw ex; // Re-throw to be handled by @ExceptionHandler
        }
    }

    public record PersonalInformationRequest(
            @JsonProperty("userId") Long userId,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastNameKana") String lastNameKana,
            @JsonProperty("firstNameKana") String firstNameKana,
            @JsonProperty("birthDate") String birthDate,
            @JsonProperty("gender") String gender
    ) {}

    /**
     * ユーザーIDを基準に本人情報を更新する。
     */
    @PostMapping("/personal-information")
    public CommonResponse<Void> updatePersonalInformation(@Valid @RequestBody PersonalInformationRequest req) {
        try {
            log.info("Updating personal information: userId={}", req.userId());
            userService.updatePersonalInformation(req.userId(), req.lastName(), req.firstName(),
                    req.lastNameKana(), req.firstNameKana(), req.birthDate(), req.gender());
            log.info("Personal information updated successfully: userId={}", req.userId());
            return CommonResponse.ok(null);
        } catch (IllegalArgumentException ex) {
            log.error("Invalid request for personal information update: userId={}", req.userId(), ex);
            return CommonResponse.fail(ex.getMessage());
        } catch (Exception ex) {
            log.error("Error updating personal information: userId={}", req.userId(), ex);
            throw ex; // Re-throw to be handled by @ExceptionHandler
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception in InternalUserController", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.fail("Internal server error: " + ex.getMessage()));
    }
}


