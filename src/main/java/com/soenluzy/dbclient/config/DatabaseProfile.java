package com.soenluzy.dbclient.config;

import com.soenluzy.dbclient.db.DatabaseType;

public class DatabaseProfile {
    private DatabaseType type;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String schema;

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

    /**
     * 目标 schema。多个 schema 用逗号分隔，按优先级排列（对应 PostgreSQL 的 search_path）。
     */
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getJdbcUrl() {
        if (type == DatabaseType.MYSQL) {
            return String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        } else if (type == DatabaseType.POSTGRESQL) {
            StringBuilder url = new StringBuilder(
                    String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
            if (schema != null && !schema.trim().isEmpty()) {
                // currentSchema 会被追加到 search_path 最前面，等价于 SET search_path TO <schema>
                url.append("?currentSchema=").append(encodeQueryValue(schema.trim()));
            }
            return url.toString();
        }
        throw new IllegalStateException("Unknown database type: " + type);
    }

    private static String encodeQueryValue(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8")
                    .replace("+", "%20")
                    .replace("%2C", ",");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
}