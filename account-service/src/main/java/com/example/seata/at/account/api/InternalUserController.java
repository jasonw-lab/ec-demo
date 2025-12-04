package com.example.seata.at.account.api;

import com.example.seata.at.account.api.dto.CommonResponse;
import com.example.seata.at.account.service.UserService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
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
        Long id = userService.syncUser(req.firebaseUid(), req.email(), req.name(), req.providerId());
        return CommonResponse.ok(new UserSyncResponse(id));
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
        userService.updatePersonalInformation(req.userId(), req.lastName(), req.firstName(),
                req.lastNameKana(), req.firstNameKana(), req.birthDate(), req.gender());
        return CommonResponse.ok(null);
    }
}


