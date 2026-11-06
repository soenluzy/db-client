package com.soenluzy.dbclient.db;

import java.util.ArrayList;
import java.util.List;

public class QueryResult {
    private final List<String> columns;
    private final List<List<Object>> rows;
    private final long executionTimeMs;
    private final int updateCount;
    private final boolean ddl;

    public QueryResult(List<String> columns, List<List<Object>> rows, long executionTimeMs) {
        this.columns = columns;
        this.rows = rows;
        this.executionTimeMs = executionTimeMs;
        this.updateCount = -1;
        this.ddl = false;
    }

    public QueryResult(int updateCount, long executionTimeMs) {
        this(updateCount, executionTimeMs, false);
    }

    public QueryResult(int updateCount, long executionTimeMs, boolean ddl) {
        this.columns = new ArrayList<>();
        this.columns.add("affected_rows");
        this.rows = new ArrayList<>();
        List<Object> row = new ArrayList<>();
        row.add(updateCount);
        this.rows.add(row);
        this.executionTimeMs = executionTimeMs;
        this.updateCount = updateCount;
        this.ddl = ddl;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    /**
     * 是否为 DDL 语句的执行结果（updateCount 无实际意义）。
     */
    public boolean isDdl() {
        return ddl;
    }

    public int getRowCount() {
        return rows.size();
    }

    public boolean hasData() {
        return !rows.isEmpty();
    }

    public String getColumnName(int index) {
        return columns.get(index);
    }

    public int getColumnCount() {
        return columns.size();
    }
}