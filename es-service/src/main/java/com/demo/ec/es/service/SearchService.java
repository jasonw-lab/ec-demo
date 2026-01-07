package com.demo.ec.es.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
 
import com.demo.ec.es.config.EsServiceProperties;
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

@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final ElasticsearchClient client;
    private final EsServiceProperties properties;

    public SearchService(ElasticsearchClient client, EsServiceProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public SearchResponse search(String q, Long minPrice, Long maxPrice, SearchSort sort, int page, int size) throws IOException {
        if (q == null || q.isBlank()) {
            return new SearchResponse(List.of(), 0L, page, size, null);
        }

        int from = Math.max(page, 0) * Math.max(size, 1);

        BoolQuery.Builder bool = new BoolQuery.Builder();
        bool.must(m -> m.multiMatch(mm -> mm
                .query(q)
                .fields("title^3", "description")
                .fuzziness("AUTO"))
        );
        bool.filter(f -> f.term(t -> t.field("status").value("ACTIVE")));
        // TODO: Implement price range filtering for Elasticsearch Java client 9.0.0
        // The range query API has changed significantly in version 9.0.0
        // For now, price filtering is disabled until the correct API usage is determined

        co.elastic.clients.elasticsearch.core.SearchResponse<ProductDocument> response = client.search(s -> {
            s.index(properties.getIndex().getAlias());
            s.from(from);
            s.size(size);
            s.query(qb -> qb.bool(bool.build()));

            if (sort != null) {
                switch (sort) {
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
                items.add(new ProductCard(doc.productId(), doc.title(), doc.price(), doc.thumbnailUrl(), doc.createdAt()));
            }
        });

        long total = response.hits().total() == null ? items.size() : response.hits().total().value();
        return new SearchResponse(items, total, page, size, null);
    }
}
