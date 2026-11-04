package com.soenluzy.dbclient.export;

import com.soenluzy.dbclient.db.QueryResult;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface Exporter {
    void export(QueryResult result, Path path) throws IOException;

    /**
     * 从 ResultSet 直接流式写入文件，不经过内存列表。
     * 用于 \export-sql 命令的大数据导出场景。
     *
     * @param rs      查询结果集
     * @param columns 列名列表
     * @param path    输出文件路径
     * @return 写入的行数
     */
    long exportFromResultSet(ResultSet rs, List<String> columns, Path path) throws SQLException, IOException;
}
