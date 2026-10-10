package com.comp.spiapp.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.comp.spiapp.model.Pesaje;

public final class PesajeRepository {
    public void insertarTodos(
        int cosechaId, 
        List<Pesaje> pesajes, Connection conexion
    ) throws SQLException {
        String sql = """
                INSERT INTO pesajes (cosecha_id, orden_llegada, peso_bruto, peso_tara) 
                VALUES (?, ?, ?, ?)
                """;
            
        try (var sentencia = conexion.prepareStatement(sql)) {
            for (Pesaje pesaje : pesajes) {
                new Parametros(sentencia)
                        .entero(cosechaId)
                        .entero(pesaje.getOrdenLlegada())
                        .decimal(pesaje.getPesoBruto())
                        .decimal(pesaje.getPesoTara());
                sentencia.executeUpdate();
            }
        }
    }
}
