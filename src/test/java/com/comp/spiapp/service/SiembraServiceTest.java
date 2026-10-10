package com.comp.spiapp.service;

import static com.comp.spiapp.Ejemplos.bd;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.comp.spiapp.Ejemplos;
import com.comp.spiapp.JdbcDoble;
import com.comp.spiapp.model.Siembra;
import com.comp.spiapp.model.ValidacionException;
import com.comp.spiapp.repository.CatalogoRepository;
import com.comp.spiapp.repository.SiembraRepository;

class SiembraServiceTest {
    private final JdbcDoble jdbc = Ejemplos.conCatalogos(new JdbcDoble());
    private final SiembraService servicio =
            new SiembraService(jdbc::abrir, new CatalogoRepository(), new SiembraRepository());

    /** P02: 600 kg y fechas válidas se guardan con el usuario de la sesión; lo opcional vacío queda en NULL. */
    @Test
    void guardaLaSiembraConElUsuarioQueLaRegistraYOpcionalesEnNulo() throws Exception {
        Siembra siembra = new Siembra(null, Ejemplos.campania(), Ejemplos.lote(), Ejemplos.semilla(),
                Ejemplos.INICIO_SIEMBRA, Ejemplos.INICIO_SIEMBRA.plusDays(1), bd("600"), null,
                "  ", "Sembradora de prueba", Ejemplos.usuario());

        int id = servicio.registrar(siembra);

        assertEquals(100, id);
        assertEquals(100, siembra.getId());
        var insercion = jdbc.inserciones.getFirst();
        assertTrue(insercion.sql().startsWith("INSERT INTO siembras"));
        assertEquals(23, insercion.parametros().size());
        assertAll(
                () -> assertEquals(2, insercion.parametro(1)),
                () -> assertEquals(1, insercion.parametro(2)),
                () -> assertEquals(1, insercion.parametro(3)),
                () -> assertEquals(LocalDate.of(2025, 10, 15), insercion.parametro(4)),
                () -> assertNull(insercion.parametro(6), "hectáreas monitor sin dato"),
                () -> assertNull(insercion.parametro(7), "un tractor en blanco no se guarda como texto vacío"),
                () -> assertNull(insercion.parametro(8), "marcha sin dato, no cero"),
                () -> assertEquals("Sembradora de prueba", insercion.parametro(10)),
                () -> assertEquals(bd("600.00"), insercion.parametro(15)),
                () -> assertEquals(7, insercion.parametro(22)),
                () -> assertEquals(siembra.getCreadoEn(), insercion.parametro(23)));
        assertTrue(jdbc.eventos.containsAll(java.util.List.of("cerrar:sentencia", "cerrar:conexion")));
    }

    @Test
    void noRegistraDosVecesLaMismaSiembra() throws Exception {
        Siembra siembra = Ejemplos.siembraGuardada();
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(siembra));
        assertEquals(0, jdbc.conexionesAbiertas());
    }

    @Test
    void unRechazoDelDisparadorSeMuestraTalCual() throws Exception {
        jdbc.fallarEnInsercion(1, "45000", "El insumo seleccionado no es una semilla");
        var error = assertThrows(OperacionException.class, () -> servicio.registrar(nuevaSiembra()));
        assertEquals("El insumo seleccionado no es una semilla", error.getMessage());
    }

    @Test
    void otrosErroresDeMySQLNoExponenDetallesInternos() throws Exception {
        jdbc.fallarEnInsercion(1);
        Siembra siembra = nuevaSiembra();
        var error = assertThrows(OperacionException.class, () -> servicio.registrar(siembra));
        assertTrue(error.getMessage().startsWith("No se pudo guardar la siembra."));
        assertFalse(error.getMessage().contains("Fallo simulado"));
        assertNull(siembra.getId());
        assertTrue(jdbc.eventos.contains("cerrar:conexion"));
    }

    @Test
    void cargaLosCatalogosEnUnaSolaConexion() throws Exception {
        var catalogos = servicio.cargarCatalogos();
        assertEquals(1, jdbc.conexionesAbiertas());
        assertEquals("25-26", catalogos.campanias().getFirst().getNombre());
        assertEquals(3, catalogos.lotes().size());
        assertEquals("ESTANCIA CHARRA", catalogos.lotes().getFirst().getEstablecimiento().getNombre());
        assertNull(catalogos.lotes().get(2).getHectareas());
        assertTrue(catalogos.semillas().getFirst().esSemilla());
        assertEquals("Maíz", catalogos.semillas().getFirst().getGrupo().getCultivo().getNombre());
        assertEquals(1, catalogos.acopios().size());
        assertTrue(jdbc.eventos.contains("cerrar:conexion"));
    }

    private static Siembra nuevaSiembra() throws ValidacionException {
        return new Siembra(null, Ejemplos.campania(), Ejemplos.lote(), Ejemplos.semilla(),
                Ejemplos.INICIO_SIEMBRA, Ejemplos.INICIO_SIEMBRA, bd("600"), null, null, null, Ejemplos.usuario());
    }
}
