package com.soenluzy.dbclient.db;

import com.soenluzy.dbclient.config.DatabaseProfile;
import com.soenluzy.dbclient.export.Exporter;

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
        // PostgreSQL 驱动默认 autoCommit=false，不显式打开的话 DDL 和不带 COMMIT 的 DML
        // 会在连接关闭时被回滚，同时长期占着锁。
        connection.setAutoCommit(true);
    }

    public QueryResult executeQuery(String sql) throws SQLException {
        ensureConnected();
        long startTime = System.currentTimeMillis();

        if (isResultSetStatement(sql)) {
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
                // execute() 同时适用于 DDL、DML 以及带 RETURNING 子句的语句，比 executeUpdate() 稳妥
                boolean hasResultSet = stmt.execute(sql);
                long executionTime = System.currentTimeMillis() - startTime;

                if (hasResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        lastResult = new QueryResult(extractColumns(rs), extractRows(rs), executionTime);
                    }
                } else {
                    int count = Math.max(stmt.getUpdateCount(), 0);
                    lastResult = new QueryResult(count, executionTime, isDdlStatement(sql));
                }
            }
        }

        return lastResult;
    }

    private static final java.util.regex.Pattern LEADING_COMMENT = java.util.regex.Pattern.compile(
            "\\A(?:\\s+|--[^\\n]*(?:\\n|\\z)|/\\*.*?\\*/)*", java.util.regex.Pattern.DOTALL);
    private static final java.util.regex.Pattern FIRST_WORD = java.util.regex.Pattern.compile("\\A[A-Za-z_]+");

    /**
     * 去掉前导空白与注释，返回大写的首个关键字（如 SELECT / CREATE）。
     */
    private static String firstKeyword(String sql) {
        if (sql == null) {
            return "";
        }
        String stripped = LEADING_COMMENT.matcher(sql).replaceFirst("");
        java.util.regex.Matcher m = FIRST_WORD.matcher(stripped);
        return m.find() ? m.group().toUpperCase() : "";
    }

    /**
     * 判断语句是否会产生结果集，用于选择 executeQuery 还是 execute。
     */
    private boolean isResultSetStatement(String sql) {
        String keyword = firstKeyword(sql);
        return keyword.equals("SELECT") ||
               keyword.equals("SHOW") ||
               keyword.equals("DESCRIBE") ||
               keyword.equals("DESC") ||
               keyword.equals("EXPLAIN") ||
               keyword.equals("VALUES") ||
               keyword.equals("TABLE") ||
               keyword.equals("WITH") ||
               keyword.equals("SET") ||
               keyword.equals("CALL");
    }

    /**
     * 判断是否为 DDL。DDL 的 updateCount 为 0，与"影响 0 行的 DML"需要区分展示。
     */
    private boolean isDdlStatement(String sql) {
        String keyword = firstKeyword(sql);
        return keyword.equals("CREATE") ||
               keyword.equals("ALTER") ||
               keyword.equals("DROP") ||
               keyword.equals("TRUNCATE") ||
               keyword.equals("RENAME") ||
               keyword.equals("COMMENT") ||
               keyword.equals("GRANT") ||
               keyword.equals("REVOKE") ||
               keyword.equals("VACUUM") ||
               keyword.equals("ANALYZE") ||
               keyword.equals("REINDEX") ||
               keyword.equals("CLUSTER");
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
     * 用之前的结果覆盖 lastResult（DDL 不冲掉可导出的查询结果时使用）。
     */
    public void restoreLastResult(QueryResult result) {
        this.lastResult = result;
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

            boolean hasResultSet = stmt.execute(sql);
            if (!hasResultSet) {
                throw new SQLException(String.format(
                        "Statement executed but returned no result set, nothing exported (%d rows affected).",
                        Math.max(stmt.getUpdateCount(), 0)));
            }

            try (ResultSet rs = stmt.getResultSet()) {
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