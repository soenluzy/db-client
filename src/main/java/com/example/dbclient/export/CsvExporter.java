package com.example.dbclient.export;

import com.example.dbclient.db.QueryResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvExporter implements Exporter {

    private static final int BATCH_SIZE = 1000;

    @Override
    public void export(QueryResult result, Path path) throws IOException {
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path), "UTF-8");
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            // Write header
            printer.printRecord(result.getColumns());

            // Write rows in batches
            List<List<Object>> rows = result.getRows();
            int total = rows.size();
            int count = 0;

            for (List<Object> row : rows) {
                printer.printRecord(row);
                count++;

                // Flush every BATCH_SIZE rows for large datasets
                if (count % BATCH_SIZE == 0) {
                    printer.flush();
                    // Progress indicator for very large exports
                    if (total > BATCH_SIZE * 10) {
                        System.err.printf("\rExporting: %d / %d rows", count, total);
                    }
                }
            }

            if (total > BATCH_SIZE * 10) {
                System.err.println();
            }
        }
    }
}