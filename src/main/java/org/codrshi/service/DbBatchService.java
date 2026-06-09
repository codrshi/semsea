package org.codrshi.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.config.ConfigManager;
import org.codrshi.error.SemseaException;
import org.codrshi.repository.DbExecutor;
import org.codrshi.util.WorkspaceDetails;

import java.util.ArrayList;
import java.util.List;

public class DbBatchService {

    private static final Logger log = LogManager.getLogger(DbBatchService.class);

    private static final int BATCH_SIZE = ConfigManager.getConfig().getSqliteBatchSize();

    private List<List<Object>> filesToInsert;
    private List<List<Object>> filesToDelete;

    private String workspaceId;

    public DbBatchService() {
        filesToInsert = new ArrayList<>();
        filesToDelete = new ArrayList<>();
    }

    public void save(String filePath, List<String> recordIds, long lastModifiedAt, long fileSize) {

        if (filesToInsert.size() >= BATCH_SIZE) {
            saveFlush();
        }

        filesToInsert.add(List.of(String.join( ",", recordIds), filePath, lastModifiedAt, fileSize));

    }

    public void delete(String filePath) {
        if(filesToDelete.size() >= BATCH_SIZE) {
            deleteFlush();
        }

        filesToDelete.add(List.of(filePath));
    }

    public void saveFlush(){
        if(!filesToInsert.isEmpty()) {
            log.debug("Flushing SQLite insert batch: {} row(s)", filesToInsert.size());
            DbExecutor.saveAll(activeWorkspaceId(), filesToInsert);
            filesToInsert = new ArrayList<>();
        }
    }

    public void deleteFlush(){
        if(!filesToDelete.isEmpty()) {
            log.debug("Flushing SQLite delete batch: {} row(s)", filesToDelete.size());
            DbExecutor.deleteAll(activeWorkspaceId(), filesToDelete);
            filesToDelete = new ArrayList<>();
        }
    }

    private String activeWorkspaceId() {
        if(workspaceId == null) {
            WorkspaceDetails active = DbExecutor.getActiveWorkspace();
            if(active == null) {
                throw new SemseaException(
                        "No workspace is currently attached.",
                        "Run 'semsea attach <workspace> --path <dir>' first.");
            }
            workspaceId = active.id();
        }
        return workspaceId;
    }
}
