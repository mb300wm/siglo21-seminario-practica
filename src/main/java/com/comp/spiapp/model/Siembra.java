package com.comp.spiapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Siembra
 */
public final class Siembra extends OperacionAuditada {
    private final Campania campania;
    private final Lote lote;
    private final Insumo semilla;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final BigDecimal semillaTotalKg;
    private final BigDecimal hectareasMonitor;
    private final String tractor;
    private final String sembradora;
    private final Integer marchaTrabajo;
    private final BigDecimal velocidadSiembra;
    private final Integer engranajeConductor;
    private final Integer engranajeConducido;
    private final String placa;
    private final BigDecimal granoPorMetro;
    private final BigDecimal calibre;
    private final BigDecimal desvio;
    private final String tipoFertilizante;
    private final BigDecimal fertilizanteKgHa;
    private final Integer marchaCajaFertilizante;
    private final String comentarios;

    public Siembra(
            Integer id,
            Campania campania,
            Lote lote,
            Insumo semilla,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal semillaTotalKg,
            BigDecimal hectareasMonitor,
            String tractor,
            String sembrador,
            Usuario creadoPor) throws ValidacionException {
        this(id, campania, lote, semilla, fechaInicio, fechaFin, semillaTotalKg,
                hectareasMonitor, tractor, sembrador, null, null, null, null, null,
                null, null, null, null, null, null, null, creadoPor);
    }

    /**
     * Construye una siembra con todos los campos opcionales de la hoja de campo.
     */
    public Siembra(Integer id, Campania campania, Lote lote, Insumo semilla,
            LocalDate fechaInicio, LocalDate fechaFin, BigDecimal semillaTotalKg,
            BigDecimal hectareasMonitor, String tractor, String sembradora,
            Integer marchaTrabajo, BigDecimal velocidadSiembra,
            Integer engranajeConductor, Integer engranajeConducido, String placa,
            BigDecimal granoPorMetro, BigDecimal calibre, BigDecimal desvio,
            String tipoFertilizante, BigDecimal fertilizanteKgHa,
            Integer marchaCajaFertilizante, String comentarios, Usuario creadoPor)
            throws ValidacionException {
        super(id, creadoPor);
        this.campania = campania;
        this.lote = lote;
        this.semilla = semilla;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.semillaTotalKg = Decimales.escalar(semillaTotalKg, Decimales.ESCALA_PESO);
        this.hectareasMonitor = Decimales.escalar(hectareasMonitor, Decimales.ESCALA_HECTAREAS);
        this.tractor = Errores.limpiar(tractor);
        this.sembradora = Errores.limpiar(sembradora);
        this.marchaTrabajo = marchaTrabajo;
        this.velocidadSiembra = Decimales.escalar(velocidadSiembra, 2);
        this.engranajeConductor = engranajeConductor;
        this.engranajeConducido = engranajeConducido;
        this.placa = Errores.limpiar(placa);
        this.granoPorMetro = Decimales.escalar(granoPorMetro, 2);
        this.calibre = Decimales.escalar(calibre, 2);
        this.desvio = Decimales.escalar(desvio, 2);
        this.tipoFertilizante = Errores.limpiar(tipoFertilizante);
        this.fertilizanteKgHa = Decimales.escalar(fertilizanteKgHa, 2);
        this.marchaCajaFertilizante = marchaCajaFertilizante;
        this.comentarios = Errores.limpiar(comentarios);
        validar();
    }

    @Override
    public void validar() throws ValidacionException {
        new Errores()
                .obligatorio(campania, "La campaña")
                .obligatorio(lote, "El lote")
                .obligatorio(semilla, "La semilla")
                .obligatorio(fechaInicio, "La fecha de inicio")
                .obligatorio(fechaFin, "La fecha de fin")
                .si(semilla != null && !semilla.esSemilla(), "El insumo seleccionado no es una semilla.")
                .si(fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio),
                        "La fecha de fin no puede ser anterior a la fecha de inicio.")
                .positivo(semillaTotalKg, "La semilla total (kg)")
                .positivoOpcional(hectareasMonitor, "Las hectáreas del monitor")
                .lanzarSiHay();
    }

    public Campania getCampania() {
        return campania;
    }

    public Lote getLote() {
        return lote;
    }

    public Insumo getSemilla() {
        return semilla;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public BigDecimal getSemillaTotalKg() {
        return semillaTotalKg;
    }

    public BigDecimal getHectareasMonitor() {
        return hectareasMonitor;
    }

    public String getTractor() {
        return tractor;
    }

    public String getSembradora() {
        return sembradora;
    }

    public Integer getMarchaTrabajo() {
        return marchaTrabajo;
    }

    public BigDecimal getVelocidadSiembra() {
        return velocidadSiembra;
    }

    public Integer getEngranajeConductor() {
        return engranajeConductor;
    }

    public Integer getEngranajeConducido() {
        return engranajeConducido;
    }

    public String getPlaca() {
        return placa;
    }

    public BigDecimal getGranoPorMetro() {
        return granoPorMetro;
    }

    public BigDecimal getCalibre() {
        return calibre;
    }

    public BigDecimal getDesvio() {
        return desvio;
    }

    public String getTipoFertilizante() {
        return tipoFertilizante;
    }

    public BigDecimal getFertilizanteKgHa() {
        return fertilizanteKgHa;
    }

    public Integer getMarchaCajaFertilizante() {
        return marchaCajaFertilizante;
    }

    public String getComentarios() {
        return comentarios;
    }
}
