package com.comp.spiapp.service;

import static com.comp.spiapp.Ejemplos.bd;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.comp.spiapp.Ejemplos;
import com.comp.spiapp.JdbcDoble;
import com.comp.spiapp.model.Cosecha;
import com.comp.spiapp.model.ValidacionException;
import com.comp.spiapp.repository.CatalogoRepository;
import com.comp.spiapp.repository.CosechaRepository;
import com.comp.spiapp.repository.PesajeRepository;
import com.comp.spiapp.repository.SiembraRepository;

class CosechaServiceTest {
    private final JdbcDoble jdbc = Ejemplos.conCatalogos(new JdbcDoble());
    private final CosechaService servicio = new CosechaService(jdbc::abrir, new SiembraRepository(),
            new CatalogoRepository(), new CosechaRepository(), new PesajeRepository());

    /** P03: pesajes 30000/10000 y 32000/10000 en 20 ha. */
    @Test
    void guardaLaCosechaYSusPesajesEnUnaTransaccion() throws Exception {
        Cosecha cosecha = cosechaConDosCamiones();

        int id = servicio.registrar(cosecha);

        assertEquals(100, id);
        assertEquals(bd("42000.00"), cosecha.calcularPesoTotal());
        assertEquals(bd("2100.0"), cosecha.calcularRinde().orElseThrow());
        assertEquals(List.of("autocommit:false", "cerrar:filas", "cerrar:sentencia", "cerrar:sentencia",
                "commit", "cerrar:conexion"), jdbc.eventos);

        var cabecera = jdbc.inserciones.get(0);
        assertTrue(cabecera.sql().startsWith("INSERT INTO cosechas"));
        assertEquals(10, cabecera.parametro(1));
        assertEquals(bd("20.00"), cabecera.parametro(4));
        assertNull(cabecera.parametro(6), "sin acopio elegido");
        assertEquals(7, cabecera.parametro(8));

        var primero = jdbc.inserciones.get(1);
        var segundo = jdbc.inserciones.get(2);
        assertEquals(List.of(100, 1, bd("30000.00"), bd("10000.00")),
                List.of(primero.parametro(1), primero.parametro(2), primero.parametro(3), primero.parametro(4)));
        assertEquals(List.of(100, 2, bd("32000.00"), bd("10000.00")),
                List.of(segundo.parametro(1), segundo.parametro(2), segundo.parametro(3), segundo.parametro(4)));
    }

    /** P05: si falla el segundo pesaje no queda guardada la cosecha ni el primer pesaje. */
    @Test
    void siFallaUnPesajeSeRevierteTodo() throws Exception {
        jdbc.fallarEnInsercion(3);
        Cosecha cosecha = cosechaConDosCamiones();

        var error = assertThrows(OperacionException.class, () -> servicio.registrar(cosecha));

        assertTrue(error.getMessage().startsWith("No se pudo guardar la cosecha."));
        assertTrue(jdbc.eventos.contains("rollback"));
        assertFalse(jdbc.eventos.contains("commit"));
        assertEquals("cerrar:conexion", jdbc.eventos.getLast());
        assertNull(cosecha.getId());
    }

    /** A3: sin al menos un pesaje la cosecha no se confirma y ni siquiera se abre la conexión. */
    @Test
    void sinPesajesNoLlegaAMySQL() throws Exception {
        Cosecha cosecha = new Cosecha(null, Ejemplos.siembraGuardada(), LocalDate.of(2026, 3, 20),
                LocalDate.of(2026, 3, 21), bd("20"), null, null, Ejemplos.usuario());
        var error = assertThrows(ValidacionException.class, () -> servicio.registrar(cosecha));
        assertEquals(List.of("Debe agregarse al menos un pesaje."), error.getErrores());
        assertEquals(0, jdbc.conexionesAbiertas());
    }

    @Test
    void cargaLasSiembrasVigentesConSuLoteSemillaYAuditoria() throws Exception {
        var opciones = servicio.cargarOpciones();

        assertTrue(jdbc.consultas.stream().anyMatch(sql -> sql.contains("s.eliminado_en IS NULL")));
        var siembra = opciones.siembras().getFirst();
        assertEquals(10, siembra.getId());
        assertEquals("6 NORTE", siembra.getLote().getNombre());
        assertEquals("ESTANCIA CHARRA", siembra.getLote().getEstablecimiento().getNombre());
        assertEquals("Maíz", siembra.getSemilla().getGrupo().getCultivo().getNombre());
        assertNull(siembra.getTractor());
        assertNull(siembra.getMarchaTrabajo());
        assertEquals("demo_encargado", siembra.getCreadoPor().getLogin());
        assertEquals(LocalDateTime.of(2025, 10, 16, 18, 30), siembra.getCreadoEn());
        assertEquals("Silo propio", opciones.acopios().getFirst().getNombre());
        assertEquals(1, jdbc.conexionesAbiertas());
    }

    private static Cosecha cosechaConDosCamiones() throws ValidacionException {
        Cosecha cosecha = new Cosecha(null, Ejemplos.siembraGuardada(), LocalDate.of(2026, 3, 20),
                LocalDate.of(2026, 3, 21), bd("20"), bd("14"), null, Ejemplos.usuario());
        cosecha.registrarPesaje(bd("30000"), bd("10000"));
        cosecha.registrarPesaje(bd("32000"), bd("10000"));
        return cosecha;
    }
}
