package com.comp.spiapp.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 
 * Cosecha
 */
public final class Cosecha extends OperacionAuditada {
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final Siembra siembra;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final BigDecimal hectareas;
    private final BigDecimal humedad;
    private final Acopio acopio;
    private final String comentarios;
    private final List<Pesaje> pesajes = new ArrayList<>();
    
    public Cosecha(
        Integer id, 
        Siembra siembra, 
        LocalDate fechaInicio, 
        LocalDate fechaFin,
        BigDecimal hectareas, 
        BigDecimal humedad, 
        Acopio acopio, 
        Usuario creadoPor
    ) throws ValidacionException {
        this(id, siembra, fechaInicio, fechaFin, hectareas, humedad, acopio, null, creadoPor);
    }

    public Cosecha(Integer id, Siembra siembra, LocalDate fechaInicio, LocalDate fechaFin,
                   BigDecimal hectareas, BigDecimal humedad, Acopio acopio,
                   String comentarios, Usuario creadoPor) throws ValidacionException {
        super(id, creadoPor);
        this.siembra = siembra;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.hectareas = Decimales.escalar(hectareas, Decimales.ESCALA_HECTAREAS);
        this.humedad = Decimales.escalar(humedad, Decimales.ESCALA_HUMEDAD);
        this.acopio = acopio;
        this.comentarios = Errores.limpiar(comentarios);
        revisarCampos().lanzarSiHay();
    }

    private Errores revisarCampos() {
        return new Errores()
            .obligatorio(siembra, "La siembra")
            .obligatorio(fechaInicio, "La fecha de inicio")
            .obligatorio(fechaFin, "La fecha de fin")
            .si(fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio),
                    "La fecha de fin no puede ser anterior a la fecha de inicio.")
            .positivoOpcional(hectareas, "Las hectáreas")
            .si(humedad != null && (humedad.signum() < 0 || humedad.compareTo(CIEN) > 0),
                    "La humedad debe estar entre 0 y 100.");
    }


    @Override
    public void validar() throws ValidacionException {
        revisarCampos()
            .si(pesajes.isEmpty(), "Debe agregarse al menos un pesaje.")
            .lanzarSiHay();
    }

    public void agregarPesaje(Pesaje pesaje) throws ValidacionException {
        new Errores()
            .obligatorio(pesaje, "El pesaje")
            .si(pesaje != null && pesajes.stream().anyMatch(p -> p.getOrdenLlegada() == pesaje.getOrdenLlegada()),
                    "Ya existe un pesaje con ese orden de llegada.")
            .lanzarSiHay();
        pesajes.add(pesaje);
    }

    public Pesaje registrarPesaje(BigDecimal pesoBruto, BigDecimal pesoTara) throws ValidacionException {
        int siguiente = pesajes.stream().mapToInt(Pesaje::getOrdenLlegada).max().orElse(0) + 1;
        Pesaje pesaje = new Pesaje(null, siguiente, pesoBruto, pesoTara);
        pesajes.add(pesaje);
        return pesaje;
    }

    public BigDecimal calcularPesoTotal() {
        BigDecimal total = BigDecimal.ZERO.setScale(Decimales.ESCALA_PESO, RoundingMode.HALF_UP);
        for (Pesaje pesaje : pesajes) {
            total = total.add(pesaje.calcularNeto());
        }
        return total;
    }

    public Optional<BigDecimal> calcularRinde() {
        if (hectareas == null || hectareas.signum() <= 0) {
            return Optional.empty();
        }
        return Optional.of(calcularPesoTotal().divide(hectareas, Decimales.ESCALA_RINDE, Decimales.REDONDEO));
    }

    public Siembra getSiembra() {
        return siembra;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public BigDecimal getHectareas() {
        return hectareas;
    }

    public BigDecimal getHumedad() {
        return humedad;
    }

    public Acopio getAcopio() {
        return acopio;
    }

    public String getComentarios() { return comentarios; }

    public List<Pesaje> getPesajes() {
        return Collections.unmodifiableList(pesajes);
    }
}
