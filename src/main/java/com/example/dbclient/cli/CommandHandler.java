package com.example.dbclient.cli;

import com.example.dbclient.config.DatabaseProfile;
import com.example.dbclient.db.DatabaseManager;
import com.example.dbclient.db.QueryResult;
import com.example.dbclient.export.ExporterFactory;
import com.example.dbclient.formatter.TableFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandHandler {
    private static final Pattern EXPORT_PATTERN = Pattern.compile("\\\\export\\s+(.+)", Pattern.CASE_INSENSITIVE);

    private final DatabaseManager dbManager;
    public final Map<String, DatabaseProfile> profiles;

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

        if (trimmed.toLowerCase().startsWith("\\connect ")) {
            return handleConnect(trimmed.substring(9).trim());
        }

        // Execute SQL
        return executeSql(trimmed);
    }

    private String getHelpText() {
        return "Available commands:\n" +
               "  SELECT ...     Execute SQL query\n" +
               "  \\export <path>  Export last result to CSV/XLSX\n" +
               "  \\connect <profile>  Switch database connection\n" +
               "  \\help         Show this help\n" +
               "  \\exit, \\quit  Exit the REPL";
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
            return TableFormatter.format(result);
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