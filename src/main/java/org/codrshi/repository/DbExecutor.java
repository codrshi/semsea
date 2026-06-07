package org.codrshi.repository;

import org.codrshi.config.ConfigManager;
import org.codrshi.metric.MetricCollector;
import org.codrshi.metric.MetricType;
import org.codrshi.metric.Timer;
import org.codrshi.util.MetadataHolder;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DbExecutor {
    private static final String INSERT_FILES_INTO_METADATA =
            """
            INSERT OR REPLACE INTO metadata(ids, workspace_id, file_path, last_modified_at, file_size)
            VALUES(?, ?, ?, ?, ?);
            """;

    private static final String DELETE_FILES_FROM_METADATA =
            """
            DELETE FROM metadata
            WHERE workspace_id = ? AND file_path = ?;
            """;

    private static final String DELETE_COLLECTION_FROM_WORKSPACE_USING_LOCATION =
            """
            DELETE FROM workspace
            WHERE location = ?
            RETURNING id;
            """;

    private static final String DELETE_COLLECTION_FROM_WORKSPACE_USING_ID =
            """
            DELETE FROM workspace
            WHERE id = ?;
            """;

    private static final String CHECK_WORKSPACE_OR_PATH_EXIST =
            """
            SELECT id, location, collection_id FROM workspace
            WHERE id = ? OR location = ?;
            """;

    private static final String INSERT_INTO_WORKSPACE =
            """
            INSERT INTO workspace(id, location, collection_id, last_refresh)
            VALUES(?, ?, ?, ?);
            """;

    private static final String LOAD_ALL_METADATA =
            """
            SELECT ids, file_path, last_modified_at, file_size FROM metadata
            WHERE workspace_id = ?;
            """;

    private static final String GET_WORKSPACE_LOCATION =
            """
            SELECT location FROM workspace
            WHERE id = ?;
            """;

    // column order in list: id, filePath, lastModifiedAt, fileSize
    public static void saveAll(List<List<Object>> list){

        executeBatch(INSERT_FILES_INTO_METADATA, list, (PreparedStatement ps, List<Object> row) -> {
            ps.setString(1, row.get(0).toString());
            ps.setString(2, ConfigManager.getConfig().getWorkspace());
            ps.setString(3, row.get(1).toString());
            ps.setLong(4, (Long) row.get(2));
            ps.setLong(5, (Long) row.get(3));

            ps.addBatch();
        });
    }

    // column order in list: filePath
    public static void deleteAll(List<List<Object>> list){
        executeBatch(DELETE_FILES_FROM_METADATA, list, (PreparedStatement ps, List<Object> row) -> {
            ps.setString(1, ConfigManager.getConfig().getWorkspace());
            ps.setString(2, row.getFirst().toString());

            ps.addBatch();
        });
    }

    public static String deleteWorkspaceByLocation(String path){

        String workspace = null;
        long startNanos = Timer.start();

        try (
                Connection connection = DbManager.getConnection();
                PreparedStatement ps = connection.prepareStatement(DELETE_COLLECTION_FROM_WORKSPACE_USING_LOCATION)
        ) {

            ps.setString(1, path);

            try(ResultSet resultSet = ps.executeQuery()){
                if(resultSet.next()){
                    workspace = resultSet.getString("id");
                }
            }
            catch(Exception e) {
                throw e;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        MetricCollector.record(MetricType.SQLITE_QUERY, Timer.stop(startNanos));
        return workspace;
    }

    public static boolean deleteWorkspaceByID(String id){
        boolean isDeleted;
        long startNanos = Timer.start();

        try (
                Connection connection = DbManager.getConnection();
                PreparedStatement ps = connection.prepareStatement(DELETE_COLLECTION_FROM_WORKSPACE_USING_ID)
        ) {

            ps.setString(1, id);
            isDeleted = ps.executeUpdate() == 1;
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        MetricCollector.record(MetricType.SQLITE_QUERY, Timer.stop(startNanos));
        return isDeleted;
    }
    public static List<Object> exists(String workspace, String location){

        String collectionId = null;
        boolean isSinglePresent = false;
        long startNanos = Timer.start();

        try (
                Connection connection = DbManager.getConnection();
                PreparedStatement ps = connection.prepareStatement(CHECK_WORKSPACE_OR_PATH_EXIST)
        ) {

            ps.setString(1, workspace);
            ps.setString(2, location);

            try(ResultSet resultSet = ps.executeQuery()){
                while(resultSet.next()){
                    String locationFromDb = resultSet.getString("location");
                    String workspaceFromDb =  resultSet.getString("id");

                    if(workspaceFromDb.equals(workspace) && locationFromDb.equals(location)){
                        collectionId = resultSet.getString("collection_id");
                    }
                    else if(workspaceFromDb.equals(workspace) || locationFromDb.equals(location)){
                        isSinglePresent = true;
                    }
                }
            }
            catch(Exception e) {
                throw e;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        MetricCollector.record(MetricType.SQLITE_QUERY, Timer.stop(startNanos));
        return Arrays.asList(collectionId, isSinglePresent);
    }

    public static void saveWorkspace(String workspace, String location, String collectionId){
        long startNanos = Timer.start();

        try (
                Connection connection = DbManager.getConnection();
                PreparedStatement ps = connection.prepareStatement(INSERT_INTO_WORKSPACE)
        ) {
            ps.setString(1, workspace);
            ps.setString(2, location);
            ps.setString(3, collectionId);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        MetricCollector.record(MetricType.SQLITE_QUERY, Timer.stop(startNanos));
    }

    public static Map<String, MetadataHolder> loadMetadata(String workspace){
        Map<String, MetadataHolder> map = new ConcurrentHashMap<>();
        long startNanos = Timer.start();

        try (
                Connection connection = DbManager.getConnection();
                PreparedStatement ps = connection.prepareStatement(LOAD_ALL_METADATA)
        ) {
            ps.setString(1, workspace);
            try(ResultSet resultSet = ps.executeQuery()){
                while(resultSet.next()){
                    List<String> ids = Arrays.asList(resultSet.getString("ids").split(","));
                    String filePath = resultSet.getString("file_path");
                    long lastModifiedAt =  resultSet.getLong("last_modified_at");
                    long fileSize = resultSet.getLong("file_size");

                    map.put(filePath, new MetadataHolder(ids, lastModifiedAt, fileSize));
                }
            }
            catch(Exception e) {
                throw e;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        MetricCollector.record(MetricType.SQLITE_QUERY, Timer.stop(startNanos));
        return map;
    }

    public static String getWorkspaceLocation(String workspace){
        String location = null;
        long startNanos = Timer.start();

        try (
                Connection connection = DbManager.getConnection();
                PreparedStatement ps = connection.prepareStatement(GET_WORKSPACE_LOCATION)
        ) {

            ps.setString(1, workspace);

            try(ResultSet resultSet = ps.executeQuery()){
                if(resultSet.next()){
                    location = resultSet.getString("location");
                }
            }
            catch(Exception e) {
                throw e;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        MetricCollector.record(MetricType.SQLITE_QUERY, Timer.stop(startNanos));
        return location;
    }

    private static void executeBatch(String sql, List<List<Object>> list, BatchAdder batchAdder){
        long startNanos = Timer.start();

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

        MetricCollector.record(MetricType.SQLITE_QUERY, Timer.stop(startNanos));
    }

    private interface BatchAdder {
        void add(PreparedStatement ps, List<Object> row) throws SQLException;
    }
}
