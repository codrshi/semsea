package org.codrshi.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.api.ChromaClient;
import org.codrshi.config.ConfigManager;
import org.codrshi.error.SemseaException;
import org.codrshi.repository.DbExecutor;
import org.codrshi.util.TerminalRenderer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class MountService {

    private static final Logger log = LogManager.getLogger(MountService.class);

    private final ChromaClient chromaClient;
    private final IOService ioMountingService;

    public MountService() {
        chromaClient = new ChromaClient();
        ioMountingService = new IOMountingService();
    }

    public void unmount(String path) {
        String absolutePath = resolveAbsolutePath(path);
        log.info("Unmounting any existing workspace at '{}'", absolutePath);

        Optional.ofNullable(DbExecutor.deleteWorkspaceByLocation(absolutePath))
                .ifPresent(chromaClient::deleteCollection);
        ConfigManager.updateWorkspace(null, null);
        log.info("Unmount complete for '{}'", absolutePath);
    }

    public void mount(String workspace, String path) {
        String absolutePath = resolveAbsolutePath(path);
        log.info("Mounting workspace '{}' at '{}'", workspace, absolutePath);

        List<Object> isPresent = DbExecutor.exists(workspace, absolutePath);

        if(isPresent.get(0)!=null){
            log.info("Workspace '{}' already attached at '{}'; only refreshing active pointer",
                    workspace, absolutePath);
            ConfigManager.updateWorkspace(workspace,(String) isPresent.get(0));
            TerminalRenderer.println("  %s workspace %s is already attached at %s",
                    TerminalRenderer.green("+"),
                    TerminalRenderer.bold(workspace),
                    TerminalRenderer.dim(absolutePath));
            return;
        }

        if((boolean) isPresent.get(1)){
            log.warn("Mount aborted: workspace '{}' or path '{}' already registered separately",
                    workspace, absolutePath);
            TerminalRenderer.println("  %s workspace %s or path %s is already registered.",
                    TerminalRenderer.red("x"),
                    TerminalRenderer.bold(workspace),
                    TerminalRenderer.dim(absolutePath));
            return;
        }

        try {
            String collectionId = chromaClient.getOrCreateCollection(workspace);
            log.debug("Created vector store collection '{}' (id={})", workspace, collectionId);

            DbExecutor.saveWorkspace(workspace, absolutePath, collectionId);
            ConfigManager.updateWorkspace(workspace, collectionId);

            ioMountingService.serialize(path);
            log.info("Mount complete for workspace '{}'", workspace);
        }
        catch (SemseaException e) {
            log.error("Mount failed for workspace '{}', cleaning up", workspace, e);
            safelyUnmount(path);
            throw e;
        }
        catch (RuntimeException e) {
            log.error("Mount failed unexpectedly for workspace '{}', cleaning up", workspace, e);
            safelyUnmount(path);
            throw new SemseaException("Failed to attach workspace '" + workspace + "'.", e);
        }
    }

    public static String resolveAbsolutePath(String path) {
        try {
            return Path.of(path).toRealPath().toString();
        }
        catch (IOException e) {
            log.error("Could not resolve path '{}'", path, e);
            throw new SemseaException(
                    "Could not resolve path '" + (path == null || path.isEmpty() ? "<current directory>" : path) + "'.",
                    "Check that the directory exists and you have permission to read it.",
                    e);
        }
    }

    private void safelyUnmount(String path) {
        try {
            unmount(path);
        }
        catch (Exception cleanupErr) {
            log.warn("Cleanup after failed mount also failed for path '{}'", path, cleanupErr);
        }
    }
}
