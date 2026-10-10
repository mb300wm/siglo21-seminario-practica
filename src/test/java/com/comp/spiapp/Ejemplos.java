package com.comp.spiapp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comp.spiapp.model.Campania;
import com.comp.spiapp.model.Cultivo;
import com.comp.spiapp.model.Establecimiento;
import com.comp.spiapp.model.GrupoInsumo;
import com.comp.spiapp.model.Insumo;
import com.comp.spiapp.model.Lote;
import com.comp.spiapp.model.Siembra;
import com.comp.spiapp.model.Usuario;
import com.comp.spiapp.model.ValidacionException;

/** Datos de ejemplo tomados de 02_datos_de_prueba.sql, como objetos y como filas de JdbcDoble. */
public final class Ejemplos {
    public static final LocalDate INICIO_SIEMBRA = LocalDate.of(2025, 10, 15);

    private Ejemplos() {
    }

    public static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    public static Usuario usuario() throws ValidacionException {
        return new Usuario(7, "Mateo", "mateo", "hash-de-prueba", true);
    }

    public static Campania campania() throws ValidacionException {
        return new Campania(2, "25-26");
    }

    public static Lote lote() throws ValidacionException {
        return new Lote(1, "6 NORTE", new Establecimiento(1, "ESTANCIA CHARRA"), bd("20"));
    }

    public static Insumo semilla() throws ValidacionException {
        return new Insumo(1, "Maíz A", new GrupoInsumo(1, "Semillas de maíz", new Cultivo(1, "Maíz")), null);
    }

    /** Una siembra ya guardada (con id), como la que se elige al registrar la cosecha. */
    public static Siembra siembraGuardada() throws ValidacionException {
        return new Siembra(10, campania(), lote(), semilla(), INICIO_SIEMBRA, INICIO_SIEMBRA.plusDays(1),
                bd("600"), bd("20"), null, null, usuario());
    }

    /** Respuestas de las consultas de catálogos de siembra y cosecha. */
    public static JdbcDoble conCatalogos(JdbcDoble jdbc) {
        return jdbc
                .resultado("FROM campanas", List.of(
                        Map.of("campana_id", 2, "nombre", "25-26"),
                        Map.of("campana_id", 1, "nombre", "24-25")))
                .resultado("FROM establecimientos", List.of(
                        Map.of("establecimiento_id", 1, "nombre", "ESTANCIA CHARRA"),
                        Map.of("establecimiento_id", 2, "nombre", "EL CARMEN")))
                .resultado("FROM lotes", List.of(
                        lote(1, 1, "6 NORTE", bd("20.00")),
                        lote(2, 1, "7 NORTE", bd("15.00")),
                        lote(5, 2, "EL BAJO", null)))
                .resultado("FROM insumos i", List.of(
                        Map.of("insumo_id", 1, "nombre", "Maíz A", "grupo_id", 1, "grupo", "Semillas de maíz",
                                "cultivo_id", 1, "cultivo", "Maíz")))
                .resultado("FROM acopios", List.of(
                        Map.of("acopio_id", 1, "nombre", "Silo propio")))
                .resultado("FROM siembras s", List.of(filaSiembra()));
    }

    /**
     * Catálogos de la orden y una orden guardada, 25-26-0001 en ESTANCIA CHARRA: 6 NORTE (20 ha) y
     * 7 NORTE (15 ha), 70 L de Herbicida de prueba aplicados 40 L y 30 L. El último correlativo
     * de 25-26 es 6, como en 02_datos_de_prueba.sql.
     */
    public static JdbcDoble conOrdenes(JdbcDoble jdbc) {
        conCatalogos(jdbc)
                .resultado("FROM labores", List.of(fila("id", 1, "nombre", "Pulverización terrestre")))
                .resultado("FROM contratistas", List.of(fila("id", 1, "nombre", "Contratista de prueba")))
                .resultado("FROM depositos", List.of(fila("id", 1, "nombre", "Depósito central")))
                .resultado("FROM cultivos ORDER BY", List.of(fila("id", 1, "nombre", "Maíz")))
                .resultado("ORDER BY g.nombre, i.nombre", List.of(
                        fila("insumo_id", 7, "nombre", "Herbicida de prueba", "grupo_id", 5, "grupo", "Herbicidas",
                                "cultivo_id", null, "cultivo", null)))
                .resultado("campana_id = ? FOR UPDATE", List.of(fila("nombre", "25-26")))
                .resultado("MAX(CAST", List.of(fila("ultimo", 6)))
                .resultado("ORDER BY ot.fecha DESC", List.of(fila("orden_id", 4, "numero_orden", "25-26-0001",
                        "fecha", LocalDate.of(2025, 11, 10), "labor", "Pulverización terrestre",
                        "establecimiento", "ESTANCIA CHARRA")))
                .resultado("WHERE ot.orden_id = ?", List.of(filaOrden()))
                .resultado("FROM orden_lotes ol", List.of(
                        filaOrdenLote(11, 1, "6 NORTE", "20.00"),
                        filaOrdenLote(12, 2, "7 NORTE", "15.00")))
                .resultado("FROM orden_insumos oi", List.of(fila("orden_insumo_id", 21,
                        "cantidad_a_remitir", bd("70.000"), "unidad", "L", "insumo_id", 7,
                        "nombre", "Herbicida de prueba", "grupo_id", 5, "grupo", "Herbicidas",
                        "cultivo_id", null, "cultivo", null)))
                .resultado("FROM orden_lote_insumos", List.of(
                        fila("id", 31, "orden_lote_id", 11, "orden_insumo_id", 21, "total", bd("40.000")),
                        fila("id", 32, "orden_lote_id", 12, "orden_insumo_id", 21, "total", bd("30.000"))));
        return jdbc;
    }

    /**
     * Escenario de P08 en la campaña 25-26:
     * 6 NORTE tiene dos ciclos (Maíz A cosechado en 20 ha, Maíz B todavía sin cosechar) y una aplicación;
     * 7 NORTE solo tiene una labranza sin insumos; EL BAJO no tiene operaciones.
     * Las filas vienen desordenadas a propósito: el servicio tiene que ordenarlas.
     */
    public static JdbcDoble conTrazabilidad(JdbcDoble jdbc) {
        return jdbc
                .resultado("FROM v_trazabilidad_campana", List.of(
                        etapa(1, "6 NORTE", 3, "Cosecha", LocalDate.of(2026, 3, 20), "Humedad: 14.00%",
                                "42000.00", "kg", null, null, 10),
                        etapa(1, "6 NORTE", 1, "Siembra", LocalDate.of(2025, 12, 20), "Semilla: Maíz B",
                                "300.00", "kg", null, null, 13),
                        etapa(1, "6 NORTE", 1, "Siembra", INICIO_SIEMBRA, "Semilla: Maíz A",
                                "600.00", "kg", null, null, 10),
                        etapa(1, "6 NORTE", 2, "Trabajo - Pulverización terrestre", LocalDate.of(2025, 11, 10),
                                "Herbicida de prueba", "40.000", "L", "Contratista de prueba", "25-26-0001", null)))
                .resultado("NOT EXISTS (SELECT 1 FROM orden_lote_insumos", List.of(
                        etapa(2, "7 NORTE", 2, "Trabajo - Labranza", LocalDate.of(2025, 9, 20), "Sin insumos",
                                null, null, "AgroServicios del Sur", "25-26-0002", null)))
                .resultado("FROM cosechas co", List.of(
                        fila("siembra_id", 10, "fecha_inicio", LocalDate.of(2026, 3, 20), "hectareas", bd("20.00"))));
    }

    private static Map<String, Object> etapa(int loteId, String lote, int ordenEtapa, String etapa, LocalDate fecha,
                                             String detalle, String cantidad, String unidad, String contratista,
                                             String numeroOrden, Integer siembraId) {
        return fila("establecimiento_id", 1, "establecimiento", "ESTANCIA CHARRA", "lote_id", loteId,
                "lote", lote, "orden_etapa", ordenEtapa, "etapa", etapa, "fecha", fecha, "detalle", detalle,
                "cantidad", cantidad == null ? null : bd(cantidad), "unidad", unidad, "contratista", contratista,
                "numero_orden", numeroOrden, "siembra_id", siembraId);
    }

    private static Map<String, Object> filaOrden() {
        return fila("orden_id", 4, "numero_orden", "25-26-0001", "fecha", LocalDate.of(2025, 11, 10),
                "remito_numero", "REMITO-2025-001", "convenio", null, "comentarios", null,
                "creado_en", LocalDateTime.of(2025, 11, 10, 9, 0),
                "campana_id", 2, "campana", "25-26", "labor_id", 1, "labor", "Pulverización terrestre",
                "establecimiento_id", 1, "establecimiento", "ESTANCIA CHARRA",
                "contratista_id", 1, "contratista", "Contratista de prueba",
                "deposito_id", 1, "deposito", "Depósito central",
                "usuario_id", 2, "creador", "Ingeniero de prueba", "usuario_login", "demo_ingeniero",
                "password_hash", "SIN_LOGIN_DATOS_DE_PRUEBA", "activo", false);
    }

    private static Map<String, Object> filaOrdenLote(int id, int loteId, String lote, String superficie) {
        return fila("orden_lote_id", id, "superficie_ha", bd(superficie), "lote_id", loteId, "nombre", lote,
                "hectareas", bd(superficie), "cultivo_id", 1, "cultivo", "Maíz");
    }

    /** Fila con pares columna, valor; admite null (Map.of no). */
    private static Map<String, Object> fila(Object... columnasYValores) {
        Map<String, Object> fila = new HashMap<>();
        for (int i = 0; i < columnasYValores.length; i += 2) {
            fila.put((String) columnasYValores[i], columnasYValores[i + 1]);
        }
        return fila;
    }

    private static Map<String, Object> lote(int id, int establecimiento, String nombre, BigDecimal hectareas) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("lote_id", id);
        fila.put("establecimiento_id", establecimiento);
        fila.put("nombre", nombre);
        fila.put("hectareas", hectareas);
        return fila;
    }

    /** Siembra 10 con datos mínimos: los técnicos opcionales vienen en NULL. */
    public static Map<String, Object> filaSiembra() {
        Map<String, Object> fila = new HashMap<>();
        fila.put("siembra_id", 10);
        fila.put("fecha_inicio", INICIO_SIEMBRA);
        fila.put("fecha_fin", INICIO_SIEMBRA.plusDays(1));
        fila.put("semilla_total_kg", bd("600.00"));
        fila.put("creado_en", LocalDateTime.of(2025, 10, 16, 18, 30));
        for (String columna : List.of("hectareas_monitor", "tractor", "marcha_trabajo", "velocidad_siembra",
                "sembradora", "engranaje_conductor", "engranaje_conducido", "placa", "grano_por_metro", "calibre",
                "desvio", "tipo_fertilizante", "fertilizante_kg_ha", "marcha_caja_fertilizante", "comentarios")) {
            fila.put(columna, null);
        }
        fila.put("campana_id", 2);
        fila.put("campana", "25-26");
        fila.put("establecimiento_id", 1);
        fila.put("establecimiento", "ESTANCIA CHARRA");
        fila.put("lote_id", 1);
        fila.put("lote", "6 NORTE");
        fila.put("lote_hectareas", bd("20.00"));
        fila.put("insumo_id", 1);
        fila.put("semilla", "Maíz A");
        fila.put("grupo_id", 1);
        fila.put("grupo", "Semillas de maíz");
        fila.put("cultivo_id", 1);
        fila.put("cultivo", "Maíz");
        fila.put("usuario_id", 1);
        fila.put("creador", "Encargado de prueba");
        fila.put("usuario_login", "demo_encargado");
        fila.put("password_hash", "SIN_LOGIN_DATOS_DE_PRUEBA");
        fila.put("activo", false);
        return fila;
    }
}
