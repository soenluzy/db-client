package com.example.dbclient.config;

import com.example.dbclient.db.DatabaseType;

public class DatabaseProfile {
    private DatabaseType type;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    public DatabaseType getType() {
        return type;
    }

    public void setType(DatabaseType type) {
        this.type = type;
    }

    public void setType(String type) {
        this.type = DatabaseType.fromString(type);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getJdbcUrl() {
        if (type == DatabaseType.MYSQL) {
            return String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        } else if (type == DatabaseType.POSTGRESQL) {
            return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        }
        throw new IllegalStateException("Unknown database type: " + type);
    }
}