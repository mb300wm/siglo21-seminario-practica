package com.comp.spiapp.model;

import java.math.BigDecimal;

/**
 * Lote
 */
public final class Lote extends Catalogo {
    private final Establecimiento establecimiento;
    private final BigDecimal hectareas;

    public Lote(Integer id, String nombre, Establecimiento establecimiento, BigDecimal hectareas)
        throws ValidacionException{
        super(id, nombre);
        this.establecimiento = establecimiento;
        this.hectareas = Decimales.escalar(hectareas, Decimales.ESCALA_HECTAREAS);
        new Errores()
            .obligatorio(establecimiento, "El establecimiento")
            .positivoOpcional(this.hectareas, "Las hectáreas")
            .lanzarSiHay();
    }

    public Establecimiento getEstablecimiento(){
        return establecimiento;
    }

    public BigDecimal getHectareas() {
        return hectareas;
    }
}
