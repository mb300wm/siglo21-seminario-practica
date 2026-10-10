package com.comp.spiapp.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una fila de la cronología de CU-07, con las columnas de la vista v_trazabilidad_campana.
 * Es el resultado de una consulta, no una entidad del dominio.
 */
public record EtapaTrazabilidad(
    int establecimientoId,
    String establecimiento,
    int loteId,
    String lote,
    int ordenEtapa,
    String etapa,
    LocalDate fecha,
    String detalle,
    BigDecimal cantidad,
    String unidad,
    String contratista,
    String numeroOrden,
    Integer siembraId
) {
    /** Valores de orden_etapa en la vista: desempatan las etapas de una misma fecha. */
    public static final int SIEMBRA = 1;
    public static final int TRABAJO = 2;
    public static final int COSECHA = 3;

    public boolean esSiembra() {
        return ordenEtapa == SIEMBRA;
    }

    public boolean esCosecha() {
        return ordenEtapa == COSECHA;
    }
}
