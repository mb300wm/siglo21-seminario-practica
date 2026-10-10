package com.comp.spiapp.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comp.spiapp.model.AplicacionInsumo;
import com.comp.spiapp.model.Cultivo;
import com.comp.spiapp.model.GrupoInsumo;
import com.comp.spiapp.model.Insumo;
import com.comp.spiapp.model.Lote;
import com.comp.spiapp.model.OrdenInsumo;
import com.comp.spiapp.model.OrdenLote;
import com.comp.spiapp.model.OrdenTrabajo;
import com.comp.spiapp.model.ValidacionException;

/**
 * Lotes, insumos a remitir y aplicaciones de una orden (orden_lotes, orden_insumos y
 * orden_lote_insumos). Recibe la conexión del servicio y no la cierra.
 */
public final class DetalleOrdenRepository {

    /** Devuelve los ids generados, en el mismo orden que la lista. */
    public List<Integer> insertarLotes(int ordenId, List<OrdenLote> lotes, Connection conexion) throws SQLException {
        String sql = "INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha) VALUES (?, ?, ?, ?)";
        List<Integer> ids = new ArrayList<>();
        try (var sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (OrdenLote ordenLote : lotes) {
                new Parametros(sentencia)
                        .entero(ordenId)
                        .id(ordenLote.getLote())
                        .idOpcional(ordenLote.getCultivo())
                        .decimal(ordenLote.getSuperficieHa());
                sentencia.executeUpdate();
                ids.add(Claves.generada(sentencia));
            }
        }
        return ids;
    }

    public int insertarInsumo(int ordenId, OrdenInsumo ordenInsumo, Connection conexion) throws SQLException {
        String sql = """
                INSERT INTO orden_insumos (orden_id, insumo_id, cantidad_a_remitir, unidad)
                VALUES (?, ?, ?, ?)
                """;
        try (var sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            new Parametros(sentencia)
                    .entero(ordenId)
                    .id(ordenInsumo.getInsumo())
                    .decimal(ordenInsumo.getCantidadARemitir())
                    .texto(ordenInsumo.getUnidad());
            sentencia.executeUpdate();
            return Claves.generada(sentencia);
        }
    }

    /**
     * orden_id se repite a propósito: las claves foráneas compuestas (orden_lote_id, orden_id) y
     * (orden_insumo_id, orden_id) garantizan que el lote y el insumo sean de la misma orden.
     */
    public int insertarAplicacion(int ordenId, int ordenLoteId, int ordenInsumoId, BigDecimal total,
                                  Connection conexion) throws SQLException {
        String sql = """
                INSERT INTO orden_lote_insumos (orden_id, orden_lote_id, orden_insumo_id, total)
                VALUES (?, ?, ?, ?)
                """;
        try (var sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            new Parametros(sentencia).entero(ordenId).entero(ordenLoteId).entero(ordenInsumoId).decimal(total);
            sentencia.executeUpdate();
            return Claves.generada(sentencia);
        }
    }

    /** Agrega a la orden sus detalles guardados; el dominio vuelve a validar cada relación. */
    public void cargar(OrdenTrabajo orden, Connection conexion) throws SQLException {
        try {
            Map<Integer, OrdenLote> lotes = cargarLotes(orden, conexion);
            Map<Integer, OrdenInsumo> insumos = cargarInsumos(orden, conexion);
            cargarAplicaciones(orden, lotes, insumos, conexion);
        } catch (ValidacionException ex) {
            throw new SQLException("Un detalle guardado de la orden contiene datos inválidos.", ex);
        }
    }

    private Map<Integer, OrdenLote> cargarLotes(OrdenTrabajo orden, Connection conexion)
            throws SQLException, ValidacionException {
        String sql = """
                SELECT ol.orden_lote_id, ol.superficie_ha, l.lote_id, l.nombre, l.hectareas,
                       c.cultivo_id, c.nombre AS cultivo
                FROM orden_lotes ol
                JOIN lotes l ON l.lote_id = ol.lote_id
                LEFT JOIN cultivos c ON c.cultivo_id = ol.cultivo_id
                WHERE ol.orden_id = ?
                ORDER BY l.nombre
                """;
        Map<Integer, OrdenLote> porId = new HashMap<>();
        try (var consulta = conexion.prepareStatement(sql)) {
            new Parametros(consulta).id(orden);
            try (var filas = consulta.executeQuery()) {
                while (filas.next()) {
                    // El disparador del esquema garantiza que el lote es del establecimiento de la orden.
                    var lote = new Lote(filas.getInt("lote_id"), filas.getString("nombre"),
                            orden.getEstablecimiento(), filas.getBigDecimal("hectareas"));
                    Integer cultivoId = filas.getObject("cultivo_id", Integer.class);
                    var cultivo = cultivoId == null ? null : new Cultivo(cultivoId, filas.getString("cultivo"));
                    var ordenLote = new OrdenLote(filas.getInt("orden_lote_id"), lote, cultivo,
                            filas.getBigDecimal("superficie_ha"));
                    orden.agregarLote(ordenLote);
                    porId.put(ordenLote.getId(), ordenLote);
                }
            }
        }
        return porId;
    }

    private Map<Integer, OrdenInsumo> cargarInsumos(OrdenTrabajo orden, Connection conexion)
            throws SQLException, ValidacionException {
        String sql = """
                SELECT oi.orden_insumo_id, oi.cantidad_a_remitir, oi.unidad,
                       i.insumo_id, i.nombre, g.grupo_id, g.nombre AS grupo, c.cultivo_id, c.nombre AS cultivo
                FROM orden_insumos oi
                JOIN insumos i ON i.insumo_id = oi.insumo_id
                JOIN grupos_insumo g ON g.grupo_id = i.grupo_id
                LEFT JOIN cultivos c ON c.cultivo_id = g.cultivo_id
                WHERE oi.orden_id = ?
                ORDER BY i.nombre
                """;
        Map<Integer, OrdenInsumo> porId = new HashMap<>();
        try (var consulta = conexion.prepareStatement(sql)) {
            new Parametros(consulta).id(orden);
            try (var filas = consulta.executeQuery()) {
                while (filas.next()) {
                    Integer cultivoId = filas.getObject("cultivo_id", Integer.class);
                    var cultivo = cultivoId == null ? null : new Cultivo(cultivoId, filas.getString("cultivo"));
                    var grupo = new GrupoInsumo(filas.getInt("grupo_id"), filas.getString("grupo"), cultivo);
                    var insumo = new Insumo(filas.getInt("insumo_id"), filas.getString("nombre"), grupo, null);
                    var ordenInsumo = new OrdenInsumo(filas.getInt("orden_insumo_id"), insumo,
                            filas.getBigDecimal("cantidad_a_remitir"), filas.getString("unidad"));
                    orden.agregarInsumo(ordenInsumo);
                    porId.put(ordenInsumo.getId(), ordenInsumo);
                }
            }
        }
        return porId;
    }

    private void cargarAplicaciones(OrdenTrabajo orden, Map<Integer, OrdenLote> lotes,
                                    Map<Integer, OrdenInsumo> insumos, Connection conexion)
            throws SQLException, ValidacionException {
        String sql = """
                SELECT id, orden_lote_id, orden_insumo_id, total
                FROM orden_lote_insumos
                WHERE orden_id = ?
                ORDER BY id
                """;
        try (var consulta = conexion.prepareStatement(sql)) {
            new Parametros(consulta).id(orden);
            try (var filas = consulta.executeQuery()) {
                while (filas.next()) {
                    orden.agregarAplicacion(new AplicacionInsumo(filas.getInt("id"),
                            lotes.get(filas.getInt("orden_lote_id")), insumos.get(filas.getInt("orden_insumo_id")),
                            filas.getBigDecimal("total")));
                }
            }
        }
    }
}
