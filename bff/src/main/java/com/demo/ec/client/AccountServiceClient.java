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
            log.info("Calling account service to sync user: url={}, firebaseUid={}, email={}", url, firebaseUid, email);
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
                log.warn("AccountServiceClient.syncUser failed: status={}, body={}", 
                        response.getStatusCode(), body);
                return null;
            }
            log.info("User synced successfully: userId={}", body.getData().id());
            return body.getData().id();
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            log.error("AccountServiceClient.syncUser - Cannot connect to account service at {}: {}", 
                    url, ex.getMessage());
            return null;
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            log.error("AccountServiceClient.syncUser - HTTP error: status={}, body={}", 
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        } catch (org.springframework.web.client.HttpServerErrorException ex) {
            log.error("AccountServiceClient.syncUser - Server error: status={}, body={}", 
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        } catch (RestClientException ex) {
            log.error("AccountServiceClient.syncUser error: {}", ex.getMessage(), ex);
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


