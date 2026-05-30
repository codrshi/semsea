package org.codrshi.service;

import org.codrshi.api.ChromaClient;
import org.codrshi.config.ConfigManager;
import org.codrshi.repository.DbExecutor;

import java.io.IOException;
import java.util.Map;

public class MountService {

    private final ChromaClient chromaClient;
    private final IOService ioService;

    public MountService() {
        chromaClient = new ChromaClient();
        ioService = new IOService();
    }

    public void unmount(String collection) {
        chromaClient.deleteCollection(collection);
        DbExecutor.deleteWorkspace(collection);
    }

    public void mount(String collection, String path) throws IOException {
        Map.Entry<String, Boolean> response = chromaClient.getOrCreateCollection(collection);

        String collectionId = response.getKey();
        Boolean isMounted = response.getValue();

        ConfigManager.updateWorkspace(collectionId);

        if(!isMounted){
            try {
                ioService.serialize(path);
            }
            catch (Exception e) {
                unmount(collection);
                throw new RuntimeException("Failed to mount.", e);
            }
        }
    }
}
