package com.demo.ec.es.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.demo.ec.es.config.EsServiceProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SuggestService {
    private final ElasticsearchClient client;
    private final EsServiceProperties properties;

    public SuggestService(ElasticsearchClient client, EsServiceProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public List<String> suggest(String q, int size) throws IOException {
        if (q == null || q.isBlank()) {
            return List.of();
        }

        BoolQuery.Builder bool = new BoolQuery.Builder();
        bool.must(m -> m.matchBoolPrefix(mp -> mp.field("title").query(q)));
        bool.filter(f -> f.term(t -> t.field("status").value("ACTIVE")));

        co.elastic.clients.elasticsearch.core.SearchResponse<Void> response = client.search(s -> s
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
        return suggestions;
    }
}
