package com.comp.spiapp.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.comp.spiapp.model.Siembra;

/**
 * Presentación de números y fechas como se escriben en las hojas de campo: 42.000,5 y 15/10/2025
 */
final class Formato {
    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formato() {
    }

    static String numero(BigDecimal valor) {
        return new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(ARGENTINA)).format(valor);
    }

    /** Las cantidades de insumo y la dosis usan tres decimales, como en el SQL. */
    static String numero(BigDecimal valor, int decimales) {
        String patron = "#,##0." + "#".repeat(decimales);
        return new DecimalFormat(patron, DecimalFormatSymbols.getInstance(ARGENTINA)).format(valor);
    }

    /** Valor para volver a editar en un campo: sin separador de miles y con coma decimal. */
    static String editable(BigDecimal valor) {
        return valor == null ? "" : valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    static String fecha(LocalDate fecha) {
        return fecha.format(FECHA);
    }

    static String siembra(Siembra siembra) {
        return siembra.getCampania().getNombre() + " · " + siembra.getLote().getEstablecimiento().getNombre()
                + " · " + siembra.getLote().getNombre() + " · " + fecha(siembra.getFechaInicio())
                + " · " + siembra.getSemilla().getNombre();
    }
}
