package com.comp.spiapp.model;

import static com.comp.spiapp.model.Datos.bd;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SiembraTest {
    @Test
    void creaUnaSiembraValida() throws ValidacionException {
        Siembra siembra = Datos.siembra();
        assertEquals(bd("1200.00"), siembra.getSemillaTotalKg());
        assertEquals(bd("20.00"), siembra.getHectareasMonitor());
        assertFalse(siembra.estaDadaDeBaja());
    }

    @Test
    void elInsumoDebeSerUnaSemilla() throws ValidacionException {
        ValidacionException e = assertThrows(ValidacionException.class, () -> new Siembra(null, Datos.campania(),
                Datos.lote(1, Datos.establecimiento(1)), Datos.herbicida(2), Datos.HOY, Datos.HOY,
                bd("100"), null, null, null, Datos.usuario()));
        assertEquals("El insumo seleccionado no es una semilla.", e.getMessage());
    }

    @Test
    void rechazaFechaFinAnteriorAlInicio() {
        assertThrows(ValidacionException.class, () -> new Siembra(null, Datos.campania(),
                Datos.lote(1, Datos.establecimiento(1)), Datos.semilla(), Datos.HOY, Datos.HOY.minusDays(1),
                bd("100"), null, null, null, Datos.usuario()));
    }

    @Test
    void reuneTodosLosErroresEnUnSoloMensaje() {
        ValidacionException e = assertThrows(ValidacionException.class, () -> new Siembra(null, null, null, null,
                null, null, bd("0"), bd("-1"), null, null, Datos.usuario()));
        assertEquals(7, e.getErrores().size());
    }

    @Test
    void exigeElUsuarioQueRegistra() {
        assertThrows(ValidacionException.class, () -> new Siembra(null, Datos.campania(),
                Datos.lote(1, Datos.establecimiento(1)), Datos.semilla(), Datos.HOY, Datos.HOY,
                bd("100"), null, null, null, null));
    }

    @Test
    void hectareasMonitorPuedenFaltarPeroNoSerNegativas() throws ValidacionException {
        Siembra sinMonitor = new Siembra(null, Datos.campania(), Datos.lote(1, Datos.establecimiento(1)),
                Datos.semilla(), Datos.HOY, Datos.HOY, bd("600"), null, null, null, Datos.usuario());
        assertNull(sinMonitor.getHectareasMonitor());
        assertThrows(ValidacionException.class, () -> new Siembra(null, Datos.campania(),
                Datos.lote(1, Datos.establecimiento(1)), Datos.semilla(), Datos.HOY, Datos.HOY,
                bd("600"), bd("-1"), null, null, Datos.usuario()));
    }

    @Test
    void conservaLaCalibracionYLosComentariosDeLaHojaDeCampo() throws ValidacionException {
        Siembra siembra = new Siembra(null, Datos.campania(), Datos.lote(1, Datos.establecimiento(1)),
                Datos.semilla(), Datos.HOY, Datos.HOY, bd("600"), bd("20"), " Tractor ", " Sembradora ",
                3, bd("7.5"), 18, 24, " P-30 ", bd("4.5"), bd("6.2"), bd("-0.5"),
                " Urea ", bd("50"), 2, " Revisar placa ", Datos.usuario());
        assertAll(
                () -> assertEquals(3, siembra.getMarchaTrabajo()),
                () -> assertEquals(bd("7.50"), siembra.getVelocidadSiembra()),
                () -> assertEquals(18, siembra.getEngranajeConductor()),
                () -> assertEquals(24, siembra.getEngranajeConducido()),
                () -> assertEquals("P-30", siembra.getPlaca()),
                () -> assertEquals(bd("4.50"), siembra.getGranoPorMetro()),
                () -> assertEquals(bd("6.20"), siembra.getCalibre()),
                () -> assertEquals(bd("-0.50"), siembra.getDesvio()),
                () -> assertEquals("Urea", siembra.getTipoFertilizante()),
                () -> assertEquals(bd("50.00"), siembra.getFertilizanteKgHa()),
                () -> assertEquals(2, siembra.getMarchaCajaFertilizante()),
                () -> assertEquals("Revisar placa", siembra.getComentarios()));
    }

    @Test
    void elConstructorSimpleDejaLosDatosTecnicosAdicionalesSinDato() throws ValidacionException {
        Siembra siembra = Datos.siembra();
        assertNull(siembra.getMarchaTrabajo());
        assertNull(siembra.getVelocidadSiembra());
        assertNull(siembra.getPlaca());
        assertNull(siembra.getTipoFertilizante());
        assertNull(siembra.getComentarios());
    }
}
