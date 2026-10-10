package com.comp.spiapp.service;

import java.sql.SQLException;

/**
 * OperacionException
 * Error de base de datos con un mensaje apto para la pantalla, sin detalles internos (RNF08).
 */
public final class OperacionException extends Exception {
    private static final long serialVersionUID = 1L;

    /** 
     * SQLSTATE que usan los disparadores del esquema para rechazar datos.
     */
    private static final String RECHAZO_DE_DISPARADOR = "45000";

    public OperacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /** 
     * Los rechazos de los disparadores ya traen un mensaje pensado para el usuario
     */
    public static OperacionException desde(SQLException ex, String accion) {
        if (RECHAZO_DE_DISPARADOR.equals(ex.getSQLState())) {
            return new OperacionException(ex.getMessage(), ex);
        }
        return new OperacionException(
            "No se pudo " + accion + ". Revisa la conexión con MySQL y vuelve a intentar", ex);
    }
}
