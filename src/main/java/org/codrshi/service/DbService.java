package org.codrshi.service;

import org.codrshi.config.ConfigManager;
import org.codrshi.repository.DbExecutor;

import java.util.ArrayList;
import java.util.List;

public class DbService {
    private static final int BATCH_SIZE = ConfigManager.getConfig().getSqliteBatchSize();

    private List<List<Object>> filesToInsert;
    private List<List<Object>> filesToUpdate;
    private List<List<Object>> filesToDelete;

    public DbService() {
        filesToInsert = new ArrayList<>();
        filesToUpdate = new ArrayList<>();
        filesToDelete = new ArrayList<>();
    }

    public void save(String filePath, long lastModifiedAt, long fileSize) {
        if(filesToInsert.size() >= BATCH_SIZE) {
            saveFlush();
        }

        filesToInsert.add(List.of(filePath, lastModifiedAt, fileSize));
    }

    public void update(String filePath, long lastModifiedAt, long fileSize) {
        if(filesToUpdate.size() >= BATCH_SIZE) {
            updateFlush();
        }

        filesToUpdate.add(List.of(filePath, lastModifiedAt, fileSize));
    }

    public void delete(String filePath) {
        if(filesToDelete.size() >= BATCH_SIZE) {
            deleteFlush();
        }

        filesToDelete.add(List.of(filePath));
    }

    public void saveFlush(){
        if(!filesToInsert.isEmpty()) {
            DbExecutor.saveAll(filesToInsert);
            filesToInsert = new ArrayList<>();
        }
    }

    public void updateFlush(){
        if(!filesToUpdate.isEmpty()) {
            DbExecutor.updateAll(filesToUpdate);
            filesToUpdate = new ArrayList<>();
        }
    }

    public void deleteFlush(){
        if(!filesToDelete.isEmpty()) {
            DbExecutor.deleteAll(filesToDelete);
            filesToDelete = new ArrayList<>();
        }
    }
}
