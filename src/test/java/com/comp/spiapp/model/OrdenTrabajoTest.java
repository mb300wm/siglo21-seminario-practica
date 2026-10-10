package com.comp.spiapp.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.comp.spiapp.model.Datos.bd;
import org.junit.jupiter.api.Test;

class OrdenTrabajoTest {
    @Test
    void aceptaElFormatoDelNumeroYRechazaOtros() throws ValidacionException {
        OrdenTrabajo orden = Datos.orden(Datos.establecimiento(1));
        assertNull(orden.getNumero());
        assertThrows(ValidacionException.class, () -> orden.asignarNumero("1-2-3"));
        assertThrows(ValidacionException.class, () -> orden.asignarNumero("AA-BB-CCCC"));
        orden.asignarNumero("26-01-0001");
        assertEquals("26-01-0001", orden.getNumero());
        assertThrows(IllegalStateException.class, () -> orden.asignarNumero("26-01-0002"));
    }

    @Test
    void elConstructorRechazaUnNumeroConFormatoInvalido() throws ValidacionException {
        Establecimiento campo = Datos.establecimiento(1);
        assertThrows(ValidacionException.class, () -> new OrdenTrabajo(null, "123", Datos.HOY,
                new Labor(1, "L"), campo, new Contratista(1, "C"), null, null, Datos.campania(), null,
                Datos.usuario()));
    }

    @Test
    void exigeAlMenosUnLoteParaValidar() throws ValidacionException {
        Establecimiento campo = Datos.establecimiento(1);
        OrdenTrabajo orden = Datos.orden(campo);
        assertThrows(ValidacionException.class, orden::validar);
        orden.agregarLote(new OrdenLote(null, Datos.lote(1, campo), null, bd("10")));
        assertDoesNotThrow(orden::validar);
    }

    @Test
    void elLoteDebeSerDelEstablecimientoDeLaOrden() throws ValidacionException {
        OrdenTrabajo orden = Datos.orden(Datos.establecimiento(1));
        Lote ajeno = Datos.lote(7, Datos.establecimiento(2));
        OrdenLote detalle = new OrdenLote(null, ajeno, null, null);
        ValidacionException error = assertThrows(ValidacionException.class, () -> orden.agregarLote(detalle));
        assertTrue(error.getMessage().contains("no pertenece al establecimiento"));
        assertTrue(orden.getLotes().isEmpty());
    }

    @Test
    void noRepiteLotesNiInsumos() throws ValidacionException {
        Establecimiento campo = Datos.establecimiento(1);
        OrdenTrabajo orden = Datos.orden(campo);
        Lote lote = Datos.lote(1, campo);
        orden.agregarLote(new OrdenLote(null, lote, null, bd("10")));
        assertThrows(ValidacionException.class, () -> orden.agregarLote(new OrdenLote(null, lote, null, bd("5"))));
        Insumo herbicida = Datos.herbicida(3);
        orden.agregarInsumo(new OrdenInsumo(null, herbicida, bd("50"), "l"));
        assertThrows(ValidacionException.class,
                () -> orden.agregarInsumo(new OrdenInsumo(null, herbicida, bd("10"), "l")));
    }

    @Test
    void laAplicacionDebeUsarLotesEInsumosDeLaMismaOrden() throws ValidacionException {
        Establecimiento campo = Datos.establecimiento(1);
        OrdenTrabajo orden = Datos.orden(campo);
        OrdenLote propio = new OrdenLote(null, Datos.lote(1, campo), null, bd("10"));
        OrdenInsumo insumo = new OrdenInsumo(null, Datos.herbicida(3), bd("50"), "l");
        orden.agregarLote(propio);
        orden.agregarInsumo(insumo);

        AplicacionInsumo valida = new AplicacionInsumo(null, propio, insumo, bd("25"));
        orden.agregarAplicacion(valida);
        assertEquals(1, orden.getAplicaciones().size());

        assertThrows(ValidacionException.class,
                () -> orden.agregarAplicacion(new AplicacionInsumo(null, propio, insumo, bd("5"))));
        OrdenLote ajeno = new OrdenLote(null, Datos.lote(2, campo), null, bd("10"));
        assertThrows(ValidacionException.class,
                () -> orden.agregarAplicacion(new AplicacionInsumo(null, ajeno, insumo, bd("5"))));
    }

    @Test
    void unaLaborSinInsumosPuedeTenerSuperficieDepositoYRemitoSinDato() throws ValidacionException {
        Establecimiento campo = Datos.establecimiento(1);
        OrdenTrabajo orden = Datos.orden(campo);
        orden.agregarLote(new OrdenLote(null, Datos.lote(1, campo), null, null));
        assertDoesNotThrow(orden::validar);
        assertNull(orden.getNumero());
        assertNull(orden.getDeposito());
        assertNull(orden.getRemitoNumero());
    }

    @Test
    void laOrdenConAplicacionesConservaLosCamposAdministrativosOpcionales() throws ValidacionException {
        Establecimiento campo = Datos.establecimiento(1);
        OrdenTrabajo orden = new OrdenTrabajo(null, null, Datos.HOY, new Labor(1, "Pulverizacion"), campo,
                new Contratista(1, "Contratista"), null, " ", Datos.campania(), " ", " Nota ", Datos.usuario());
        OrdenLote lote = new OrdenLote(null, Datos.lote(1, campo), null, bd("20"));
        OrdenInsumo insumo = new OrdenInsumo(null, Datos.herbicida(3), bd("40"), "L");
        orden.agregarLote(lote);
        orden.agregarInsumo(insumo);
        orden.agregarAplicacion(new AplicacionInsumo(null, lote, insumo, bd("40")));
        assertDoesNotThrow(orden::validar);
        assertNull(orden.getDeposito());
        assertNull(orden.getRemitoNumero());
        assertNull(orden.getConvenio());
        assertEquals("Nota", orden.getComentarios());
    }

    @Test
    void rechazaAplicacionesConUnInsumoQueNoEstaEnLaOrden() throws ValidacionException {
        Establecimiento campo = Datos.establecimiento(1);
        OrdenTrabajo orden = Datos.orden(campo);
        OrdenLote lote = new OrdenLote(null, Datos.lote(1, campo), null, bd("20"));
        orden.agregarLote(lote);
        OrdenInsumo ajeno = new OrdenInsumo(null, Datos.herbicida(9), bd("40"), "L");
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> orden.agregarAplicacion(new AplicacionInsumo(null, lote, ajeno, bd("40"))));
        assertTrue(error.getMessage().contains("El insumo de la aplicación no pertenece"));
        assertTrue(orden.getAplicaciones().isEmpty());
    }
}
