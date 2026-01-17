package com.demo.ec.es.application;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import com.demo.ec.es.config.EsServiceProperties;
import com.demo.ec.es.domain.ElasticsearchOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing Elasticsearch index lifecycle.
 * Handles index creation, alias management, and reindexing operations.
 */
@Service
public class IndexAdminService {
    private static final Logger log = LoggerFactory.getLogger(IndexAdminService.class);

    private final ElasticsearchClient client;
    private final EsServiceProperties properties;

    public IndexAdminService(ElasticsearchClient client, EsServiceProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * Initializes the product index if it doesn't exist.
     * Creates index with proper mappings and sets up alias.
     */
    public void initIndexIfMissing() {
        String indexName = properties.getIndex().getName();
        
        try {
            boolean exists = client.indices().exists(b -> b.index(indexName)).value();
            if (exists) {
                log.debug("Index already exists: {}", indexName);
                return;
            }

            log.info("Creating index: {}", indexName);
            createIndex(indexName);
            ensureAlias(indexName, properties.getIndex().getAlias());
            log.info("Index created successfully: {}", indexName);
            
        } catch (IOException ex) {
            log.error("Failed to initialize index: {}", indexName, ex);
            throw new ElasticsearchOperationException("Failed to initialize index: " + indexName, ex);
        }
    }

    public void ensureAlias(String indexName, String alias) throws IOException {
        if (alias == null || alias.isBlank() || alias.equals(indexName)) {
            return;
        }
        UpdateAliasesRequest request = UpdateAliasesRequest.of(a -> a
                .actions(
                        Action.of(act -> act.remove(r -> r.index("*").alias(alias))),
                        Action.of(act -> act.add(ad -> ad.index(indexName).alias(alias)))
                ));
        client.indices().updateAliases(request);
    }

    public void reindex(String source, String target, String alias) throws IOException {
        boolean targetExists = client.indices().exists(b -> b.index(target)).value();
        if (!targetExists) {
            createIndex(target);
        }

        client.reindex(r -> r
                .source(s -> s.index(source))
                .dest(d -> d.index(target))
        );

        if (alias != null && !alias.isBlank()) {
            ensureAlias(target, alias);
        }
    }

    private void createIndex(String indexName) throws IOException {
        Map<String, Property> props = new HashMap<>();
        props.put("productId", Property.of(p -> p.long_(l -> l)));
        props.put("title", Property.of(p -> p.text(t -> t.fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256))))));
        props.put("description", Property.of(p -> p.text(t -> t)));
        props.put("price", Property.of(p -> p.long_(l -> l)));
        props.put("status", Property.of(p -> p.keyword(k -> k)));
        props.put("thumbnailUrl", Property.of(p -> p.keyword(k -> k)));
        props.put("createdAt", Property.of(p -> p.date(d -> d)));

        CreateIndexRequest request = CreateIndexRequest.of(c -> c
                .index(indexName)
                .mappings(TypeMapping.of(m -> m.properties(props))));
        client.indices().create(request);
    }
}
