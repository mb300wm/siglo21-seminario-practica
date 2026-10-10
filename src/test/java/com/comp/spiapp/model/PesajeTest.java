package com.comp.spiapp.model;

import static com.comp.spiapp.model.Datos.bd;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PesajeTest {
    @Test
    void calculaElNetoConDecimalesExactos() throws ValidacionException {
        Pesaje pesaje = new Pesaje(null, 1, bd("30000.30"), bd("10000.10"));
        assertEquals(bd("20000.20"), pesaje.calcularNeto());
    }

    @Test
    void normalizaLaEscalaALaDeLaColumna() throws ValidacionException {
        Pesaje pesaje = new Pesaje(null, 1, bd("1000.5"), bd("400"));
        assertEquals("1000.50", pesaje.getPesoBruto().toPlainString());
        assertEquals("600.50", pesaje.calcularNeto().toPlainString());
    }

    @Test
    void rechazaTaraIgualOMayorQueElBruto() {
        assertThrows(ValidacionException.class, () -> new Pesaje(null, 1, bd("100"), bd("100")));
        assertThrows(ValidacionException.class, () -> new Pesaje(null, 1, bd("100"), bd("150")));
    }

    @Test
    void rechazaPesosNoPositivos() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> new Pesaje(null, 1, bd("0"), bd("-5")));
        assertEquals(2, e.getErrores().size());
    }

    @Test
    void rechazaOrdenDeLlegadaInvalido() {
        assertThrows(ValidacionException.class, () -> new Pesaje(null, 0, bd("100"), bd("50")));
    }
}
