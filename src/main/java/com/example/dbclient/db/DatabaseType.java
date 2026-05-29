package com.example.dbclient.db;

public enum DatabaseType {
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("postgresql", "org.postgresql.Driver");

    private final String name;
    private final String driverClass;

    DatabaseType(String name, String driverClass) {
        this.name = name;
        this.driverClass = driverClass;
    }

    public String getName() {
        return name;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public static DatabaseType fromString(String type) {
        for (DatabaseType dt : values()) {
            if (dt.name.equalsIgnoreCase(type)) {
                return dt;
            }
        }
        throw new IllegalArgumentException("Unknown database type: " + type);
    }
}