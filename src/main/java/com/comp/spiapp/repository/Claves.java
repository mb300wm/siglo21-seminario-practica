package com.comp.spiapp.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Lee el ID autoincrementado que MySQL asignó en el último insert
 * Claves
 */
final class Claves {
    private Claves() {}

    static int generada(PreparedStatement sentencia) throws SQLException {
        try (var claves = sentencia.getGeneratedKeys()) {
            if (!claves.next()) {
                throw new SQLException("MySQL no devolvió el id del registro insertado.");
            }
            return claves.getInt(1);
        }
    }
}
