package org.codrshi.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChromaClient extends Client {
    private static final Logger logger = LogManager.getLogger(ChromaClient.class.getName());

    private static final String BASE_URL = "http://localhost:8000/api/v2";
    private static final String DELETE_COLLECTION = "/tenants/default_tenant/databases/default_database/collections/%s";
    private static final String GET_OR_CREATE_COLLECTION = "/tenants/default_tenant/databases/default_database/collections";
    private static final String SAVE_EMBEDDINGS = "/tenants/default_tenant/databases/default_database/collections/%s/add";
    private static final String SEARCH_EMBEDDINGS = "/tenants/default_tenant/databases/default_database/collections/%s/query";

    private final ObjectMapper objectMapper;

    public ChromaClient() {
        super();
        objectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public void deleteCollection(String collection) {
        String requestUrl = BASE_URL + String.format(DELETE_COLLECTION, collection);
        HttpResponse<String> response = executeDelete(requestUrl);

        if(response.statusCode() != 200) {
            logger.warn("Failed to delete collection: {}", response);
        }
    }

    public Map.Entry<String, Boolean> getOrCreateCollection(String collection) {
        GetOrCreateCollectionRequest getOrCreateCollectionRequest = new GetOrCreateCollectionRequest(collection, true);
        String requestBody = objectMapper.writeValueAsString(getOrCreateCollectionRequest);
        String requestUrl = BASE_URL + GET_OR_CREATE_COLLECTION;

        HttpResponse<String> response = executePost(requestUrl, requestBody);

        if(response.statusCode() != 200) {
            logger.error("Failed to get/create collection: {}", response);
            throw new RuntimeException("Failed to get/create collection: " + response.body());
        }

        Map<Object, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
        String collectionId = (String) responseMap.get("id");
        boolean isNew = responseMap.get("dimension") != null;

        logger.info("attached to collection = {} with id = {}", collection, collectionId);

        return Map.entry(collectionId, isNew);
    }

    public void saveEmbedding(String collectionId, List<UUID> ids, List<String> documents, List<List<Float>> embeddings, List<Map<String, Object>> metadatas) {
        logger.debug("Calling chromaDB to store {} embeddings.", documents.size());

        SaveEmbeddingRequest saveEmbeddingRequest = new SaveEmbeddingRequest(ids, documents, embeddings, metadatas);
        String requestBody = objectMapper.writeValueAsString(saveEmbeddingRequest);
        String requestUrl = BASE_URL + String.format(SAVE_EMBEDDINGS, collectionId);

        HttpResponse<String> response = executePost(requestUrl, requestBody);

        if(response.statusCode() != 201) {
            logger.error("Failed to save embeddings: {}", response);
            throw new RuntimeException("Failed to save embeddings: " + response.body());
        }

        logger.debug("Saved embeddings with ids {}", ids);
    }

    public Map<Object,Object> searchEmbeddings(String collectionId, List<List<Float>> embedding, int limit){
        SearchEmbeddingRequest searchEmbeddingRequest = new SearchEmbeddingRequest(embedding, limit);
        String requestBody = objectMapper.writeValueAsString(searchEmbeddingRequest);
        String requestUrl = BASE_URL + String.format(SEARCH_EMBEDDINGS, collectionId);

        HttpResponse<String> response = executePost(requestUrl, requestBody);

        if(response.statusCode() != 200) {
            logger.error("Failed to search embeddings: {}", response);
            throw new RuntimeException("Failed to search embeddings: " + response.body());
        }

        return objectMapper.readValue(response.body(), Map.class);
    }

    private record GetOrCreateCollectionRequest(String name, boolean get_or_create) {}
    private record SaveEmbeddingRequest(List<UUID> ids, List<String> documents, List<List<Float>> embeddings, List<Map<String, Object>> metadatas) {}
    private record SearchEmbeddingRequest(List<List<Float>> query_embeddings, int n_results) {}
}
