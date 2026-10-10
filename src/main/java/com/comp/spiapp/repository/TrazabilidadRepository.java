package com.comp.spiapp.repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.comp.spiapp.model.Campania;

/** Lecturas de CU-07 (sección 20.2 del TP2). Recibe la conexión del servicio y no la cierra. */
public final class TrazabilidadRepository {

    /** Siembras, aplicaciones y cosechas vigentes de la campaña, desde la vista del TP2. */
    public List<EtapaTrazabilidad> etapas(Campania campania, Connection conexion) throws SQLException {
        String sql = """
                SELECT establecimiento_id, establecimiento, lote_id, lote, orden_etapa, etapa, fecha,
                       detalle, cantidad, unidad, contratista, numero_orden, siembra_id
                FROM v_trazabilidad_campana
                WHERE campana_id = ?
                ORDER BY fecha, orden_etapa
                """;
        return leer(sql, campania, conexion);
    }

    /**
     * Lotes de órdenes vigentes sin ninguna aplicación, como una labranza. La vista solo trae las
     * aplicaciones; el TP2 indica que el servicio debe sumar estos trabajos a la cronología.
     * Devuelve las mismas columnas que la vista.
     */
    public List<EtapaTrazabilidad> trabajosSinInsumos(Campania campania, Connection conexion) throws SQLException {
        String sql = """
                SELECT e.establecimiento_id, e.nombre AS establecimiento, l.lote_id, l.nombre AS lote,
                       2 AS orden_etapa, CONCAT('Trabajo - ', lab.nombre) AS etapa, ot.fecha,
                       'Sin insumos' AS detalle, NULL AS cantidad, NULL AS unidad,
                       con.nombre AS contratista, ot.numero_orden, NULL AS siembra_id
                FROM ordenes_trabajo ot
                JOIN orden_lotes ol ON ol.orden_id = ot.orden_id
                JOIN lotes l ON l.lote_id = ol.lote_id
                JOIN establecimientos e ON e.establecimiento_id = l.establecimiento_id
                JOIN labores lab ON lab.labor_id = ot.labor_id
                JOIN contratistas con ON con.contratista_id = ot.contratista_id
                WHERE ot.campana_id = ? AND ot.eliminado_en IS NULL
                  AND NOT EXISTS (SELECT 1 FROM orden_lote_insumos oli WHERE oli.orden_lote_id = ol.orden_lote_id)
                """;
        return leer(sql, campania, conexion);
    }

    /** Hectáreas de las cosechas vigentes de la campaña. */
    public List<HectareasCosecha> hectareasDeCosechas(Campania campania, Connection conexion) throws SQLException {
        String sql = """
                SELECT co.siembra_id, co.fecha_inicio, co.hectareas
                FROM cosechas co
                JOIN siembras s ON s.siembra_id = co.siembra_id
                WHERE s.campana_id = ? AND co.eliminado_en IS NULL AND s.eliminado_en IS NULL
                """;
        List<HectareasCosecha> cosechas = new ArrayList<>();
        try (var consulta = conexion.prepareStatement(sql)) {
            new Parametros(consulta).id(campania);
            try (var filas = consulta.executeQuery()) {
                while (filas.next()) {
                    cosechas.add(new HectareasCosecha(filas.getInt("siembra_id"),
                            filas.getObject("fecha_inicio", LocalDate.class), filas.getBigDecimal("hectareas")));
                }
            }
        }
        return cosechas;
    }

    private List<EtapaTrazabilidad> leer(String sql, Campania campania, Connection conexion) throws SQLException {
        List<EtapaTrazabilidad> etapas = new ArrayList<>();
        try (var consulta = conexion.prepareStatement(sql)) {
            new Parametros(consulta).id(campania);
            try (var filas = consulta.executeQuery()) {
                while (filas.next()) {
                    etapas.add(mapear(filas));
                }
            }
        }
        return etapas;
    }

    private EtapaTrazabilidad mapear(ResultSet filas) throws SQLException {
        return new EtapaTrazabilidad(
                filas.getInt("establecimiento_id"),
                filas.getString("establecimiento"),
                filas.getInt("lote_id"),
                filas.getString("lote"),
                filas.getInt("orden_etapa"),
                filas.getString("etapa"),
                filas.getObject("fecha", LocalDate.class),
                filas.getString("detalle"),
                filas.getBigDecimal("cantidad"),
                filas.getString("unidad"),
                filas.getString("contratista"),
                filas.getString("numero_orden"),
                filas.getObject("siembra_id", Integer.class));
    }
}
