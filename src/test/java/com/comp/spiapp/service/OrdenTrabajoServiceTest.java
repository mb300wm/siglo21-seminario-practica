package com.comp.spiapp.service;

import static com.comp.spiapp.Ejemplos.bd;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.comp.spiapp.Ejemplos;
import com.comp.spiapp.JdbcDoble;
import com.comp.spiapp.model.AplicacionInsumo;
import com.comp.spiapp.model.Contratista;
import com.comp.spiapp.model.Insumo;
import com.comp.spiapp.model.GrupoInsumo;
import com.comp.spiapp.model.Labor;
import com.comp.spiapp.model.OrdenInsumo;
import com.comp.spiapp.model.OrdenLote;
import com.comp.spiapp.model.OrdenTrabajo;
import com.comp.spiapp.model.ValidacionException;
import com.comp.spiapp.repository.CatalogoRepository;
import com.comp.spiapp.repository.DetalleOrdenRepository;
import com.comp.spiapp.repository.OrdenTrabajoRepository;

class OrdenTrabajoServiceTest {
    private final JdbcDoble jdbc = Ejemplos.conOrdenes(new JdbcDoble());
    private final OrdenTrabajoService servicio = new OrdenTrabajoService(jdbc::abrir, new CatalogoRepository(),
            new OrdenTrabajoRepository(), new DetalleOrdenRepository());

    /** P06: una orden con un lote del establecimiento se guarda con el número siguiente de la campaña. */
    @Test
    void guardaLaOrdenConSusLotesYElNumeroSiguiente() throws Exception {
        OrdenTrabajo orden = ordenConUnLote();

        String numero = servicio.registrar(orden);

        assertEquals("25-26-0007", numero);
        assertEquals("25-26-0007", orden.getNumero());
        assertEquals(100, orden.getId());
        assertEquals(101, orden.getLotes().getFirst().getId());

        var cabecera = jdbc.inserciones.get(0);
        assertTrue(cabecera.sql().contains("INSERT INTO ordenes_trabajo"));
        assertEquals("25-26-0007", cabecera.parametro(1));
        assertNull(cabecera.parametro(6), "sin depósito");
        assertNull(cabecera.parametro(7), "sin remito");
        assertEquals(2, cabecera.parametro(8));
        assertEquals(7, cabecera.parametro(11));
        var lote = jdbc.inserciones.get(1);
        assertEquals(List.of(100, 1, bd("20.00")), List.of(lote.parametro(1), lote.parametro(2), lote.parametro(4)));
        assertNull(lote.parametro(3), "sin cultivo");
    }

    /** El bloqueo de la campaña va antes de calcular el número y se mantiene hasta el commit. */
    @Test
    void bloqueaLaCampaniaAntesDeNumerarYDentroDeLaTransaccion() throws Exception {
        servicio.registrar(ordenConUnLote());

        int inicio = jdbc.eventos.indexOf("autocommit:false");
        int bloqueo = indice("FOR UPDATE");
        int maximo = indice("MAX(CAST");
        int commit = jdbc.eventos.indexOf("commit");
        assertTrue(inicio >= 0 && inicio < bloqueo, "el bloqueo ocurre dentro de la transacción");
        assertTrue(bloqueo < maximo, "el correlativo se lee después de bloquear");
        assertTrue(maximo < commit);
        assertEquals("cerrar:conexion", jdbc.eventos.getLast());
    }

    @Test
    void laPrimeraOrdenDeLaCampaniaEsLa0001() throws Exception {
        jdbc.resultado("MAX(CAST", List.of(Map.of("ultimo", 0)));
        assertEquals("25-26-0001", servicio.registrar(ordenConUnLote()));
    }

    /** P06 y A1: sin lotes la orden no se guarda y ni siquiera se abre la conexión. */
    @Test
    void sinLotesNoLlegaAMySQL() throws Exception {
        var error = assertThrows(ValidacionException.class, () -> servicio.registrar(ordenSinLotes()));
        assertEquals(List.of("La orden debe tener al menos un lote."), error.getErrores());
        assertEquals(0, jdbc.conexionesAbiertas());
    }

    @Test
    void unaCampaniaSinFormatoAAAANoRecibeNumero() throws Exception {
        jdbc.resultado("campana_id = ? FOR UPDATE", List.of(Map.of("nombre", "2025")));
        OrdenTrabajo orden = ordenConUnLote();

        var error = assertThrows(ValidacionException.class, () -> servicio.registrar(orden));

        assertTrue(error.getMessage().contains("no tiene el formato AA-AA"));
        assertTrue(jdbc.eventos.contains("rollback"));
        assertTrue(jdbc.inserciones.isEmpty());
        assertNull(orden.getNumero());
    }

    /** Si falla un lote no queda la cabecera, y la orden en memoria sigue sin número ni id. */
    @Test
    void siFallaUnLoteSeRevierteTodo() throws Exception {
        jdbc.fallarEnInsercion(2);
        OrdenTrabajo orden = ordenConUnLote();

        assertThrows(OperacionException.class, () -> servicio.registrar(orden));

        assertTrue(jdbc.eventos.contains("rollback"));
        assertFalse(jdbc.eventos.contains("commit"));
        assertNull(orden.getNumero());
        assertNull(orden.getId());
        assertNull(orden.getLotes().getFirst().getId());
    }

    /** P07 sobre una orden guardada: 40 L en 20 ha dan 2 L/ha. */
    @Test
    void abreUnaOrdenConSusDetallesYCalculaLaDosis() throws Exception {
        OrdenTrabajo orden = servicio.buscar(4);

        assertEquals("25-26-0001", orden.getNumero());
        assertEquals("Depósito central", orden.getDeposito().getNombre());
        assertEquals(2, orden.getLotes().size());
        assertEquals(1, orden.getInsumos().size());
        assertEquals(2, orden.getAplicaciones().size());
        assertEquals(bd("2.000"), orden.getAplicaciones().get(0).calcularDosis());
        assertEquals(bd("2.000"), orden.getAplicaciones().get(1).calcularDosis(), "30 L en 15 ha");
        assertEquals(1, jdbc.conexionesAbiertas());
    }

    @Test
    void unaOrdenDadaDeBajaNoSeAbre() {
        jdbc.resultado("WHERE ot.orden_id = ?", List.of());
        var error = assertThrows(ValidacionException.class, () -> servicio.buscar(4));
        assertEquals("La orden ya no existe o fue dada de baja.", error.getMessage());
    }

    /** CU-05: guarda solo lo nuevo, con los ids generados, y registra quién modificó la orden (RNF10). */
    @Test
    void guardaInsumosYAplicacionesNuevosYAuditaLaModificacion() throws Exception {
        OrdenTrabajo orden = servicio.buscar(4);
        OrdenInsumo fungicida = new OrdenInsumo(null, fungicida(), bd("15"), "L");
        orden.agregarInsumo(fungicida);
        AplicacionInsumo aplicacion = new AplicacionInsumo(null, orden.getLotes().get(1), fungicida, bd("15"));
        orden.agregarAplicacion(aplicacion);

        int guardados = servicio.registrarAplicaciones(orden, Ejemplos.usuario());

        assertEquals(2, guardados);
        assertEquals(3, jdbc.inserciones.size());
        var auditoria = jdbc.inserciones.get(0);
        assertTrue(auditoria.sql().contains("UPDATE ordenes_trabajo SET modificado_por"));
        assertEquals(7, auditoria.parametro(1));
        assertEquals(4, auditoria.parametro(3));
        var insumo = jdbc.inserciones.get(1);
        assertEquals(List.of(4, 9), List.of(insumo.parametro(1), insumo.parametro(2)));
        var nueva = jdbc.inserciones.get(2);
        assertEquals(List.of(4, 12, fungicida.getId(), bd("15.000")),
                List.of(nueva.parametro(1), nueva.parametro(2), nueva.parametro(3), nueva.parametro(4)));
        assertNotNull(aplicacion.getId());
        assertEquals("mateo", orden.getModificadoPor().getLogin());
        assertTrue(jdbc.eventos.contains("commit"));
    }

    @Test
    void siFallaUnaAplicacionNoQuedaNadaGuardadoNiAuditado() throws Exception {
        OrdenTrabajo orden = servicio.buscar(4);
        OrdenInsumo fungicida = new OrdenInsumo(null, fungicida(), null, "L");
        orden.agregarInsumo(fungicida);
        orden.agregarAplicacion(new AplicacionInsumo(null, orden.getLotes().get(0), fungicida, bd("10")));
        jdbc.fallarEnInsercion(3);

        assertThrows(OperacionException.class, () -> servicio.registrarAplicaciones(orden, Ejemplos.usuario()));

        assertTrue(jdbc.eventos.contains("rollback"));
        assertNull(fungicida.getId());
        assertNull(orden.getModificadoPor());
    }

    @Test
    void sinNadaNuevoNoAbreConexion() throws Exception {
        OrdenTrabajo orden = servicio.buscar(4);
        int abiertas = jdbc.conexionesAbiertas();
        var error = assertThrows(ValidacionException.class,
                () -> servicio.registrarAplicaciones(orden, Ejemplos.usuario()));
        assertEquals("No hay insumos ni aplicaciones nuevos para guardar.", error.getMessage());
        assertEquals(abiertas, jdbc.conexionesAbiertas());
    }

    private int indice(String fragmento) {
        for (int i = 0; i < jdbc.eventos.size(); i++) {
            if (jdbc.eventos.get(i).startsWith("consulta:") && jdbc.eventos.get(i).contains(fragmento)) {
                return i;
            }
        }
        throw new AssertionError("No se ejecutó la consulta " + fragmento);
    }

    private static OrdenTrabajo ordenSinLotes() throws ValidacionException {
        return new OrdenTrabajo(null, null, LocalDate.of(2025, 11, 20), new Labor(1, "Pulverización terrestre"),
                Ejemplos.lote().getEstablecimiento(), new Contratista(1, "Contratista de prueba"), null, null,
                Ejemplos.campania(), null, Ejemplos.usuario());
    }

    private static OrdenTrabajo ordenConUnLote() throws ValidacionException {
        OrdenTrabajo orden = ordenSinLotes();
        orden.agregarLote(new OrdenLote(null, Ejemplos.lote(), null, bd("20")));
        return orden;
    }

    private static Insumo fungicida() throws ValidacionException {
        return new Insumo(9, "Fungicida foliar", new GrupoInsumo(6, "Fungicidas", null), null);
    }
}
