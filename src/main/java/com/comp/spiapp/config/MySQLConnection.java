package com.comp.spiapp.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre una conexión
 * Quien la solicita debe cerrarla
 */
public final class MySQLConnection {
    private final DatabaseConfig config;

    public MySQLConnection(DatabaseConfig config) {
        this.config = config;
    }

    public Connection abrir() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", config.user());
        properties.setProperty("password", config.password());
        properties.setProperty("connectTimeout", "5000");
        properties.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection(config.url(), properties);
    }
}
