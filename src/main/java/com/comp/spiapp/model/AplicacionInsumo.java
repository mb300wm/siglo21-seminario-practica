package com.comp.spiapp.model;

import java.math.BigDecimal;

/**
 * Total de un insumo aplicado en un lote de la orden
 */
public final class AplicacionInsumo extends Entidad {
    private final OrdenLote ordenLote;
    private final OrdenInsumo ordenInsumo;
    private final BigDecimal total;

    public AplicacionInsumo(
        Integer id, 
        OrdenLote ordenLote, 
        OrdenInsumo ordenInsumo, 
        BigDecimal total
    ) throws ValidacionException {
        super(id);
        this.ordenLote = ordenLote;
        this.ordenInsumo = ordenInsumo;
        this.total = Decimales.escalar(total, Decimales.ESCALA_CANTIDAD);
        validar();
    }

    public void validar() throws ValidacionException {
        new Errores()
            .obligatorio(ordenLote, "El lote de la orden")
            .obligatorio(ordenInsumo, "El insumo de la orden")
            .positivo(total, "El total aplicado")
            .si(ordenLote != null && (ordenLote.getSuperficieHa() == null
                    || ordenLote.getSuperficieHa().signum() <= 0),
                    "La aplicación necesita una superficie mayor que cero para calcular la dosis.")
            .lanzarSiHay();
    }

    public BigDecimal calcularDosis() throws ValidacionException {
        BigDecimal superficie = ordenLote.getSuperficieHa();
        if (superficie == null || superficie.signum() <= 0) {
            throw new ValidacionException("No se puede calcular la dosis: la superficie del lote debe ser mayor que cero.");
        }
        return total.divide(superficie, Decimales.ESCALA_DOSIS, Decimales.REDONDEO);
    }

    public OrdenLote getOrdenLote() {
        return ordenLote;
    }
    public OrdenInsumo getOrdenInsumo() {
        return ordenInsumo;
    }
    public BigDecimal getTotal() {
        return total;
    }
}
