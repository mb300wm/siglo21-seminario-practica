package com.comp.spiapp.repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.comp.spiapp.model.Entidad;

final class Parametros {
    private final PreparedStatement sentencia;
    private int indice;

    Parametros(PreparedStatement sentencia) {
        this.sentencia = sentencia;
    }

    Parametros id(Entidad entidad) throws SQLException {
        if (entidad.getId() == null) {
            throw new IllegalArgumentException("La referencia todavía no está guardada en la base.");
        }
        sentencia.setInt(++indice, entidad.getId());
        return this;
    }

    Parametros idOpcional(Entidad entidad) throws SQLException {
        return entidad == null ? nulo(Types.INTEGER) : id(entidad);
    }

    Parametros entero(int valor) throws SQLException {
        sentencia.setInt(++indice, valor);
        return this;
    }

    Parametros entero(Integer valor) throws SQLException {
        return valor == null ? nulo(Types.INTEGER) : entero(valor.intValue());
    }

    Parametros decimal(BigDecimal valor) throws SQLException {
        if (valor == null) {
            return nulo(Types.DECIMAL);
        }
        sentencia.setBigDecimal(++indice, valor);
        return this;
    }

    Parametros texto(String valor) throws SQLException {
        if (valor == null) {
            return nulo(Types.VARCHAR);
        }
        sentencia.setString(++indice, valor);
        return this;
    }

    Parametros fecha(LocalDate valor) throws SQLException {
        sentencia.setObject(++indice, valor);
        return this;
    }

    Parametros momento(LocalDateTime valor) throws SQLException {
        sentencia.setObject(++indice, valor);
        return this;
    }

    private Parametros nulo(int tipo) throws SQLException {
        sentencia.setNull(++indice, tipo);
        return this;
    }
}
