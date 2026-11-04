package com.soenluzy.dbclient.db;

import java.util.List;

/**
 * 流式查询结果：不将所有行加载到内存，仅记录元信息。
 * 用于 \export-sql 命令的流式导出场景。
 */
public class StreamingQueryResult {
    private final List<String> columns;
    private final long rowCount;
    private final long executionTimeMs;

    public StreamingQueryResult(List<String> columns, long rowCount, long executionTimeMs) {
        this.columns = columns;
        this.rowCount = rowCount;
        this.executionTimeMs = executionTimeMs;
    }

    public List<String> getColumns() {
        return columns;
    }

    public long getRowCount() {
        return rowCount;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
}
