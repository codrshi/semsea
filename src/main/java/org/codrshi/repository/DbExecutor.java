package org.codrshi.repository;

import org.codrshi.config.ConfigManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class DbExecutor {
    private static final String INSERT_FILES_INTO_METADATA =
            """
            INSERT INTO metadata(workspace, file_path, last_modified_at, file_size)
            VALUES(?, ?, ?, ?);
            """;

    private static final String UPDATE_FILES_IN_METADATA =
            """
            UPDATE metadata
            SET
                last_modified_at = ?, file_size = ?
            WHERE workspace = ? AND file_path = ?;
            """;

    private static final String DELETE_FILES_FROM_METADATA =
            """
            DELETE FROM metadata
            WHERE workspace = ? AND file_path = ?;
            """;

    private static final String DELETE_WORKSPACE_FROM_METADATA =
            """
            DELETE FROM metadata
            WHERE workspace = ?;
            """;

    // column order in list: filePath, lastModifiedAt, fileSize
    public static void saveAll(List<List<Object>> list){

        executeBatch(INSERT_FILES_INTO_METADATA, list, (PreparedStatement ps, List<Object> row) -> {
            ps.setString(1, ConfigManager.getConfig().getWorkspace());
            ps.setString(2, row.get(0).toString());
            ps.setLong(3, (Long) row.get(1));
            ps.setLong(4, (Long) row.get(2));

            ps.addBatch();
        });
    }

    // column order in list: filePath, lastModifiedAt, fileSize
    public static void updateAll(List<List<Object>> list){

        executeBatch(UPDATE_FILES_IN_METADATA, list, (PreparedStatement ps, List<Object> row) -> {
            ps.setLong(1, (Long) row.get(1));
            ps.setLong(2, (Long) row.get(2));
            ps.setString(3, ConfigManager.getConfig().getWorkspace());
            ps.setString(4, row.get(0).toString());

            ps.addBatch();
        });
    }

    // column order in list: filePath
    public static void deleteAll(List<List<Object>> list){
        executeBatch(DELETE_FILES_FROM_METADATA, list, (PreparedStatement ps, List<Object> row) -> {
            ps.setString(1, ConfigManager.getConfig().getWorkspace());
            ps.setString(2, row.getFirst().toString());
        });
    }

    public static void deleteWorkspace(String workspace){
        try (
                Connection connection = DbManager.getConnection();
                PreparedStatement ps = connection.prepareStatement(DELETE_WORKSPACE_FROM_METADATA);
        ) {
            connection.setAutoCommit(false);

            ps.setString(1, workspace);
            try {
                ps.executeUpdate();
                connection.commit();
            }
            catch(Exception e) {
                connection.rollback();
                throw e;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void executeBatch(String sql, List<List<Object>> list, BatchAdder batchAdder){
        try (
                Connection connection = DbManager.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)
        ) {
            connection.setAutoCommit(false);
            for(List<Object> row : list){
                batchAdder.add(ps, row);
            }

            try {
                ps.executeBatch();
                connection.commit();
            }
            catch(Exception e) {
                connection.rollback();
                throw e;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private interface BatchAdder {
        void add(PreparedStatement ps, List<Object> row) throws SQLException;
    }
}
