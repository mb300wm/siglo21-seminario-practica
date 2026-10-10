package com.comp.spiapp.model;

import java.math.BigDecimal;

/**
 * Pesaje
 */
public final class Pesaje extends Entidad {
    private final int ordenLlegada;
    private final BigDecimal pesoBruto;
    private final BigDecimal pesoTara;

    public Pesaje(Integer id, int ordenLlegada, BigDecimal pesoBruto, BigDecimal pesoTara)
        throws ValidacionException {
        super(id);
        this.ordenLlegada = ordenLlegada;
        this.pesoBruto = Decimales.escalar(pesoBruto, Decimales.ESCALA_PESO);
        this.pesoTara = Decimales.escalar(pesoTara, Decimales.ESCALA_PESO);
        validar();
    }

    public void validar() throws ValidacionException {
        new Errores()
            .si(ordenLlegada <= 0, "El orden de llegada debe ser mayor que cero.")
            .positivo(pesoBruto, "El peso bruto")
            .positivo(pesoTara, "La tara")
            .si(pesoBruto != null && pesoTara != null
                && pesoBruto.signum() > 0 && pesoTara.signum() > 0
                && pesoBruto.compareTo(pesoTara) <= 0,
                "El peso bruto debe ser mayor que la tara.")
            .lanzarSiHay();
    }

    public BigDecimal calcularNeto() {
        return pesoBruto.subtract(pesoTara);
    }

    public int getOrdenLlegada() {
        return ordenLlegada;
    }
    public BigDecimal getPesoBruto() {
        return pesoBruto;
    }
    public BigDecimal getPesoTara() {
        return pesoTara;
    }
}
