package com.comp.spiapp.model;

import static com.comp.spiapp.model.Datos.bd;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CosechaTest {
    @Test 
    void sumaLosNetosDeLosPesajes() throws ValidacionException {
        Cosecha cosecha = Datos.cosecha("20");
        cosecha.registrarPesaje(bd("30000.10"), bd("10000.05"));
        cosecha.registrarPesaje(bd("25000.20"), bd("8000.10"));
        assertEquals(bd("37000.15"), cosecha.calcularPesoTotal());
    }

    @Test
    void calculaElRindePorHectarea() throws ValidacionException {
        Cosecha cosecha = Datos.cosecha("20");
        cosecha.registrarPesaje(bd("30000"), bd("10000"));
        cosecha.registrarPesaje(bd("30000"), bd("8000"));
        assertEquals(bd("42000.00"), cosecha.calcularPesoTotal());
        assertEquals(bd("2100.0"), cosecha.calcularRinde().orElseThrow());
    }

    @Test
    void redondeaElRindeHaciaArribaEnLaMitad() throws ValidacionException {
        Cosecha cosecha = Datos.cosecha("3");
        cosecha.registrarPesaje(bd("100.15"), bd("50.00"));   // neto 50.15 / 3 = 16.7166...
        assertEquals(bd("16.7"), cosecha.calcularRinde().orElseThrow());
        Cosecha otra = Datos.cosecha("4");
        otra.registrarPesaje(bd("100.50"), bd("50.00"));       // 50.50 / 4 = 12.625 -> 12.6
        assertEquals(bd("12.6"), otra.calcularRinde().orElseThrow());
        Cosecha mitad = Datos.cosecha("8");
        mitad.registrarPesaje(bd("100.70"), bd("50.00"));      // 50.70 / 8 = 6.3375 -> 6.3
        assertEquals(bd("6.3"), mitad.calcularRinde().orElseThrow());
        Cosecha exacta = Datos.cosecha("2");
        exacta.registrarPesaje(bd("100.50"), bd("50.00"));     // 50.50 / 2 = 25.25 -> 25.3
        assertEquals(bd("25.3"), exacta.calcularRinde().orElseThrow());
    }

    @Test
    void sinHectareasElRindeEsSinDato() throws ValidacionException {
        Cosecha cosecha = Datos.cosecha(null);
        cosecha.registrarPesaje(bd("30000"), bd("10000"));
        assertTrue(cosecha.calcularRinde().isEmpty());
    }

    @Test 
    void rechazaHectareasEnCero() {
        assertThrows(ValidacionException.class, () -> Datos.cosecha("0"));
    }

    @Test
    void humedadDebeEstarEntre0Y100() throws ValidacionException {
        Siembra siembra = Datos.siembra();
        for (String invalida : new String[]{"-0.01", "100.01"}) {
            assertThrows(ValidacionException.class, () -> new Cosecha(null, siembra, Datos.HOY, Datos.HOY,
                    null, bd(invalida), null, Datos.usuario()));
        }
        for (String valida : new String[]{"0", "100"}) {
            assertDoesNotThrow(() -> new Cosecha(null, siembra, Datos.HOY, Datos.HOY,
                    null, bd(valida), null, Datos.usuario()));
        }
    }

    @Test
    void rechazaFechaFinAnteriorAlInicio() throws ValidacionException {
        Siembra siembra = Datos.siembra();
        assertThrows(ValidacionException.class, () -> new Cosecha(null, siembra, Datos.HOY, Datos.HOY.minusDays(1),
                null, null, null, Datos.usuario()));
    }

    @Test
    void noSePuedeConfirmarSinPesajes() throws ValidacionException {
        Cosecha cosecha = Datos.cosecha("20");
        ValidacionException e = assertThrows(ValidacionException.class, cosecha::validar);
        assertEquals("Debe agregarse al menos un pesaje.", e.getMessage());
        cosecha.registrarPesaje(bd("100"), bd("40"));
        assertDoesNotThrow(cosecha::validar);
    }

    @Test
    void asignaElSiguienteOrdenDeLlegadaYRechazaRepetidos() throws ValidacionException {
        Cosecha cosecha = Datos.cosecha("20");
        assertEquals(1, cosecha.registrarPesaje(bd("100"), bd("40")).getOrdenLlegada());
        assertEquals(2, cosecha.registrarPesaje(bd("100"), bd("40")).getOrdenLlegada());
        Pesaje repetido = new Pesaje(null, 2, bd("100"), bd("40"));
        assertThrows(ValidacionException.class, () -> cosecha.agregarPesaje(repetido));
        assertEquals(2, cosecha.getPesajes().size());
    }

    @Test
    void unPesajeInvalidoNoSeIncorpora() throws ValidacionException {
        Cosecha cosecha = Datos.cosecha("20");
        assertThrows(ValidacionException.class, () -> cosecha.registrarPesaje(bd("40"), bd("100")));
        assertTrue(cosecha.getPesajes().isEmpty());
        assertEquals(BigDecimal.ZERO.setScale(2), cosecha.calcularPesoTotal());
    }

    @Test
    void laListaDePesajesNoSePuedeModificarDesdeAfuera() throws ValidacionException {
        Cosecha cosecha = Datos.cosecha("20");
        assertThrows(UnsupportedOperationException.class, () -> cosecha.getPesajes().add(null));
    }

    @Test
    void conservaComentariosYPermiteQueFalten() throws ValidacionException {
        Cosecha cosecha = new Cosecha(null, Datos.siembra(), Datos.HOY, Datos.HOY, null, null,
                null, " Dos camiones ", Datos.usuario());
        assertEquals("Dos camiones", cosecha.getComentarios());
        assertNull(Datos.cosecha("20").getComentarios());
    }
}
