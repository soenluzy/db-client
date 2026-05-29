package com.example.dbclient.export;

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

    public static void exportToFile(Path path, com.example.dbclient.db.QueryResult result) throws IOException {
        Exporter exporter = create(path);
        exporter.export(result, path);
    }
}