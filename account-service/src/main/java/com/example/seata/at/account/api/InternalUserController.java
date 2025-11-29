package com.example.seata.at.account.api;

import com.example.seata.at.account.api.dto.CommonResponse;
import com.example.seata.at.account.service.UserService;
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
            String firebaseUid,
            String email,
            String name,
            String providerId
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
}


