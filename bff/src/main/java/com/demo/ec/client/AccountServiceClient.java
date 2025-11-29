package com.demo.ec.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class AccountServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AccountServiceClient(RestTemplate restTemplate,
                                @Value("${services.account.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public Long syncUser(String firebaseUid, String email, String name, String providerId) {
        String url = baseUrl + "/api/account/internal/users/sync";
        try {
            ParameterizedTypeReference<CommonResponse<UserSyncResponse>> type =
                    new ParameterizedTypeReference<>() {};
            UserSyncRequest req = new UserSyncRequest(firebaseUid, email, name, providerId);
            ResponseEntity<CommonResponse<UserSyncResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(req),
                    type
            );
            CommonResponse<UserSyncResponse> body = response.getBody();
            if (body == null || !body.isSuccess() || body.getData() == null) {
                log.warn("AccountServiceClient.syncUser failed: body={}", body);
                return null;
            }
            return body.getData().id();
        } catch (RestClientException ex) {
            log.error("AccountServiceClient.syncUser error {}", ex.toString());
            return null;
        }
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

    public static class CommonResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}


