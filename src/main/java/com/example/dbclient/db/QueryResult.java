package com.example.dbclient.db;

import java.util.ArrayList;
import java.util.List;

public class QueryResult {
    private final List<String> columns;
    private final List<List<Object>> rows;
    private final long executionTimeMs;
    private final int updateCount;

    public QueryResult(List<String> columns, List<List<Object>> rows, long executionTimeMs) {
        this.columns = columns;
        this.rows = rows;
        this.executionTimeMs = executionTimeMs;
        this.updateCount = -1;
    }

    public QueryResult(int updateCount, long executionTimeMs) {
        this.columns = new ArrayList<>();
        this.columns.add("affected_rows");
        this.rows = new ArrayList<>();
        List<Object> row = new ArrayList<>();
        row.add(updateCount);
        this.rows.add(row);
        this.executionTimeMs = executionTimeMs;
        this.updateCount = updateCount;
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