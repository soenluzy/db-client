package com.example.dbclient.export;

import com.example.dbclient.db.QueryResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ExcelExporter implements Exporter {

    private static final int FLUSH_ROWS = 1000;

    @Override
    public void export(QueryResult result, Path path) throws IOException {
        // Use SXSSFWorkbook for streaming write to handle large datasets
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(FLUSH_ROWS)) {
            workbook.setCompressTempFiles(true);
            SXSSFSheet sheet = workbook.createSheet("Query Result");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            int rowIndex = 0;

            // Write header
            Row headerRow = sheet.createRow(rowIndex++);
            List<String> columns = result.getColumns();
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Write data rows
            List<List<Object>> rows = result.getRows();
            int total = rows.size();
            int count = 0;

            for (List<Object> dataRow : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < dataRow.size(); i++) {
                    Cell cell = row.createCell(i);
                    Object value = dataRow.get(i);
                    if (value == null) {
                        cell.setCellValue("");
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value.toString());
                    }
                    cell.setCellStyle(dataStyle);
                }

                count++;
                if (count % FLUSH_ROWS == 0 && total > FLUSH_ROWS * 10) {
                    System.err.printf("\rExporting: %d / %d rows", count, total);
                }
            }

            if (total > FLUSH_ROWS * 10) {
                System.err.println();
            }

            try (FileOutputStream outputStream = new FileOutputStream(path.toFile())) {
                workbook.write(outputStream);
            }
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}