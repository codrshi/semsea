package org.codrshi.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.config.ConfigManager;
import org.codrshi.error.SemseaException;
import org.codrshi.util.SemseaPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DbManager {

    private static final Logger log = LogManager.getLogger(DbManager.class);

    private static final String DB_NAME = ConfigManager.getConfig().getDbName();
    private static final String DB_DIRECTORY = ConfigManager.getConfig().getDbDirectory();
    private static final String DB_URL;
    private static final String CREATE_WORKSPACE_TABLE =
            """
            CREATE TABLE IF NOT EXISTS workspace (
                id TEXT,
                collection_id TEXT UNIQUE NOT NULL,
                last_refresh DATETIME NOT NULL,
                location TEXT UNIQUE NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 0,
            
                PRIMARY KEY(id)
            )
            """;

    private static final String CREATE_IDX_ACTIVE_WORKSPACE =
            """
            CREATE UNIQUE INDEX IF NOT EXISTS uniq_active_workspace
            ON workspace(is_active) WHERE is_active = 1;
            """;

    private static final String CREATE_METADATA_TABLE =
            """
            CREATE TABLE IF NOT EXISTS metadata (
                ids TEXT NOT NULL,
                workspace_id TEXT NOT NULL,
                file_path TEXT NOT NULL,
                last_modified_at BIGINT NOT NULL,
                file_size BIGINT NOT NULL,
            
                CONSTRAINT fk_workspace
                       FOREIGN KEY (workspace_id)
                       REFERENCES workspace(id)
                       ON DELETE CASCADE,
                CONSTRAINT uq_workspace_id_file_path UNIQUE(workspace_id, file_path)
            )
            """;
    private static final String CREATE_IDX_METADATA_workspaceId =
            """
            CREATE INDEX IF NOT EXISTS idx_metadata ON metadata(workspace_id);
            """;

    static {
        try {
            Path dbDirectoryPath = SemseaPaths.home().resolve(DB_DIRECTORY);
            if(!Files.exists(dbDirectoryPath)){
                    Files.createDirectories(dbDirectoryPath);
            }
            DB_URL = ConfigManager.getConfig().getDbUrl() + ":" + dbDirectoryPath.resolve(DB_NAME) + "?foreign_keys=true";
        }
        catch (Exception e) {
            log.error("Failed to prepare local database directory under semsea home", e);
            throw new SemseaException(
                    "Could not prepare the local database directory.",
                    e);
        }
    }

    public static void init() {
        try(Connection connection = getConnection();
            Statement st = connection.createStatement()) {
            st.execute(CREATE_WORKSPACE_TABLE);
            st.execute(CREATE_IDX_ACTIVE_WORKSPACE);
            st.execute(CREATE_METADATA_TABLE);
            st.execute(CREATE_IDX_METADATA_workspaceId);
        }
        catch (Exception e) {
            log.error("Failed to initialize SQLite schema", e);
            throw new SemseaException("Failed to initialize the local database.", e);
        }
        log.info("Local database ready at {}", DB_URL);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
