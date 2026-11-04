package com.soenluzy.dbclient.export;

import com.soenluzy.dbclient.db.QueryResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CsvExporter implements Exporter {

    private static final int BATCH_SIZE = 1000;

    @Override
    public void export(QueryResult result, Path path) throws IOException {
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8);
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

    @Override
    public long exportFromResultSet(ResultSet rs, List<String> columns, Path path) throws SQLException, IOException {
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            // Write header
            printer.printRecord(columns);

            long rowCount = 0;
            int columnCount = columns.size();

            while (rs.next()) {
                List<Object> row = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                printer.printRecord(row);
                rowCount++;

                if (rowCount % BATCH_SIZE == 0) {
                    printer.flush();
                    System.err.printf("\rExporting: %d rows", rowCount);
                }
            }

            if (rowCount >= BATCH_SIZE) {
                System.err.println();
            }

            return rowCount;
        }
    }
}
