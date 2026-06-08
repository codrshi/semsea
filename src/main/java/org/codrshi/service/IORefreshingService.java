package org.codrshi.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.util.MetadataHolder;
import org.codrshi.util.ProgressRenderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

public class IORefreshingService extends IOService{
    private static final Logger log = LogManager.getLogger(IORefreshingService.class.getName());

    private final Map<String, MetadataHolder> metadataHolderMap;

    public IORefreshingService(Map<String, MetadataHolder> metadataHolderMap) {
        super();
        this.metadataHolderMap = metadataHolderMap;
    }

    @Override
    protected String getProgressTitle() {
        return "Refreshing workspace";
    }

    @Override
    public void processFile(Path projectPath, Path filePath, String fileType) throws IOException {
        log.debug("Processing file {}", filePath.getFileName());

        Path relativePath = Path.of(projectPath.getFileName().toString(), projectPath.relativize(filePath).toString());
        FileTime lastModifiedAt = Files.getLastModifiedTime(filePath);
        long fileSize = Files.size(filePath);
        Map<String,Object> metadata = Map.of("file-type", fileType, "last-modified-time", lastModifiedAt.toString());

        if(isNewFile(relativePath.toString())){
            List<String> recordIds = batchService.saveChunks(Files.readString(filePath), relativePath, metadata);
            dbBatchService.save(relativePath.toString(), recordIds, lastModifiedAt.toMillis(), Files.size(filePath));
        }
        else if(isModifiedFile(relativePath.toString(), lastModifiedAt, fileSize)){
            MetadataHolder metadataHolder = metadataHolderMap.get(relativePath.toString());

            batchService.deleteChunks(metadataHolder.ids());
            List<String> recordIds = batchService.saveChunks(Files.readString(filePath), relativePath, metadata);
            dbBatchService.save(relativePath.toString(), recordIds, lastModifiedAt.toMillis(), Files.size(filePath));
        }

        metadataHolderMap.remove(relativePath.toString());
    }

    @Override
    public void flush() {
        deleteObsoleteFiles();

        batchService.deleteFlush();
        dbBatchService.deleteFlush();

        batchService.llmFlush();
        batchService.saveFlush();
        dbBatchService.saveFlush();
    }

    private void deleteObsoleteFiles(){
        if(metadataHolderMap.isEmpty()){
            return;
        }

        metadataHolderMap.forEach((key, value) -> {
            ProgressRenderer.get().noteRemoval(key);
            batchService.deleteChunks(value.ids());
            dbBatchService.delete(key);
        });
    }

    private boolean isNewFile(String relativePath){
        return !metadataHolderMap.containsKey(relativePath);
    }

    private boolean isModifiedFile(String relativePath, FileTime lastModifiedAt, long fileSize){
        if(metadataHolderMap.containsKey(relativePath)) {
            MetadataHolder metadataHolder = metadataHolderMap.get(relativePath);

            return lastModifiedAt.toMillis() != metadataHolder.lastModifiedAt() && fileSize != metadataHolder.fileSize();
        }

        return false;
    }
}
