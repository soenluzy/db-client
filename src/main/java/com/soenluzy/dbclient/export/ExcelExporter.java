package com.soenluzy.dbclient.export;

import com.soenluzy.dbclient.db.QueryResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                writeRow(row, dataRow, dataStyle);

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

    @Override
    public long exportFromResultSet(ResultSet rs, List<String> columns, Path path) throws SQLException, IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(FLUSH_ROWS)) {
            workbook.setCompressTempFiles(true);
            SXSSFSheet sheet = workbook.createSheet("Query Result");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            // Write header
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Stream data from ResultSet
            long rowCount = 0;
            int rowIndex = 1;
            int columnCount = columns.size();

            while (rs.next()) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 1; i <= columnCount; i++) {
                    Cell cell = row.createCell(i - 1);
                    Object value = rs.getObject(i);
                    setCellValue(cell, value);
                    cell.setCellStyle(dataStyle);
                }
                rowCount++;

                if (rowCount % FLUSH_ROWS == 0) {
                    System.err.printf("\rExporting: %d rows", rowCount);
                }
            }

            if (rowCount >= FLUSH_ROWS) {
                System.err.println();
            }

            try (FileOutputStream outputStream = new FileOutputStream(path.toFile())) {
                workbook.write(outputStream);
            }

            return rowCount;
        }
    }

    private void writeRow(Row row, List<Object> dataRow, CellStyle dataStyle) {
        for (int i = 0; i < dataRow.size(); i++) {
            Cell cell = row.createCell(i);
            Object value = dataRow.get(i);
            setCellValue(cell, value);
            cell.setCellStyle(dataStyle);
        }
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
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
