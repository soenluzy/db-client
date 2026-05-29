package com.example.dbclient;

import com.example.dbclient.cli.Repl;
import com.example.dbclient.config.DatabaseProfile;
import com.example.dbclient.config.ConfigLoader;

import java.util.Map;

public class Main {

    public static void main(String[] args) {
        String configPath = null;
        String profileName = null;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--config".equals(arg)) {
                if (i + 1 < args.length) {
                    configPath = args[++i];
                }
            } else if ("--profile".equals(arg)) {
                if (i + 1 < args.length) {
                    profileName = args[++i];
                }
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsage();
                return;
            }
        }

        // Load config
        Map<String, DatabaseProfile> profiles = ConfigLoader.loadConfig(configPath);

        if (profiles.isEmpty()) {
            System.err.println("No profiles found in config");
            System.exit(1);
        }

        // Default profile
        if (profileName == null) {
            profileName = profiles.keySet().iterator().next();
        }

        if (!profiles.containsKey(profileName)) {
            System.err.println("Unknown profile: " + profileName);
            System.err.println("Available profiles: " + profiles.keySet());
            System.exit(1);
        }

        // Start REPL
        try (Repl repl = new Repl(profiles, profileName)) {
            repl.run();
        } catch (Exception e) {
            System.err.println("Failed to start REPL: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Database Client CLI");
        System.out.println("");
        System.out.println("Usage: db-client [options]");
        System.out.println("");
        System.out.println("Options:");
        System.out.println("  --config <path>   Path to config file (default: ~/.db-client/db-config.yaml)");
        System.out.println("  --profile <name>  Database profile to connect (default: first profile)");
        System.out.println("  -h, --help        Show this help");
        System.out.println("");
        System.out.println("Example:");
        System.out.println("  db-client --profile mysql-dev");
        System.out.println("  db-client --config /path/to/config.yaml --profile pgsql-dev");
    }
}