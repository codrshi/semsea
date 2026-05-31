package org.codrshi.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.config.ConfigManager;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public abstract class IOService {
    private static final Logger log = LogManager.getLogger(IOService.class.getName());

    protected final BatchService batchService;
    protected final DbBatchService dbBatchService;

    public IOService() {
        batchService = new BatchService();
        dbBatchService = new DbBatchService();
    }

    public void serialize(String path) throws IOException {
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

        flush();
    }

    private String getFileType(String fileName) {
        return ConfigManager.getConfig().getSupportedFiles()
                .stream().filter(fileName::endsWith)
                .findFirst().orElse(null);
    }

    protected abstract void processFile(Path projectPath, Path filePath, String fileType)  throws IOException;

    protected abstract void flush();
}
