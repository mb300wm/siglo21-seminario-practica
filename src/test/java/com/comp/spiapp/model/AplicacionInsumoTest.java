package com.comp.spiapp.model;

import org.junit.jupiter.api.Test;

import static com.comp.spiapp.model.Datos.bd;
import static org.junit.jupiter.api.Assertions.*;

class AplicacionInsumoTest {
    @Test
    void calculaLaDosisConLaSuperficieTrabajada() throws ValidacionException {
        OrdenLote lote = new OrdenLote(null, Datos.lote(1, Datos.establecimiento(1)), null, bd("20"));
        OrdenInsumo insumo = new OrdenInsumo(null, Datos.herbicida(3), bd("40"), "L");
        AplicacionInsumo aplicacion = new AplicacionInsumo(null, lote, insumo, bd("40"));
        assertEquals(bd("2.000"), aplicacion.calcularDosis());
    }

    @Test
    void rechazaLaAplicacionAntesDeConfirmarlaSiNoHaySuperficie() throws ValidacionException {
        OrdenLote lote = new OrdenLote(null, Datos.lote(1, Datos.establecimiento(1)), null, null);
        OrdenInsumo insumo = new OrdenInsumo(null, Datos.herbicida(3), null, "L");
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> new AplicacionInsumo(null, lote, insumo, bd("40")));
        assertTrue(error.getMessage().contains("superficie mayor que cero"));
    }

    @Test
    void rechazaLaSuperficieYLaCantidadQueSeRedondeanACero() throws ValidacionException {
        Lote lote = Datos.lote(1, Datos.establecimiento(1));
        assertThrows(ValidacionException.class, () -> new OrdenLote(null, lote, null, bd("0.004")));
        Insumo insumo = Datos.herbicida(3);
        assertThrows(ValidacionException.class, () -> new OrdenInsumo(null, insumo, bd("0.0004"), "L"));
        assertEquals(bd("0.001"), new OrdenInsumo(null, insumo, bd("0.0005"), "L").getCantidadARemitir());
        assertNull(new OrdenInsumo(null, insumo, null, "L").getCantidadARemitir());
    }

    @Test
    void rechazaTotalesNoPositivosYReferenciasAusentes() throws ValidacionException {
        OrdenLote lote = new OrdenLote(null, Datos.lote(1, Datos.establecimiento(1)), null, bd("20"));
        OrdenInsumo insumo = new OrdenInsumo(null, Datos.herbicida(3), bd("40"), "L");
        for (String total : new String[]{"0", "-1", "0.0004"}) {
            assertThrows(ValidacionException.class, () -> new AplicacionInsumo(null, lote, insumo, bd(total)));
        }
        assertThrows(ValidacionException.class, () -> new AplicacionInsumo(null, null, insumo, bd("40")));
        assertThrows(ValidacionException.class, () -> new AplicacionInsumo(null, lote, null, bd("40")));
    }
}
