package org.codrshi.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.error.SemseaException;
import org.codrshi.metric.MetricType;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public final class ChromaClient extends Client {
    private static final Logger log = LogManager.getLogger(ChromaClient.class);

    private static final String SERVICE_NAME = "ChromaDB";

    public  static final String BASE_URL = "http://localhost:8000/api/v2";
    private static final String HEARTBEAT = "/heartbeat";
    private static final String DELETE_COLLECTION = "/tenants/default_tenant/databases/default_database/collections/%s";
    private static final String GET_OR_CREATE_COLLECTION = "/tenants/default_tenant/databases/default_database/collections";
    private static final String SAVE_EMBEDDINGS = "/tenants/default_tenant/databases/default_database/collections/%s/add";
    private static final String SEARCH_EMBEDDINGS = "/tenants/default_tenant/databases/default_database/collections/%s/query";
    private static final String DELETE_EMBEDDINGS = "/tenants/default_tenant/databases/default_database/collections/%s/delete";

    private final ObjectMapper objectMapper;

    public ChromaClient() {
        super(SERVICE_NAME);
        objectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public void heartbeat() {
        log.debug("Pinging ChromaDB heartbeat endpoint");
        HttpResponse<String> response = executeGet(BASE_URL + HEARTBEAT, MetricType.CHROMA_HEARTBEAT);
        if(response.statusCode() != 200) {
            log.error("ChromaDB heartbeat failed: status={} body={}",
                    response.statusCode(), response.body());
            throw new SemseaException("ChromaDB heartbeat returned status " + response.statusCode() + ".");
        }
    }

    /**
     * Best-effort delete. Any failure is logged but not surfaced, since this
     * is called as part of cleanup paths (unmount, remove) where the SQLite
     * state has already been removed.
     */
    public void deleteCollection(String collection) {
        String requestUrl = BASE_URL + String.format(DELETE_COLLECTION, collection);
        try {
            HttpResponse<String> response = executeDelete(requestUrl, MetricType.CHROMA_DELETE_COLLECTION);
            if(response.statusCode() != 200) {
                log.warn("ChromaDB delete collection '{}' returned status {}: {}",
                        collection, response.statusCode(), response.body());
            }
        }
        catch (SemseaException e) {
            log.warn("ChromaDB delete collection '{}' failed: {}", collection, e.getMessage());
        }
    }

    public String getOrCreateCollection(String collection) {
        log.debug("Ensuring ChromaDB collection '{}' exists", collection);
        GetOrCreateCollectionRequest body = new GetOrCreateCollectionRequest(collection, false);
        HttpResponse<String> response = executePost(
                BASE_URL + GET_OR_CREATE_COLLECTION,
                objectMapper.writeValueAsString(body),
                MetricType.CHROMA_GET_OR_CREATE_COLLECTION);

        if(response.statusCode() != 200) {
            log.error("ChromaDB getOrCreateCollection failed: status={} body={}",
                    response.statusCode(), response.body());
            throw new SemseaException(
                    "Could not create vector store collection '" + collection + "'.");
        }

        try {
            Map<Object, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            return (String) responseMap.get("id");
        }
        catch (Exception e) {
            log.error("ChromaDB getOrCreateCollection returned malformed response: {}", response.body(), e);
            throw new SemseaException("ChromaDB returned an unexpected response.", e);
        }
    }

    public void saveEmbedding(String collectionId, List<String> ids, List<String> documents, List<List<Float>> embeddings, List<Map<String, Object>> metadatas) {
        log.debug("Calling chromaDB to store {} embeddings.", documents.size());

        SaveEmbeddingRequest body = new SaveEmbeddingRequest(ids, documents, embeddings, metadatas);
        HttpResponse<String> response = executePost(
                BASE_URL + String.format(SAVE_EMBEDDINGS, collectionId),
                objectMapper.writeValueAsString(body),
                MetricType.CHROMA_SAVE_EMBEDDINGS);

        if(response.statusCode() != 201) {
            log.error("ChromaDB saveEmbedding failed: status={} body={}",
                    response.statusCode(), response.body());
            throw new SemseaException("Could not store embeddings in the vector store.");
        }
    }

    public Map<Object,Object> searchEmbeddings(String collectionId, List<List<Float>> embedding, int limit){
        log.debug("Querying ChromaDB collection '{}' for top {} matches", collectionId, limit);
        SearchEmbeddingRequest body = new SearchEmbeddingRequest(embedding, limit);
        HttpResponse<String> response = executePost(
                BASE_URL + String.format(SEARCH_EMBEDDINGS, collectionId),
                objectMapper.writeValueAsString(body),
                MetricType.CHROMA_SEARCH_EMBEDDINGS);

        if(response.statusCode() != 200) {
            log.error("ChromaDB searchEmbeddings failed: status={} body={}",
                    response.statusCode(), response.body());
            throw new SemseaException("Vector store search failed.");
        }

        try {
            return objectMapper.readValue(response.body(), Map.class);
        }
        catch (Exception e) {
            log.error("ChromaDB searchEmbeddings returned malformed response: {}", response.body(), e);
            throw new SemseaException("ChromaDB returned an unexpected response.", e);
        }
    }

    public void deleteEmbeddings(String collectionId, List<String> ids) {
        log.debug("Deleting {} embedding id(s) from ChromaDB collection '{}'",
                ids.size(), collectionId);
        DeleteEmbeddingRequest body = new DeleteEmbeddingRequest(ids);
        HttpResponse<String> response = executePost(
                BASE_URL + String.format(DELETE_EMBEDDINGS, collectionId),
                objectMapper.writeValueAsString(body),
                MetricType.CHROMA_DELETE_EMBEDDINGS);

        if(response.statusCode() != 200) {
            log.error("ChromaDB deleteEmbeddings failed: status={} body={}",
                    response.statusCode(), response.body());
            throw new SemseaException("Could not remove stale embeddings from the vector store.");
        }
    }

    private record GetOrCreateCollectionRequest(String name, boolean get_or_create) {}
    private record SaveEmbeddingRequest(List<String> ids, List<String> documents, List<List<Float>> embeddings, List<Map<String, Object>> metadatas) {}
    private record SearchEmbeddingRequest(List<List<Float>> query_embeddings, int n_results) {}
    private record DeleteEmbeddingRequest(List<String> ids) {}
}
