package org.codrshi.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.util.TerminalRenderer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;

public class IOMountingService extends IOService {

    private static final Logger log = LogManager.getLogger(IOMountingService.class.getName());

    public IOMountingService() {
        super();
    }

    // TODO: enable backpressure/rate limiting via queue + worker thread
    @Override
    public void processFile(Path projectPath, Path filePath, String fileType) throws IOException {
        log.debug("Processing file {}", filePath.getFileName());
        TerminalRenderer.print("⏳ processing %s...", filePath.getFileName().toString());
        long start = System.currentTimeMillis();

        Path relativePath = Path.of(projectPath.getFileName().toString(), projectPath.relativize(filePath).toString());
        FileTime lastModifiedAt = Files.getLastModifiedTime(filePath);

        Map<String,Object> metadata = Map.of("file-type", fileType, "last-modified-time", lastModifiedAt.toString());

        List<String> recordIds = batchService.saveChunks(Files.readString(filePath), relativePath, metadata);
        dbBatchService.save(relativePath.toString(), recordIds, lastModifiedAt.toMillis(), Files.size(filePath));

        TerminalRenderer.print("\r\033[K✅ %s mounted (%.1fs)%n", filePath.getFileName().toString(), (System.currentTimeMillis() - start)/1000f);
    }

    @Override
    public void flush() {
        batchService.saveFlush();
        dbBatchService.saveFlush();
    }
}
