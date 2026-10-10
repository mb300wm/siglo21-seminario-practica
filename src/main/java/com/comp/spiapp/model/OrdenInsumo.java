package com.comp.spiapp.model;

import java.math.BigDecimal;

/**
 * OrdenInsumo
 * Insumo que se remite para una orden de trabajo
 */
public final class OrdenInsumo extends Entidad {
    private final Insumo insumo;
    private final BigDecimal cantidadARemitir;
    private final String unidad;

    public OrdenInsumo(Integer id, Insumo insumo, BigDecimal cantidadARemitir, String unidad)
        throws ValidacionException {
        super(id);
        this.insumo = insumo;
        this.cantidadARemitir = Decimales.escalar(cantidadARemitir, Decimales.ESCALA_CANTIDAD);
        this.unidad = Errores.limpiar(unidad);
        new Errores()
            .obligatorio(insumo, "El insumo")
            .positivoOpcional(this.cantidadARemitir, "La cantidad a remitir")
            .lanzarSiHay();
    }

    public Insumo getInsumo() {
        return insumo;
    }
    public BigDecimal getCantidadARemitir() {
        return cantidadARemitir;
    }
    public String getUnidad() {
        return unidad;
    }
}
