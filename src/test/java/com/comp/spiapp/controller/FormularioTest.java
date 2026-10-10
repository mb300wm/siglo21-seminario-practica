package com.comp.spiapp.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FormularioTest {
    @Test
    void aceptaComaOPuntoDecimalYDejaSinDatoLoVacio() {
        assertEquals(new BigDecimal("20.5"), Formulario.leerDecimal("20,5"));
        assertEquals(new BigDecimal("20.5"), Formulario.leerDecimal("20.5"));
        assertEquals(new BigDecimal("600"), Formulario.leerDecimal("  600 "));
        assertNull(Formulario.leerDecimal("   "));
        assertNull(Formulario.leerDecimal(null));
        assertEquals(17, Formulario.leerEntero(" 17 "));
        assertNull(Formulario.leerEntero(""));
    }

    @Test
    void rechazaTextoYSeparadorDeMiles() {
        assertThrows(NumberFormatException.class, () -> Formulario.leerDecimal("abc"));
        assertThrows(NumberFormatException.class, () -> Formulario.leerDecimal("1.234,5"));
        assertThrows(NumberFormatException.class, () -> Formulario.leerEntero("4,5"));
    }

    @Test
    void muestraLosNumerosComoEnLasHojasDeCampo() {
        assertEquals("42.000", Formato.numero(new BigDecimal("42000.00")));
        assertEquals("2.100", Formato.numero(new BigDecimal("2100.0")));
        assertEquals("1.833,3", Formato.numero(new BigDecimal("1833.3")));
        assertEquals("0,01", Formato.numero(new BigDecimal("0.01")));
    }
}
