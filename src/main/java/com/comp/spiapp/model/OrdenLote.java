package com.comp.spiapp.model;

import java.math.BigDecimal;

/**
 * OrdenLote
 * Lote incluido en una orden de trabajo, con la superficie trabajada
 */
public final class OrdenLote extends Entidad {
    private final Lote lote;
    private final Cultivo cultivo;
    private final BigDecimal superficieHa;

    public OrdenLote(Integer id, Lote lote, Cultivo cultivo, BigDecimal superficieHa)
        throws ValidacionException {
        super(id);
        this.lote = lote;
        this.cultivo = cultivo;
        this.superficieHa = Decimales.escalar(superficieHa, Decimales.ESCALA_HECTAREAS);
        new Errores()
                .obligatorio(lote, "El lote")
                .positivoOpcional(this.superficieHa, "La superficie")
                .lanzarSiHay();
    }

    public Lote getLote() {
        return lote;
    }
    public Cultivo getCultivo() {
        return cultivo;
    }
    public BigDecimal getSuperficieHa() {
        return superficieHa;
    }
}
