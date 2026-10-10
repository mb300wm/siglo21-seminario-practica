package com.comp.spiapp.repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.comp.spiapp.model.Campania;
import com.comp.spiapp.model.Cultivo;
import com.comp.spiapp.model.Establecimiento;
import com.comp.spiapp.model.GrupoInsumo;
import com.comp.spiapp.model.Insumo;
import com.comp.spiapp.model.Lote;
import com.comp.spiapp.model.Siembra;
import com.comp.spiapp.model.Usuario;
import com.comp.spiapp.model.ValidacionException;

public final class SiembraRepository {
    public int insertar(Siembra siembra, Connection conexion) throws SQLException {
        String sql = """
                INSERT INTO siembras (
                    campana_id, lote_id, semilla_insumo_id, fecha_inicio, fecha_fin,
                    hectareas_monitor, tractor, marcha_trabajo, velocidad_siembra, sembradora,
                    engranaje_conductor, engranaje_conducido, placa, grano_por_metro, semilla_total_kg,
                    calibre, desvio, tipo_fertilizante, fertilizante_kg_ha, marcha_caja_fertilizante,
                    comentarios, creado_por, creado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (var sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            new Parametros(sentencia)
                    .id(siembra.getCampania())                     // campana_id
                    .id(siembra.getLote())                         // lote_id
                    .id(siembra.getSemilla())                      // semilla_insumo_id
                    .fecha(siembra.getFechaInicio())               // fecha_inicio
                    .fecha(siembra.getFechaFin())                  // fecha_fin
                    .decimal(siembra.getHectareasMonitor())        // hectareas_monitor
                    .texto(siembra.getTractor())                   // tractor
                    .entero(siembra.getMarchaTrabajo())            // marcha_trabajo
                    .decimal(siembra.getVelocidadSiembra())        // velocidad_siembra
                    .texto(siembra.getSembradora())                // sembradora
                    .entero(siembra.getEngranajeConductor())       // engranaje_conductor
                    .entero(siembra.getEngranajeConducido())       // engranaje_conducido
                    .texto(siembra.getPlaca())                     // placa
                    .decimal(siembra.getGranoPorMetro())           // grano_por_metro
                    .decimal(siembra.getSemillaTotalKg())          // semilla_total_kg
                    .decimal(siembra.getCalibre())                 // calibre
                    .decimal(siembra.getDesvio())                  // desvio
                    .texto(siembra.getTipoFertilizante())          // tipo_fertilizante
                    .decimal(siembra.getFertilizanteKgHa())        // fertilizante_kg_ha
                    .entero(siembra.getMarchaCajaFertilizante())   // marcha_caja_fertilizante
                    .texto(siembra.getComentarios())               // comentarios
                    .id(siembra.getCreadoPor())                    // creado_por
                    .momento(siembra.getCreadoEn());               // creado_en
            sentencia.executeUpdate();
            return Claves.generada(sentencia);
        }
    }

    public List<Siembra> listarVigentes(Connection conexion) throws SQLException {
        String sql = """
                SELECT s.siembra_id, s.fecha_inicio, s.fecha_fin, s.hectareas_monitor, s.tractor,
                       s.marcha_trabajo, s.velocidad_siembra, s.sembradora, s.engranaje_conductor,
                       s.engranaje_conducido, s.placa, s.grano_por_metro, s.semilla_total_kg, s.calibre,
                       s.desvio, s.tipo_fertilizante, s.fertilizante_kg_ha, s.marcha_caja_fertilizante,
                       s.comentarios, s.creado_en,
                       ca.campana_id, ca.nombre AS campana,
                       e.establecimiento_id, e.nombre AS establecimiento,
                       l.lote_id, l.nombre AS lote, l.hectareas AS lote_hectareas,
                       i.insumo_id, i.nombre AS semilla, g.grupo_id, g.nombre AS grupo,
                       cu.cultivo_id, cu.nombre AS cultivo,
                       u.usuario_id, u.nombre AS creador, u.usuario_login, u.password_hash, u.activo
                FROM siembras s
                JOIN campanas ca ON ca.campana_id = s.campana_id
                JOIN lotes l ON l.lote_id = s.lote_id
                JOIN establecimientos e ON e.establecimiento_id = l.establecimiento_id
                JOIN insumos i ON i.insumo_id = s.semilla_insumo_id
                JOIN grupos_insumo g ON g.grupo_id = i.grupo_id
                JOIN cultivos cu ON cu.cultivo_id = g.cultivo_id
                JOIN usuarios u ON u.usuario_id = s.creado_por
                WHERE s.eliminado_en IS NULL
                ORDER BY s.fecha_inicio DESC, s.siembra_id DESC
                """;
        List<Siembra> siembras = new ArrayList<>();
        try (var consulta = conexion.prepareStatement(sql); var filas = consulta.executeQuery()) {
            while (filas.next()) {
                siembras.add(mapear(filas));
            }
        } catch (ValidacionException ex) {
            throw new SQLException("Una siembra guardada contiene datos inválidos.", ex);
        }
        return siembras;
    }

    private Siembra mapear(ResultSet filas) throws SQLException, ValidacionException {
        Establecimiento establecimiento = new Establecimiento(
                filas.getInt("establecimiento_id"),
                filas.getString("establecimiento"));

        Lote lote = new Lote(
                filas.getInt("lote_id"),
                filas.getString("lote"),
                establecimiento,
                filas.getBigDecimal("lote_hectareas"));

        Cultivo cultivo = new Cultivo(
                filas.getInt("cultivo_id"),
                filas.getString("cultivo"));

        GrupoInsumo grupo = new GrupoInsumo(
                filas.getInt("grupo_id"),
                filas.getString("grupo"),
                cultivo);

        Insumo semilla = new Insumo(
                filas.getInt("insumo_id"),
                filas.getString("semilla"),
                grupo,
                null);

        // Usuario exige el hash; el creador puede estar inactivo porque
        // representa una referencia histórica.
        Usuario creador = new Usuario(
                filas.getInt("usuario_id"),
                filas.getString("creador"),
                filas.getString("usuario_login"),
                filas.getString("password_hash"),
                filas.getBoolean("activo"));

        Campania campania = new Campania(
                filas.getInt("campana_id"),
                filas.getString("campana"));

        Siembra siembra = new Siembra(
                filas.getInt("siembra_id"),
                campania,
                lote,
                semilla,
                filas.getObject("fecha_inicio", LocalDate.class),
                filas.getObject("fecha_fin", LocalDate.class),
                filas.getBigDecimal("semilla_total_kg"),
                filas.getBigDecimal("hectareas_monitor"),
                filas.getString("tractor"),
                filas.getString("sembradora"),
                filas.getObject("marcha_trabajo", Integer.class),
                filas.getBigDecimal("velocidad_siembra"),
                filas.getObject("engranaje_conductor", Integer.class),
                filas.getObject("engranaje_conducido", Integer.class),
                filas.getString("placa"),
                filas.getBigDecimal("grano_por_metro"),
                filas.getBigDecimal("calibre"),
                filas.getBigDecimal("desvio"),
                filas.getString("tipo_fertilizante"),
                filas.getBigDecimal("fertilizante_kg_ha"),
                filas.getObject("marcha_caja_fertilizante", Integer.class),
                filas.getString("comentarios"),
                creador);

        // En esta fase solo interesa la fecha de creación.
        siembra.restaurarAuditoria(
                filas.getObject("creado_en", LocalDateTime.class),
                null,
                null,
                null,
                null);

        return siembra;
    }
}
