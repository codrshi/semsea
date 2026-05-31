package org.codrshi.service;

import org.codrshi.api.ChromaClient;
import org.codrshi.api.LLMClient;
import org.codrshi.api.OllamaClient;
import org.codrshi.config.ConfigManager;

import java.nio.file.Path;
import java.util.*;

public class BatchService {

    private static final int BATCH_SIZE = ConfigManager.getConfig().getBatchSize();
    private static final int MAX_CHARS = ConfigManager.getConfig().getMaxChunkSize();

    private BatchContext batchContext;
    private List<String> embeddingsToDelete;
    private final OllamaClient ollamaClient;
    private final ChromaClient chromaClient;
    private final LLMClient llmClient;

    public BatchService() {
        embeddingsToDelete = new ArrayList<>();
        batchContext = new BatchContext();
        ollamaClient = new OllamaClient();
        chromaClient = new ChromaClient();
        llmClient = new LLMClient();
    }

    public List<String> saveChunks(String text, Path relativePath, Map<String,Object> metadata) {

        List<String> uuids = new ArrayList<>();

        if(text.length() < MAX_CHARS){
            uuids.add(UUID.randomUUID().toString());
            add(text,relativePath, uuids.getLast(), metadata);
            return uuids;
        }

        for(int start = 0; start < text.length(); start+=MAX_CHARS) {
            int end = Math.min(text.length(), start + MAX_CHARS);
            uuids.add(UUID.randomUUID().toString());
            add(text.substring(start, end), relativePath, uuids.getLast(), metadata);
        }

        return uuids;
    }

    public void deleteChunks(List<String> ids) {
        for(String id : ids){
            if(embeddingsToDelete.size() >= BATCH_SIZE){
                deleteFlush();
            }

            embeddingsToDelete.add(id);
        }
    }

    public void saveFlush(){

        if(batchContext.position == 0){
            return;
        }

        String collectionId = ConfigManager.getConfig().getCollectionId();

        List<List<Float>> embeddings = ollamaClient.createEmbeddings(batchContext.texts);

        chromaClient.saveEmbedding(collectionId, batchContext.ids, batchContext.documents, embeddings, batchContext.metadatas);

        batchContext = new BatchContext();
    }

    public void deleteFlush(){
        if(embeddingsToDelete.isEmpty()){
            return;
        }

        chromaClient.deleteEmbeddings(ConfigManager.getConfig().getCollectionId(), embeddingsToDelete);
        embeddingsToDelete = new ArrayList<>();
    }

    private void add(String text, Path relativePath, String uuid, Map<String,Object> metadata) {
        if(batchContext.position == BATCH_SIZE){
            saveFlush();
        }

        batchContext.ids.add(uuid);
        batchContext.texts.add(llmClient.generateSummary(text, relativePath.getFileName().toString()));
        batchContext.documents.add(relativePath.toString());
        batchContext.metadatas.add(metadata);
        batchContext.position++;
    }

    private static class BatchContext {
        int position;
        final List<String> ids;
        final List<String> texts;
        final List<String> documents;
        final List<Map<String, Object>> metadatas;

        public BatchContext() {
            position = 0;
            ids = new ArrayList<>();
            documents = new ArrayList<>();
            texts = new ArrayList<>();
            metadatas = new ArrayList<>();
        }
    }
}
