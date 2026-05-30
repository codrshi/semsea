package org.codrshi.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.config.ConfigManager;
import org.codrshi.util.TerminalRenderer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

//TODO: update whenever file is changed.
public class IOService {

    private static final Logger log = LogManager.getLogger(IOService.class.getName());

    private final BatchService batchService;
    private TerminalRenderer terminalRenderer;

    public IOService() {
        batchService = new BatchService();
    }

    public void serialize(String path) throws IOException {
        terminalRenderer = TerminalRenderer.get();
        Path projectPath = Path.of(path);

        log.debug("Resolved path from {} to {}", projectPath.toString(), projectPath.toRealPath());

        //TODO: display in UI which file is getting processed
        Files.walkFileTree(projectPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Objects.requireNonNull(dir);
                if(ConfigManager.getConfig().getIgnoredDirectories().contains(dir.getFileName().toString())){
                    log.debug("Ignoring directory {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(!attrs.isRegularFile() || ConfigManager.getConfig().getIgnoredFiles().contains(file.getFileName().toString())) {
                    log.debug("Ignoring file {}", file);
                    return FileVisitResult.CONTINUE;
                }

                String fileType = getFileType(file.getFileName().toString());
                if(fileType==null) {
                    log.debug("Unsupported file type {}", file);
                    return FileVisitResult.CONTINUE;
                }

                processFile(projectPath, file, fileType);
                return FileVisitResult.CONTINUE;
            }
        });

        batchService.flush();
    }

    // TODO: enable backpressure/rate limiting via queue + worker thread
    private void processFile(Path projectPath, Path filePath, String fileType) throws IOException {
        log.debug("Processing file {}", filePath.getFileName());
        terminalRenderer.print("⏳ processing %s...", filePath.getFileName().toString());
        long start = System.currentTimeMillis();

        //List<String> textChunks = ChunkUtil.getChunks(Files.readAllLines(filePath));
        Path relativePath = Path.of(projectPath.getFileName().toString(), projectPath.relativize(filePath).toString());

        Map<String,Object> metadata = Map.of("file-type", fileType, "last-modified-time", Files.getLastModifiedTime(filePath).toString());
        batchService.addChunks(Files.readString(filePath), relativePath, metadata);

        terminalRenderer.print("\r\033[K✅ %s mounted (%.1fs)%n", filePath.getFileName().toString(), (System.currentTimeMillis() - start)/1000f);
    }

    private String getFileType(String fileName) {
        return ConfigManager.getConfig().getSupportedFiles()
                .stream().filter(fileName::endsWith)
                .findFirst().orElse(null);
    }
}
