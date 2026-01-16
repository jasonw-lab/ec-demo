package com.demo.ec.client;

import com.demo.ec.model.Product;
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

import java.util.Optional;

@Component
public class StorageServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StorageServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public StorageServiceClient(RestTemplate restTemplate,
                                @Value("${services.storage.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public Optional<Product> getProduct(Long productId) {
        String url = baseUrl + "/api/storage/products/" + productId;
        try {
            ParameterizedTypeReference<CommonResponse<Product>> type =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<CommonResponse<Product>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    type
            );
            CommonResponse<Product> body = response.getBody();
            if (body == null || !body.isSuccess()) {
                log.warn("StorageServiceClient.getProduct failed: status={}, body={}",
                        response.getStatusCode(), body);
                return Optional.empty();
            }
            return Optional.ofNullable(body.getData());
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            log.error("StorageServiceClient.getProduct - Cannot connect to storage service at {}: {}",
                    url, ex.getMessage());
            return Optional.empty();
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            log.warn("StorageServiceClient.getProduct - HTTP error: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return Optional.empty();
        } catch (org.springframework.web.client.HttpServerErrorException ex) {
            log.error("StorageServiceClient.getProduct - Server error: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return Optional.empty();
        } catch (RestClientException ex) {
            log.error("StorageServiceClient.getProduct error: {}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    public StockLookupResult getStock(Long productId) {
        String url = baseUrl + "/api/storage/stocks/" + productId;
        try {
            ParameterizedTypeReference<CommonResponse<StockResponse>> type =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<CommonResponse<StockResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    type
            );
            CommonResponse<StockResponse> body = response.getBody();
            if (body == null || !body.isSuccess() || body.getData() == null) {
                log.warn("StorageServiceClient.getStock failed: status={}, body={}",
                        response.getStatusCode(), body);
                return new StockLookupResult(Optional.empty(), true);
            }
            return new StockLookupResult(Optional.of(body.getData()), true);
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            log.error("StorageServiceClient.getStock - Cannot connect to storage service at {}: {}",
                    url, ex.getMessage());
            return new StockLookupResult(Optional.empty(), false);
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            log.warn("StorageServiceClient.getStock - HTTP error: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return new StockLookupResult(Optional.empty(), true);
        } catch (org.springframework.web.client.HttpServerErrorException ex) {
            log.error("StorageServiceClient.getStock - Server error: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return new StockLookupResult(Optional.empty(), false);
        } catch (RestClientException ex) {
            log.error("StorageServiceClient.getStock error: {}", ex.getMessage(), ex);
            return new StockLookupResult(Optional.empty(), false);
        }
    }

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

    public static class StockLookupResult {
        private final Optional<StockResponse> stock;
        private final boolean reachable;

        public StockLookupResult(Optional<StockResponse> stock, boolean reachable) {
            this.stock = stock;
            this.reachable = reachable;
        }

        public Optional<StockResponse> stock() { return stock; }
        public boolean reachable() { return reachable; }
    }

    public static class StockResponse {
        private Long productId;
        private Integer total;
        private Integer used;
        private Integer residue;
        private Integer frozen;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
        public Integer getUsed() { return used; }
        public void setUsed(Integer used) { this.used = used; }
        public Integer getResidue() { return residue; }
        public void setResidue(Integer residue) { this.residue = residue; }
        public Integer getFrozen() { return frozen; }
        public void setFrozen(Integer frozen) { this.frozen = frozen; }
    }
}
