package com.example.dbclient.config;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class ConfigLoader {

    private static final String DEFAULT_CONFIG_DIR = System.getProperty("user.home") + "/.db-client";
    private static final String DEFAULT_CONFIG_FILE = "db-config.yaml";

    public static Map<String, DatabaseProfile> loadConfig(String configPath) {
        try {
            InputStream input = resolveInputStream(configPath);
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);
            return parseConfig(data);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Config file not found: " + configPath, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    private static InputStream resolveInputStream(String configPath) throws FileNotFoundException {
        if (configPath != null && !configPath.isEmpty()) {
            return new FileInputStream(configPath);
        }

        Path defaultPath = Paths.get(DEFAULT_CONFIG_DIR, DEFAULT_CONFIG_FILE);
        if (Files.exists(defaultPath)) {
            return new FileInputStream(defaultPath.toFile());
        }

        // Try classpath resource
        InputStream resource = ConfigLoader.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE);
        if (resource != null) {
            return resource;
        }

        throw new FileNotFoundException("No config file found at: " + defaultPath);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, DatabaseProfile> parseConfig(Map<String, Object> data) {
        Map<String, DatabaseProfile> profiles = new java.util.HashMap<>();
        Map<String, Map<String, Object>> profilesData = (Map<String, Map<String, Object>>) data.get("profiles");

        if (profilesData == null) {
            throw new RuntimeException("No 'profiles' section found in config");
        }

        for (Map.Entry<String, Map<String, Object>> entry : profilesData.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> profileData = entry.getValue();

            DatabaseProfile profile = new DatabaseProfile();
            profile.setType((String) profileData.get("type"));
            profile.setHost((String) profileData.get("host"));
            profile.setPort(((Number) profileData.get("port")).intValue());
            profile.setDatabase((String) profileData.get("database"));
            profile.setUsername((String) profileData.get("username"));
            profile.setPassword((String) profileData.get("password"));

            profiles.put(name, profile);
        }

        return profiles;
    }
}