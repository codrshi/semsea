package org.codrshi.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;

public class IOMountingService extends IOService {

    private static final Logger log = LogManager.getLogger(IOMountingService.class.getName());

    public IOMountingService() {
        super();
    }

    @Override
    protected String getProgressTitle() {
        return "Indexing workspace";
    }

    // TODO: enable backpressure/rate limiting via queue + worker thread
    @Override
    public void processFile(Path projectPath, Path filePath, String fileType) throws IOException {
        log.debug("Processing file {}", filePath.getFileName());

        Path relativePath = Path.of(projectPath.getFileName().toString(), projectPath.relativize(filePath).toString());
        FileTime lastModifiedAt = Files.getLastModifiedTime(filePath);

        Map<String,Object> metadata = Map.of("file-type", fileType, "last-modified-time", lastModifiedAt.toString());

        List<String> recordIds = batchService.saveChunks(Files.readString(filePath), relativePath, metadata);
        dbBatchService.save(relativePath.toString(), recordIds, lastModifiedAt.toMillis(), Files.size(filePath));
    }

    @Override
    public void flush() {
        batchService.llmFlush();
        batchService.saveFlush();
        dbBatchService.saveFlush();
    }
}
