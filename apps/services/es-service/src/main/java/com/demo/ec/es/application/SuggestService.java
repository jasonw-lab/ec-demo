package com.demo.ec.es.application;

import com.demo.ec.es.config.EsServiceProperties;
import com.demo.ec.es.domain.ElasticsearchOperationException;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SuggestService {
    private static final Logger log = LoggerFactory.getLogger(SuggestService.class);

    private final OpenSearchClient client;
    private final EsServiceProperties properties;

    public SuggestService(OpenSearchClient client, EsServiceProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public List<String> suggest(String q, int size) {
        if (q == null || q.isBlank() || q.trim().length() < 2) {
            log.debug("Query too short for suggestions: '{}'", q);
            return List.of();
        }

        log.debug("Generating suggestions: q={}, size={}", q, size);

        BoolQuery.Builder bool = new BoolQuery.Builder();
        bool.must(m -> m.multiMatch(mm -> {
            mm.query(q).fields("title");
            if (q.trim().length() >= 3) {
                mm.fuzziness("AUTO");
            }
            return mm;
        }));
        bool.filter(f -> f.term(t -> t.field("status").value(FieldValue.of("ACTIVE"))));

        try {
            org.opensearch.client.opensearch.core.SearchResponse<Void> response = client.search(s -> s
                            .index(properties.getIndex().getAlias())
                            .size(0)
                            .query(qb -> qb.bool(bool.build()))
                            .aggregations("titles", a -> a.terms(t -> t.field("title.keyword").size(size))),
                    Void.class);

            List<String> suggestions = new ArrayList<>();
            if (response.aggregations() != null && response.aggregations().get("titles") != null) {
                var buckets = response.aggregations().get("titles").sterms().buckets().array();
                for (var b : buckets) {
                    if (b.key() != null) {
                        suggestions.add(b.key().toString());
                    }
                }
            }

            log.debug("Suggestions generated: count={}", suggestions.size());
            return suggestions;

        } catch (IOException ex) {
            log.error("Suggest operation failed", ex);
            throw new ElasticsearchOperationException("Suggest operation failed", ex);
        }
    }
}
