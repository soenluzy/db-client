package com.soenluzy.dbclient.cli;

import com.soenluzy.dbclient.config.DatabaseProfile;
import com.soenluzy.dbclient.db.DatabaseManager;
import com.soenluzy.dbclient.db.QueryResult;
import com.soenluzy.dbclient.db.StreamingQueryResult;
import com.soenluzy.dbclient.export.Exporter;
import com.soenluzy.dbclient.export.ExporterFactory;
import com.soenluzy.dbclient.formatter.TableFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandHandler {
    private static final Pattern EXPORT_PATTERN = Pattern.compile("\\\\export\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPORT_SQL_PATTERN = Pattern.compile("\\\\export-sql\\s+(\\S+)\\s+(.+)");

    private final DatabaseManager dbManager;
    public final Map<String, DatabaseProfile> profiles;
    private boolean quietMode = false;

    public CommandHandler(DatabaseManager dbManager, Map<String, DatabaseProfile> profiles) {
        this.dbManager = dbManager;
        this.profiles = profiles;
    }

    public String handle(String input) {
        String trimmed = input.trim();

        if (trimmed.equalsIgnoreCase("\\exit") || trimmed.equalsIgnoreCase("\\quit")) {
            return "EXIT";
        }

        if (trimmed.equalsIgnoreCase("\\help")) {
            return getHelpText();
        }

        Matcher exportMatcher = EXPORT_PATTERN.matcher(trimmed);
        if (exportMatcher.matches()) {
            return handleExport(exportMatcher.group(1).trim());
        }

        Matcher exportSqlMatcher = EXPORT_SQL_PATTERN.matcher(trimmed);
        if (exportSqlMatcher.matches()) {
            return handleExportSql(exportSqlMatcher.group(1).trim(), exportSqlMatcher.group(2).trim());
        }

        if (trimmed.toLowerCase().startsWith("\\connect ")) {
            return handleConnect(trimmed.substring(9).trim());
        }

        if (trimmed.toLowerCase().startsWith("\\quiet ")) {
            return handleQuiet(trimmed.substring(7).trim());
        }

        // Execute SQL
        return executeSql(trimmed);
    }

    private String getHelpText() {
        return "Available commands:\n" +
               "  SELECT ...             Execute SQL query\n" +
               "  \\export <path>         Export last result to CSV/XLSX\n" +
               "  \\export-sql <path> <sql>  Execute SQL and export directly (streaming, no display)\n" +
               "  \\quiet on|off          Toggle quiet mode (show summary only)\n" +
               "  \\connect <profile>     Switch database connection\n" +
               "  \\help                  Show this help\n" +
               "  \\exit, \\quit           Exit the REPL";
    }

    private String handleExport(String pathStr) {
        QueryResult lastResult = dbManager.getLastResult();
        if (lastResult == null || !lastResult.hasData()) {
            return "No data to export. Run a SELECT query first.";
        }

        try {
            Path path = Paths.get(pathStr);
            ExporterFactory.exportToFile(path, lastResult);
            return "Exported to " + pathStr;
        } catch (IOException e) {
            return "Export failed: " + e.getMessage();
        }
    }

    private String handleExportSql(String pathStr, String sql) {
        try {
            Path path = Paths.get(pathStr);
            Exporter exporter = ExporterFactory.create(path);
            StreamingQueryResult result = dbManager.executeAndExport(sql, exporter, path);
            return String.format("Exported %d rows to %s (%dms)",
                    result.getRowCount(), pathStr, result.getExecutionTimeMs());
        } catch (SQLException e) {
            return "Export failed (SQL Error):\n" + getFullStackTrace(e);
        } catch (IOException e) {
            return "Export failed (IO Error): " + e.getMessage();
        }
    }

    private String handleQuiet(String mode) {
        if (mode.equalsIgnoreCase("on")) {
            quietMode = true;
            return "Quiet mode ON - query results will show summary only.";
        } else if (mode.equalsIgnoreCase("off")) {
            quietMode = false;
            return "Quiet mode OFF - query results will be displayed.";
        } else {
            return "Usage: \\quiet on|off";
        }
    }

    private String handleConnect(String profileName) {
        DatabaseProfile profile = profiles.get(profileName);
        if (profile == null) {
            return "Unknown profile: " + profileName + ". Available: " + profiles.keySet();
        }

        try {
            dbManager.connect(profile);
            return "Connected to " + profileName;
        } catch (SQLException e) {
            return "Connection failed:\n" + getFullStackTrace(e);
        }
    }

    private String executeSql(String sql) {
        try {
            QueryResult result = dbManager.executeQuery(sql);

            if (quietMode && result.hasData() && result.getUpdateCount() < 0) {
                // Quiet mode: SELECT-like queries show summary only
                return String.format("(%d rows in %dms)", result.getRowCount(), result.getExecutionTimeMs());
            }

            return TableFormatter.format(result, TableFormatter.DEFAULT_PAGE_SIZE);
        } catch (SQLException e) {
            return "SQL Error:\n" + getFullStackTrace(e);
        }
    }

    private String getFullStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("    at ").append(element).append("\n");
            if (sb.length() > 500) {
                sb.append("    ... (truncated)");
                break;
            }
        }
        return sb.toString();
    }
}