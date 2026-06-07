package org.codrshi.service;

import org.codrshi.api.ChromaClient;
import org.codrshi.config.ConfigManager;
import org.codrshi.repository.DbExecutor;
import org.codrshi.util.TerminalRenderer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class MountService {

    private final ChromaClient chromaClient;
    private final IOService ioMountingService;

    public MountService() {
        chromaClient = new ChromaClient();
        ioMountingService = new IOMountingService();
    }

    public void unmount(String path) {
        try {
            path = Path.of(path).toRealPath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve absolute path", e);
        }

        Optional.ofNullable(DbExecutor.deleteWorkspaceByLocation(path))
                .ifPresent(chromaClient::deleteCollection);
        ConfigManager.updateWorkspace(null, null);
    }

    public void mount(String workspace, String path) throws IOException {
        String absolutePath = Path.of(path).toRealPath().toString();
        List<Object> isPresent = DbExecutor.exists(workspace, absolutePath);

        if(isPresent.get(0)!=null){
            ConfigManager.updateWorkspace(workspace,(String) isPresent.get(0));
            TerminalRenderer.println("  %s workspace %s is already attached at %s",
                    TerminalRenderer.green("+"),
                    TerminalRenderer.bold(workspace),
                    TerminalRenderer.dim(absolutePath));
            return;
        }

        if((boolean) isPresent.get(1)){
            TerminalRenderer.println("  %s workspace %s or path %s is already registered.",
                    TerminalRenderer.red("x"),
                    TerminalRenderer.bold(workspace),
                    TerminalRenderer.dim(absolutePath));
            return;
        }

        try {
            String collectionId = chromaClient.getOrCreateCollection(workspace);
            DbExecutor.saveWorkspace(workspace, absolutePath,  collectionId);
            ConfigManager.updateWorkspace(workspace, collectionId);

            ioMountingService.serialize(path);
        }
        catch (Exception e) {
            unmount(path);
            throw new RuntimeException("Failed to mount.", e);
        }
    }
}
