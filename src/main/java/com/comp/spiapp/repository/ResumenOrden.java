package com.comp.spiapp.repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * ResumenOrden
 */
public record ResumenOrden(
    int id,
    String numero,
    LocalDate fecha,
    String labor,
    String establecimiento
) {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override 
    public String toString() {
        return numero + " · " + fecha.format(FECHA) + " · " + labor + " · " + establecimiento;
    }
}
