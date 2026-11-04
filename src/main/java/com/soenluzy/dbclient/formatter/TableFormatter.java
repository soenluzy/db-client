package com.soenluzy.dbclient.formatter;

import com.soenluzy.dbclient.db.QueryResult;

import java.util.List;

public class TableFormatter {

    public static final int DEFAULT_PAGE_SIZE = 50;

    public static String format(QueryResult result) {
        return format(result, Integer.MAX_VALUE);
    }

    public static String format(QueryResult result, int maxRows) {
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

        int totalRows = rows.size();
        boolean truncated = totalRows > maxRows;
        int displayRows = truncated ? maxRows : totalRows;

        // Only calculate widths from the rows we actually display
        int[] widths = calculateColumnWidths(columns, rows, displayRows);
        StringBuilder sb = new StringBuilder();

        sb.append(formatRow(columns, widths)).append("\n");
        sb.append(formatSeparator(widths, '+', '-')).append("\n");

        for (int i = 0; i < displayRows; i++) {
            sb.append(formatRow(rows.get(i), widths)).append("\n");
        }

        sb.append(formatSeparator(widths, '+', '-'));

        if (truncated) {
            sb.append(String.format("\n(%d rows total, showing first %d, %dms)",
                    totalRows, maxRows, result.getExecutionTimeMs()));
        } else {
            sb.append(String.format("\n(%d rows in %dms)", totalRows, result.getExecutionTimeMs()));
        }

        return sb.toString();
    }

    private static int[] calculateColumnWidths(List<String> columns, List<List<Object>> rows, int maxRows) {
        int[] widths = new int[columns.size()];

        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).length();
        }

        int limit = Math.min(rows.size(), maxRows);
        for (int r = 0; r < limit; r++) {
            List<Object> row = rows.get(r);
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