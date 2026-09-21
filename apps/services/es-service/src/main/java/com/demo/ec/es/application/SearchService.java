package com.demo.ec.es.application;

import com.demo.ec.es.config.EsServiceProperties;
import com.demo.ec.es.domain.ElasticsearchOperationException;
import com.demo.ec.es.domain.ProductCard;
import com.demo.ec.es.domain.ProductDocument;
import com.demo.ec.es.domain.SearchResponse;
import com.demo.ec.es.domain.SearchSort;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final OpenSearchClient client;
    private final EsServiceProperties properties;

    public SearchService(OpenSearchClient client, EsServiceProperties properties) {
        this.client = client;
        this.properties = properties;
    }

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
        bool.filter(f -> f.term(t -> t.field("status").value(FieldValue.of("ACTIVE"))));
        if (minPrice != null || maxPrice != null) {
            bool.filter(f -> f.range(r -> {
                r.field("price");
                if (minPrice != null) r.gte(JsonData.of(minPrice.doubleValue()));
                if (maxPrice != null) r.lte(JsonData.of(maxPrice.doubleValue()));
                return r;
            }));
        }

        try {
            org.opensearch.client.opensearch.core.SearchResponse<ProductDocument> response = client.search(s -> {
                s.index(properties.getIndex().getAlias());
                s.from(from);
                s.size(size);
                s.query(qb -> qb.bool(bool.build()));

                if (sort == SearchSort.relevance) {
                    s.sort(so -> so.score(sc -> sc.order(SortOrder.Desc)));
                } else if (sort == SearchSort.newest) {
                    s.sort(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc)));
                } else if (sort == SearchSort.price_asc) {
                    s.sort(so -> so.field(f -> f.field("price").order(SortOrder.Asc)));
                } else if (sort == SearchSort.price_desc) {
                    s.sort(so -> so.field(f -> f.field("price").order(SortOrder.Desc)));
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
            boolean pathHasBucket = path.equals("/" + bucket) || path.startsWith("/" + bucket + "/") || path.contains("/" + bucket + "/");
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
