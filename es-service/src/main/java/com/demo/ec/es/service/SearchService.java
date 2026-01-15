package com.demo.ec.es.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.json.JsonData;
import com.demo.ec.es.config.EsServiceProperties;
import com.demo.ec.es.exception.ElasticsearchOperationException;
import com.demo.ec.es.model.ProductCard;
import com.demo.ec.es.model.ProductDocument;
import com.demo.ec.es.model.SearchResponse;
import com.demo.ec.es.model.SearchSort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for searching products in Elasticsearch.
 * Supports full-text search with fuzziness, price filtering, and multiple sort options.
 */
@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final ElasticsearchClient client;
    private final EsServiceProperties properties;

    public SearchService(ElasticsearchClient client, EsServiceProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * Searches products with multi_match query, filters, and sorting.
     *
     * @param q        search query (optional, searches title^3 and description)
     * @param minPrice minimum price filter (optional)
     * @param maxPrice maximum price filter (optional)
     * @param sort     sort option (relevance, newest, price_asc, price_desc)
     * @param page     page number (0-indexed)
     * @param size     page size
     * @return search response with products, pagination, and total count
     */
    public SearchResponse search(String q, Long minPrice, Long maxPrice, SearchSort sort, int page, int size) {
        int from = Math.max(page, 0) * Math.max(size, 1);

        log.debug("Executing search: q={}, minPrice={}, maxPrice={}, sort={}, from={}, size={}",
                q, minPrice, maxPrice, sort, from, size);

        BoolQuery.Builder bool = new BoolQuery.Builder();
        if (q != null && !q.isBlank()) {
            bool.must(m -> m.multiMatch(mm -> {
                mm.query(q)
                        .fields("title^3", "description");
                if (q.length() >= 3) {
                    mm.fuzziness("AUTO");
                }
                return mm;
            }));
        }
        bool.filter(f -> f.term(t -> t.field("status").value("ACTIVE")));
        if (minPrice != null || maxPrice != null) {
            bool.filter(f -> f.range(r -> {
                r.number(n -> {
                    n.field("price");
                    if (minPrice != null) {
                        n.gte(minPrice.doubleValue());
                    }
                    if (maxPrice != null) {
                        n.lte(maxPrice.doubleValue());
                    }
                    return n;
                });
                return r;
            }));
        }

        try {
            co.elastic.clients.elasticsearch.core.SearchResponse<ProductDocument> response = client.search(s -> {
                s.index(properties.getIndex().getAlias());
                s.from(from);
                s.size(size);
                s.query(qb -> qb.bool(bool.build()));

                if (sort != null) {
                    switch (sort) {
                        case relevance -> s.sort(so -> so.score(sc -> sc.order(SortOrder.Desc)));
                        case newest -> s.sort(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc)));
                        case price_asc -> s.sort(so -> so.field(f -> f.field("price").order(SortOrder.Asc)));
                        case price_desc -> s.sort(so -> so.field(f -> f.field("price").order(SortOrder.Desc)));
                        default -> {
                        }
                    }
                }
                return s;
        }, ProductDocument.class);

            List<ProductCard> items = new ArrayList<>();
            response.hits().hits().forEach(hit -> {
                ProductDocument doc = hit.source();
                if (doc != null) {
                    String thumbnailUrl = normalizeThumbnailUrl(doc.thumbnailUrl());
                    items.add(new ProductCard(doc.productId(), doc.title(), doc.price(), thumbnailUrl, doc.createdAt()));
                }
            });

            long total = response.hits().total() == null ? items.size() : response.hits().total().value();
            
            log.debug("Search completed: hits={}, total={}", items.size(), total);
            return new SearchResponse(items, total, page, size, null);
            
        } catch (IOException ex) {
            log.error("Search failed", ex);
            throw new ElasticsearchOperationException("Search operation failed", ex);
        }
    }

    private String normalizeThumbnailUrl(String thumbnailUrl) {
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
            return thumbnailUrl;
        }
        String bucket = properties.getMinio().getBucket();
        if (bucket == null || bucket.isBlank()) {
            return thumbnailUrl;
        }
        try {
            java.net.URI uri = java.net.URI.create(thumbnailUrl);
            String host = uri.getHost();
            String path = uri.getPath() == null ? "" : uri.getPath();
            if (host == null) {
                return thumbnailUrl;
            }
            boolean hostHasBucket = host.startsWith(bucket + ".");
            boolean pathHasBucket = path.equals("/" + bucket) || path.startsWith("/" + bucket + "/");
            if (hostHasBucket || pathHasBucket) {
                return thumbnailUrl;
            }
            String normalizedPath = "/" + bucket + (path.startsWith("/") ? path : "/" + path);
            return new java.net.URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    normalizedPath,
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();
        } catch (Exception ex) {
            log.warn("Failed to normalize thumbnailUrl: {}", thumbnailUrl);
            return thumbnailUrl;
        }
    }
}
