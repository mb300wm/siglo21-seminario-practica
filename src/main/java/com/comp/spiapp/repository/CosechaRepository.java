package com.comp.spiapp.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.comp.spiapp.model.Cosecha;

public final class CosechaRepository {
    public int insertar(Cosecha cosecha, Connection conexion) throws SQLException {
        String sql = """
                INSERT INTO cosechas (
                    siembra_id, fecha_inicio, fecha_fin, hectareas, humedad,
                    acopio_id, comentarios, creado_por, creado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        
        try (var sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            new Parametros(sentencia)
                    .id(cosecha.getSiembra())
                    .fecha(cosecha.getFechaInicio())
                    .fecha(cosecha.getFechaFin())
                    .decimal(cosecha.getHectareas())
                    .decimal(cosecha.getHumedad())
                    .idOpcional(cosecha.getAcopio())
                    .texto(cosecha.getComentarios())
                    .id(cosecha.getCreadoPor())
                    .momento(cosecha.getCreadoEn());
            sentencia.executeUpdate();
            return Claves.generada(sentencia);
        }
    }
}
