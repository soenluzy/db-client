package com.example.dbclient.formatter;

import com.example.dbclient.db.QueryResult;

import java.util.List;

public class TableFormatter {

    public static String format(QueryResult result) {
        if (result == null) {
            return "No result";
        }

        List<String> columns = result.getColumns();
        List<List<Object>> rows = result.getRows();

        if (columns.isEmpty()) {
            if (result.getUpdateCount() >= 0) {
                return String.format("(%d rows affected in %dms)", result.getUpdateCount(), result.getExecutionTimeMs());
            }
            return String.format("(0 rows in %dms)", result.getExecutionTimeMs());
        }

        int[] widths = calculateColumnWidths(columns, rows);
        StringBuilder sb = new StringBuilder();

        sb.append(formatRow(columns, widths)).append("\n");
        sb.append(formatSeparator(widths, '+', '-')).append("\n");

        for (List<Object> row : rows) {
            sb.append(formatRow(row, widths)).append("\n");
        }

        sb.append(formatSeparator(widths, '+', '-'));
        sb.append(String.format("\n(%d rows in %dms)", result.getRowCount(), result.getExecutionTimeMs()));

        return sb.toString();
    }

    private static int[] calculateColumnWidths(List<String> columns, List<List<Object>> rows) {
        int[] widths = new int[columns.size()];

        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).length();
        }

        for (List<Object> row : rows) {
            for (int i = 0; i < row.size() && i < widths.length; i++) {
                int len = row.get(i) != null ? row.get(i).toString().length() : 4; // "NULL"
                widths[i] = Math.max(widths[i], len);
            }
        }

        return widths;
    }

    private static String formatRow(List<?> cells, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < cells.size() && i < widths.length; i++) {
            String value = cells.get(i) != null ? cells.get(i).toString() : "NULL";
            sb.append(" ").append(padRight(value, widths[i])).append(" |");
        }
        return sb.toString();
    }

    private static String formatSeparator(int[] widths, char corner, char fill) {
        StringBuilder sb = new StringBuilder();
        sb.append(corner);
        for (int width : widths) {
            for (int i = 0; i < width + 2; i++) {
                sb.append('-');
            }
            sb.append(corner);
        }
        return sb.toString();
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}