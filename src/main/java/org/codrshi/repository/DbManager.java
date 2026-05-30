package org.codrshi.repository;

import org.codrshi.config.ConfigManager;
import org.codrshi.config.SemseaConfig;

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
    private static final String CREATE_METADATA_TABLE =
            """
            CREATE TABLE IF NOT EXISTS metadata (
                workspace TEXT NOT NULL,
                file_path TEXT NOT NULL,
                last_modified_at BIGINT NOT NULL,
                file_size BIGINT NOT NULL,
            
                PRIMARY KEY(workspace, file_path)
            )
            """;
    private static final String CREATE_IDX_WORKSPACE =
            """
            CREATE INDEX IF NOT EXISTS idx_workspace ON metadata(workspace);
            """;

    static {
        try {
            Path dbDirectoryPath = Paths.get(DB_DIRECTORY);
            if(!Files.exists(dbDirectoryPath)){
                    Files.createDirectories(dbDirectoryPath);
            }

            DB_URL = ConfigManager.getConfig().getDbUrl() + ":" + dbDirectoryPath.resolve(DB_NAME);
        }
        catch (Exception e){
            throw new RuntimeException("Failed to initialize SQLite directory", e);
        }
    }

    public static void init(){
        try(Statement st = getConnection().createStatement()) {
            st.execute(CREATE_METADATA_TABLE);
            st.execute(CREATE_IDX_WORKSPACE);
        }
        catch (Exception e){
            throw new RuntimeException("Failed to initialize metadata table", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
