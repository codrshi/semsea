package org.codrshi.repository;

import org.codrshi.config.ConfigManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DbManager {
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
            
                PRIMARY KEY(id)
            )
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
            Path dbDirectoryPath = Paths.get(DB_DIRECTORY);
            if(!Files.exists(dbDirectoryPath)){
                    Files.createDirectories(dbDirectoryPath);
            }

            DB_URL = ConfigManager.getConfig().getDbUrl() + ":" + dbDirectoryPath.resolve(DB_NAME) + "?foreign_keys=true";
        }
        catch (Exception e){
            throw new RuntimeException("Failed to initialize SQLite directory", e);
        }
    }

    public static void init(){
        try(Statement st = getConnection().createStatement()) {
            st.execute(CREATE_WORKSPACE_TABLE);
            st.execute(CREATE_METADATA_TABLE);
            st.execute(CREATE_IDX_METADATA_workspaceId);
        }
        catch (Exception e){
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
