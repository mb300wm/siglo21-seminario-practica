package com.comp.spiapp.model;

import static com.comp.spiapp.model.Datos.bd;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Corrección de insumos y aplicaciones cargados por error antes de guardarlos. */
class OrdenTrabajoPendientesTest {
    @Test
    void quitaUnaAplicacionYDespuesSuInsumo() throws ValidacionException {
        Establecimiento campo = Datos.establecimiento(1);
        OrdenTrabajo orden = Datos.orden(campo);
        OrdenLote lote = new OrdenLote(1, Datos.lote(1, campo), null, bd("20"));
        OrdenInsumo insumo = new OrdenInsumo(null, Datos.herbicida(3), bd("40"), "L");
        orden.agregarLote(lote);
        orden.agregarInsumo(insumo);
        AplicacionInsumo aplicacion = new AplicacionInsumo(null, lote, insumo, bd("40"));
        orden.agregarAplicacion(aplicacion);

        ValidacionException enUso = assertThrows(ValidacionException.class, () -> orden.quitarInsumo(insumo));
        assertTrue(enUso.getMessage().contains("primero quitá la aplicación"));

        orden.quitarAplicacion(aplicacion);
        orden.quitarInsumo(insumo);
        assertTrue(orden.getAplicaciones().isEmpty());
        assertTrue(orden.getInsumos().isEmpty());
    }

    @Test
    void loGuardadoNoSePuedeQuitar() throws ValidacionException {
        Establecimiento campo = Datos.establecimiento(1);
        OrdenTrabajo orden = Datos.orden(campo);
        OrdenLote lote = new OrdenLote(1, Datos.lote(1, campo), null, bd("20"));
        OrdenInsumo guardado = new OrdenInsumo(5, Datos.herbicida(3), bd("40"), "L");
        orden.agregarLote(lote);
        orden.agregarInsumo(guardado);
        AplicacionInsumo aplicacion = new AplicacionInsumo(8, lote, guardado, bd("40"));
        orden.agregarAplicacion(aplicacion);

        assertThrows(ValidacionException.class, () -> orden.quitarAplicacion(aplicacion));
        assertEquals(1, orden.getAplicaciones().size());
        assertThrows(ValidacionException.class, () -> orden.quitarInsumo(guardado));
        assertEquals(1, orden.getInsumos().size());
    }

    @Test
    void rechazaQuitarAlgoQueNoEstaEnLaOrden() throws ValidacionException {
        OrdenTrabajo orden = Datos.orden(Datos.establecimiento(1));
        OrdenInsumo ajeno = new OrdenInsumo(null, Datos.herbicida(3), null, "L");
        assertThrows(ValidacionException.class, () -> orden.quitarInsumo(ajeno));
        assertThrows(ValidacionException.class, () -> orden.quitarAplicacion(null));
    }
}
