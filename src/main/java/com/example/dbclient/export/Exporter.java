package com.example.dbclient.export;

import com.example.dbclient.db.QueryResult;
import java.io.IOException;
import java.nio.file.Path;

public interface Exporter {
    void export(QueryResult result, Path path) throws IOException;
}