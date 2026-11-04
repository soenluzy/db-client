package com.soenluzy.dbclient.export;

import com.soenluzy.dbclient.db.QueryResult;

import java.io.IOException;
import java.nio.file.Path;

public class ExporterFactory {

    public static Exporter create(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return new ExcelExporter();
        }
        return new CsvExporter();
    }

    public static void exportToFile(Path path, QueryResult result) throws IOException {
        Exporter exporter = create(path);
        exporter.export(result, path);
    }
}