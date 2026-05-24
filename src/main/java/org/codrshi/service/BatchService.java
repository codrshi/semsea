package org.codrshi.service;

import org.codrshi.api.ChromaClient;
import org.codrshi.api.LLMClient;
import org.codrshi.api.OllamaClient;
import org.codrshi.config.ConfigManager;
import org.codrshi.util.UUIDGenerator;

import java.nio.file.Path;
import java.util.*;

public class BatchService {

    private static final int BATCH_SIZE = ConfigManager.getConfig().getBatchSize();

    private BatchContext batchContext;
    private final OllamaClient ollamaClient;
    private final ChromaClient chromaClient;
    private final LLMClient llmClient;

    public BatchService() {
        batchContext = new BatchContext();
        ollamaClient = new OllamaClient();
        chromaClient = new ChromaClient();
        llmClient = new LLMClient();
    }

    public void addChunks(String text, Path relativePath, Map<String,Object> metadata) {

        if(batchContext.position == BATCH_SIZE){
            flush();
        }

        batchContext.texts.add(llmClient.generateSummary(text, relativePath.getFileName().toString()));
        batchContext.documents.add(relativePath.toString());
        batchContext.metadatas.add(metadata);
        batchContext.position++;
    }

    public void flush(){

        if(batchContext.position == 0){
            return;
        }

        String collectionId = ConfigManager.getConfig().getWorkspace();
        List<UUID> uuids = UUIDGenerator.get(batchContext.position);

        List<List<Float>> embeddings = ollamaClient.createEmbeddings(batchContext.texts);

        chromaClient.saveEmbedding(collectionId, uuids, batchContext.documents, embeddings, batchContext.metadatas);

        batchContext = new BatchContext();
    }

    private static class BatchContext {
        int position;
        final List<String> texts;
        final List<String> documents;
        final List<Map<String, Object>> metadatas;

        public BatchContext() {
            position = 0;
            documents = new ArrayList<>();
            texts = new ArrayList<>();
            metadatas = new ArrayList<>();
        }
    }
}
