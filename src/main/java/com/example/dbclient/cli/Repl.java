package com.example.dbclient.cli;

import com.example.dbclient.config.DatabaseProfile;
import com.example.dbclient.db.DatabaseManager;
import org.jline.reader.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Repl implements AutoCloseable {
    private final LineReader reader;
    private final CommandHandler commandHandler;
    private final Terminal terminal;
    private final DatabaseManager dbManager;

    public Repl(Map<String, DatabaseProfile> profiles, String initialProfile) throws IOException {
        this.terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        this.dbManager = new DatabaseManager();

        this.commandHandler = new CommandHandler(dbManager, profiles);

        this.reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new NullCompleter())
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "")
                .build();

        if (initialProfile != null && profiles.containsKey(initialProfile)) {
            connectToProfile(initialProfile);
        }
    }

    public void connectToProfile(String profileName) {
        try {
            DatabaseProfile profile = commandHandler.profiles.get(profileName);
            dbManager.connect(profile);
        } catch (Exception e) {
            System.err.println("Initial connection failed: " + e.getMessage());
        }
    }

    public void run() {
        printWelcome();

        while (true) {
            try {
                String prompt = buildPrompt();
                String line = reader.readLine(prompt);

                if (line == null) {
                    break;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String result = commandHandler.handle(line);

                if ("EXIT".equals(result)) {
                    break;
                }

                terminal.writer().println(result);
                terminal.writer().flush();

            } catch (UserInterruptException e) {
                // Ctrl+C - continue
            } catch (EndOfFileException e) {
                break;
            } catch (Exception e) {
                terminal.writer().println("Error: " + e.getMessage());
                terminal.writer().flush();
            }
        }

        cleanup();
    }

    private void printWelcome() {
        terminal.writer().println("Database Client REPL");
        terminal.writer().println("Type \\help for available commands");
        terminal.writer().println();
    }

    private String buildPrompt() {
        if (!dbManager.isConnected()) {
            return "(not connected)> ";
        }

        DatabaseProfile profile = dbManager.getCurrentProfile();
        String dbName = profile.getDatabase();
        String type = profile.getType().getName().toUpperCase();

        return "[" + type + "] " + dbName + "> ";
    }

    private void cleanup() {
        try {
            dbManager.close();
        } catch (Exception ignored) {
        }
        terminal.writer().println("\nGoodbye!");
    }

    @Override
    public void close() throws IOException {
        cleanup();
        terminal.close();
    }

    private static class NullCompleter implements Completer {
        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            // No completion
        }
    }
}