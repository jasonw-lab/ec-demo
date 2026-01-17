package com.demo.ec.bff.gateway.client;

import com.demo.ec.bff.gateway.client.dto.SearchResponse;
import com.demo.ec.bff.gateway.client.dto.SuggestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;

@Component
public class ProductSearchClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProductSearchClient(RestTemplate restTemplate,
                               @Value("${es-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public SearchResponse searchProducts(String q, Long minPrice, Long maxPrice,
                                         String sort, int page, int size) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/search/products")
                .queryParam("q", q)
                .queryParam("minPrice", minPrice)
                .queryParam("maxPrice", maxPrice)
                .queryParam("sort", sort)
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();

        ResponseEntity<SearchResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        SearchResponse body = response.getBody();
        if (body == null) {
            return new SearchResponse(Collections.emptyList(), 0L, page, size, null);
        }
        return body;
    }

    public SuggestResponse suggest(String q, int size) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/search/suggest")
                .queryParam("q", q)
                .queryParam("size", size)
                .toUriString();

        ResponseEntity<SuggestResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        SuggestResponse body = response.getBody();
        if (body == null) {
            return new SuggestResponse(Collections.emptyList());
        }
        return body;
    }
}
