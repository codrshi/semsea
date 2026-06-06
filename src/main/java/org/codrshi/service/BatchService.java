package org.codrshi.service;

import org.codrshi.api.ChromaClient;
import org.codrshi.api.LLMClient;
import org.codrshi.api.OllamaClient;
import org.codrshi.config.ConfigManager;

import java.nio.file.Path;
import java.util.*;

public class BatchService {

    private static final int BATCH_SIZE = ConfigManager.getConfig().getBatchSize();
    private static final int MAX_LLM_CHARS = ConfigManager.getConfig().getLlmContextLimit();

    private EmbeddingBatchContext embeddingBatchContext;
    private LlmBatchContext llmBatchContext;
    private List<String> embeddingsToDelete;
    private final OllamaClient ollamaClient;
    private final ChromaClient chromaClient;
    private final LLMClient llmClient;

    public BatchService() {
        embeddingsToDelete = new ArrayList<>();
        embeddingBatchContext = new EmbeddingBatchContext();
        llmBatchContext = new LlmBatchContext();
        ollamaClient = new OllamaClient();
        chromaClient = new ChromaClient();
        llmClient = new LLMClient();
    }

    public List<String> saveChunks(String text, Path relativePath, Map<String,Object> metadata) {

        List<String> uuids = new ArrayList<>();

        if(text.length() <= llmBatchContext.availableChars()){
            uuids.add(UUID.randomUUID().toString());
            addToLlmBatch(text,relativePath, uuids.getLast(), metadata);
            return uuids;
        }

        for(int start = 0; start < text.length(); start+=MAX_LLM_CHARS) {
            int end = Math.min(text.length(), start + MAX_LLM_CHARS);
            uuids.add(UUID.randomUUID().toString());
            addToLlmBatch(text.substring(start, end), relativePath, uuids.getLast(), metadata);
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

        if(embeddingBatchContext.count == 0){
            return;
        }

        String collectionId = ConfigManager.getConfig().getCollectionId();

        List<List<Float>> embeddings = ollamaClient.createEmbeddings(embeddingBatchContext.texts);

        chromaClient.saveEmbedding(collectionId, embeddingBatchContext.ids, embeddingBatchContext.documents, embeddings, embeddingBatchContext.metadatas);

        embeddingBatchContext = new EmbeddingBatchContext();
    }

    public void deleteFlush(){
        if(embeddingsToDelete.isEmpty()){
            return;
        }

        chromaClient.deleteEmbeddings(ConfigManager.getConfig().getCollectionId(), embeddingsToDelete);
        embeddingsToDelete = new ArrayList<>();
    }

    public void llmFlush() {
        if(llmBatchContext.texts.isEmpty()){
            return;
        }

        List<String> summaries = llmClient.generateSummary(llmBatchContext.texts, llmBatchContext.relativePath);

        for(int i=0; i<summaries.size(); i++){
            addToEmbeddingBatch(summaries.get(i), llmBatchContext.relativePath.get(i), llmBatchContext.ids.get(i), llmBatchContext.metadatas.get(i));
        }

        llmBatchContext = new LlmBatchContext();
    }

    private void addToLlmBatch(String text, Path relativePath, String uuid, Map<String,Object> metadata) {
        if(llmBatchContext.totalChars + text.length() >= MAX_LLM_CHARS){
            llmFlush();
        }

        llmBatchContext.ids.add(uuid);
        llmBatchContext.texts.add(text);
        llmBatchContext.relativePath.add(relativePath);
        llmBatchContext.metadatas.add(metadata);
        llmBatchContext.totalChars += text.length();
    }

    private void addToEmbeddingBatch(String summary, Path relativePath, String uuid, Map<String,Object> metadata) {
        if(embeddingBatchContext.count == BATCH_SIZE){
            saveFlush();
        }

        embeddingBatchContext.ids.add(uuid);
        embeddingBatchContext.texts.add(summary);
        embeddingBatchContext.documents.add(relativePath.toString());
        embeddingBatchContext.metadatas.add(metadata);
        embeddingBatchContext.count++;
    }

    private static class LlmBatchContext extends BatchContext {
        int totalChars;
        final List<Path> relativePath;

        public LlmBatchContext() {
            super();
            totalChars = 0;
            relativePath =  new ArrayList<>();
        }

        public int availableChars() {
            return MAX_LLM_CHARS - totalChars;
        }
    }

    private static class EmbeddingBatchContext extends BatchContext{
        int count;
        final List<String> documents;

        public EmbeddingBatchContext() {
            super();
            count = 0;
            documents = new ArrayList<>();
        }
    }

    private static class BatchContext {

        final List<String> ids;
        final List<String> texts;
        final List<Map<String, Object>> metadatas;

        public BatchContext() {
            ids = new ArrayList<>();
            texts = new ArrayList<>();
            metadatas = new ArrayList<>();
        }
    }
}
