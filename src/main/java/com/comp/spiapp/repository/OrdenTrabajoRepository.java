package com.comp.spiapp.repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.comp.spiapp.model.Campania;
import com.comp.spiapp.model.Contratista;
import com.comp.spiapp.model.Deposito;
import com.comp.spiapp.model.Establecimiento;
import com.comp.spiapp.model.Labor;
import com.comp.spiapp.model.OrdenTrabajo;
import com.comp.spiapp.model.Usuario;
import com.comp.spiapp.model.ValidacionException;

/**
 * OrdenTrabajoRepository
 */
public final class OrdenTrabajoRepository {
    /**
     * Bloquea la fila de la campaña hasta el commit o rollback: otra sesión que quiera numerar
     * una orden de la misma campaña espera acá. Devuelve el nombre de la campaña (prefijo AA-AA).
     */
    public String bloquearCampania(Campania campania, Connection conexion) throws SQLException {
        String sql = "SELECT nombre FROM campanas WHERE campana_id = ? FOR UPDATE";
        try (var consulta = conexion.prepareStatement(sql)) {
            new Parametros(consulta).id(campania);
            try (var filas = consulta.executeQuery()) {
                if (!filas.next()) {
                    throw new SQLException("La campaña de la orden ya no existe.");
                }
                return filas.getString("nombre");
            }
        }
    }

    /**
     * Mayor correlativo usado en la campaña, contando las órdenes dadas de baja para no reutilizar
     * su número. Debe llamarse después de bloquearCampania, dentro de la misma transacción.
     */
    public int ultimoCorrelativo(Campania campania, Connection conexion) throws SQLException {
        String sql = """
                SELECT COALESCE(MAX(CAST(RIGHT(numero_orden, 4) AS UNSIGNED)), 0) AS ultimo
                FROM ordenes_trabajo
                WHERE campana_id = ?
                """;
        try (var consulta = conexion.prepareStatement(sql)) {
            new Parametros(consulta).id(campania);
            try (var filas = consulta.executeQuery()) {
                filas.next();
                return filas.getInt("ultimo");
            }
        }
    }

    public int insertar(OrdenTrabajo orden, String numero, Connection conexion) throws SQLException {
        String sql = """
                INSERT INTO ordenes_trabajo (
                    numero_orden, fecha, labor_id, establecimiento_id, contratista_id,
                    deposito_id, remito_numero, campana_id, convenio, comentarios,
                    creado_por, creado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            new Parametros(sentencia)
                    .texto(numero)
                    .fecha(orden.getFecha())
                    .id(orden.getLabor())
                    .id(orden.getEstablecimiento())
                    .id(orden.getContratista())
                    .idOpcional(orden.getDeposito())
                    .texto(orden.getRemitoNumero())
                    .id(orden.getCampania())
                    .texto(orden.getConvenio())
                    .texto(orden.getComentarios())
                    .id(orden.getCreadoPor())
                    .momento(orden.getCreadoEn());
            sentencia.executeUpdate();
            return Claves.generada(sentencia);
        }
    }

    /** Auditoría de CU-05. Devuelve false si la orden ya no está vigente. */
    public boolean registrarModificacion(
        int ordenId, 
        Usuario usuario, 
        LocalDateTime momento, 
        Connection conexion
    ) throws SQLException {
        String sql = """
                UPDATE ordenes_trabajo SET modificado_por = ?, modificado_en = ?
                WHERE orden_id = ? AND eliminado_en IS NULL
                """;
        try (var sentencia = conexion.prepareStatement(sql)) {
            new Parametros(sentencia).id(usuario).momento(momento).entero(ordenId);
            return sentencia.executeUpdate() == 1;
        }
    }

    /** Órdenes sin baja lógica, de la más reciente a la más antigua. */
    public List<ResumenOrden> listarVigentes(Connection conexion) throws SQLException {
        String sql = """
                SELECT ot.orden_id, ot.numero_orden, ot.fecha, la.nombre AS labor, e.nombre AS establecimiento
                FROM ordenes_trabajo ot
                JOIN labores la ON la.labor_id = ot.labor_id
                JOIN establecimientos e ON e.establecimiento_id = ot.establecimiento_id
                WHERE ot.eliminado_en IS NULL
                ORDER BY ot.fecha DESC, ot.orden_id DESC
                """;
        List<ResumenOrden> ordenes = new ArrayList<>();
        try (var consulta = conexion.prepareStatement(sql); var filas = consulta.executeQuery()) {
            while (filas.next()) {
                ordenes.add(new ResumenOrden(filas.getInt("orden_id"), filas.getString("numero_orden"),
                        filas.getObject("fecha", LocalDate.class), filas.getString("labor"),
                        filas.getString("establecimiento")));
            }
        }
        return ordenes;
    }

    /** Cabecera de una orden vigente; los lotes, insumos y aplicaciones los agrega DetalleOrdenRepository. */
    public Optional<OrdenTrabajo> buscarCabecera(int ordenId, Connection conexion) throws SQLException {
        String sql = """
                SELECT ot.orden_id, ot.numero_orden, ot.fecha, ot.remito_numero, ot.convenio, ot.comentarios,
                       ot.creado_en,
                       ca.campana_id, ca.nombre AS campana,
                       la.labor_id, la.nombre AS labor,
                       e.establecimiento_id, e.nombre AS establecimiento,
                       co.contratista_id, co.nombre AS contratista,
                       d.deposito_id, d.nombre AS deposito,
                       u.usuario_id, u.nombre AS creador, u.usuario_login, u.password_hash, u.activo
                FROM ordenes_trabajo ot
                JOIN campanas ca ON ca.campana_id = ot.campana_id
                JOIN labores la ON la.labor_id = ot.labor_id
                JOIN establecimientos e ON e.establecimiento_id = ot.establecimiento_id
                JOIN contratistas co ON co.contratista_id = ot.contratista_id
                LEFT JOIN depositos d ON d.deposito_id = ot.deposito_id
                JOIN usuarios u ON u.usuario_id = ot.creado_por
                WHERE ot.orden_id = ? AND ot.eliminado_en IS NULL
                """;
        try (var consulta = conexion.prepareStatement(sql)) {
            new Parametros(consulta).entero(ordenId);
            try (var filas = consulta.executeQuery()) {
                return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
            }
        } catch (ValidacionException ex) {
            throw new SQLException("Una orden guardada contiene datos inválidos.", ex);
        }
    }

    private OrdenTrabajo mapear(ResultSet filas) throws SQLException, ValidacionException {
        Integer depositoId = filas.getObject("deposito_id", Integer.class);
        var deposito = depositoId == null ? null : new Deposito(depositoId, filas.getString("deposito"));
        // Usuario exige el hash; el creador puede estar inactivo porque es una referencia histórica.
        var creador = new Usuario(filas.getInt("usuario_id"), filas.getString("creador"),
                filas.getString("usuario_login"), filas.getString("password_hash"), filas.getBoolean("activo"));
        var orden = new OrdenTrabajo(
                filas.getInt("orden_id"),
                filas.getString("numero_orden"),
                filas.getObject("fecha", LocalDate.class),
                new Labor(filas.getInt("labor_id"), filas.getString("labor")),
                new Establecimiento(filas.getInt("establecimiento_id"), filas.getString("establecimiento")),
                new Contratista(filas.getInt("contratista_id"), filas.getString("contratista")),
                deposito,
                filas.getString("remito_numero"),
                new Campania(filas.getInt("campana_id"), filas.getString("campana")),
                filas.getString("convenio"),
                filas.getString("comentarios"),
                creador);
        // CU-05 solo necesita la fecha de alta; la modificación nueva se registra al guardar.
        orden.restaurarAuditoria(filas.getObject("creado_en", LocalDateTime.class), null, null, null, null);
        return orden;
    }
}
