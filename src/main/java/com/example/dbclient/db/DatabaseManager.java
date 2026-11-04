package com.example.dbclient.db;

import com.example.dbclient.config.DatabaseProfile;
import com.example.dbclient.export.Exporter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager implements AutoCloseable {
    private Connection connection;
    private DatabaseProfile currentProfile;
    private QueryResult lastResult;

    public void connect(DatabaseProfile profile) throws SQLException {
        try {
            close();
        } catch (SQLException e) {
            // Ignore close exceptions
        }

        currentProfile = profile;
        try {
            Class.forName(profile.getType().getDriverClass());
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database driver not found: " + profile.getType().getDriverClass(), e);
        }

        connection = DriverManager.getConnection(
                profile.getJdbcUrl(),
                profile.getUsername(),
                profile.getPassword()
        );
    }

    public QueryResult executeQuery(String sql) throws SQLException {
        long startTime = System.currentTimeMillis();

        if (isSelectStatement(sql)) {
            // Use forward-only, read-only cursor for efficiency
            Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // Use a reasonable fetch size; Integer.MIN_VALUE enables streaming in some PostgreSQL versions
            // but causes errors in others. Using 1000 as a safe default for most use cases.
            try {
                stmt.setFetchSize(1000);
            } catch (SQLException e) {
                // Fallback: try with default fetch size
                stmt.setFetchSize(100);
            }

            try (ResultSet rs = stmt.executeQuery(sql)) {
                List<String> columns = extractColumns(rs);
                List<List<Object>> rows = extractRows(rs);
                long executionTime = System.currentTimeMillis() - startTime;

                lastResult = new QueryResult(columns, rows, executionTime);
            }
        } else {
            try (Statement stmt = connection.createStatement()) {
                int count = stmt.executeUpdate(sql);
                long executionTime = System.currentTimeMillis() - startTime;
                lastResult = new QueryResult(count, executionTime);
            }
        }

        return lastResult;
    }

    private boolean isSelectStatement(String sql) {
        String trimmed = sql.trim().toUpperCase();
        return trimmed.startsWith("SELECT") ||
               trimmed.startsWith("SHOW") ||
               trimmed.startsWith("DESCRIBE") ||
               trimmed.startsWith("EXPLAIN");
    }

    private List<String> extractColumns(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        List<String> columns = new ArrayList<>();

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            columns.add(metaData.getColumnLabel(i));
        }

        return columns;
    }

    private List<List<Object>> extractRows(ResultSet rs) throws SQLException {
        List<List<Object>> rows = new ArrayList<>();

        while (rs.next()) {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
        }

        return rows;
    }

    public QueryResult getLastResult() {
        return lastResult;
    }

    /**
     * 执行查询并直接流式导出到文件，不将全部数据驻留内存。
     * 用于 \export-sql 命令的大数据导出场景。
     *
     * @param sql      查询语句
     * @param exporter 导出器
     * @param path     输出文件路径
     * @return 流式查询结果（仅包含元信息，不含行数据）
     */
    public StreamingQueryResult executeAndExport(String sql, Exporter exporter, java.nio.file.Path path)
            throws SQLException, java.io.IOException {
        ensureConnected();
        long startTime = System.currentTimeMillis();

        try (Statement stmt = connection.createStatement(
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

            try {
                stmt.setFetchSize(1000);
            } catch (SQLException e) {
                stmt.setFetchSize(100);
            }

            try (ResultSet rs = stmt.executeQuery(sql)) {
                List<String> columns = extractColumns(rs);
                long rowCount = exporter.exportFromResultSet(rs, columns, path);
                long elapsed = System.currentTimeMillis() - startTime;
                return new StreamingQueryResult(columns, rowCount, elapsed);
            }
        }
    }

    private void ensureConnected() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("Not connected to a database. Use \\connect first.");
        }
    }

    public DatabaseProfile getCurrentProfile() {
        return currentProfile;
    }

    public boolean isConnected() {
        if (connection == null) {
            return false;
        }
        try {
            return !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}