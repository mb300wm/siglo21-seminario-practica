package com.comp.spiapp.repository;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.comp.spiapp.config.DatabaseConfig;
import com.comp.spiapp.config.MySQLConnection;

@FunctionalInterface 
public interface ConexionBD {
    Connection abrir() throws SQLException;

    static ConexionBD desdeConfiguracion() {
        return () -> {
            try {
                return new MySQLConnection(DatabaseConfig.load()).abrir();
            } catch (IOException | IllegalArgumentException ex) {
                throw new SQLException("No se pudo cargar la configuración de conexión.", ex);
            }
        };
    }
}
