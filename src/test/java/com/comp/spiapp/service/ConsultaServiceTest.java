package com.comp.spiapp.service;

import static com.comp.spiapp.Ejemplos.bd;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.comp.spiapp.Ejemplos;
import com.comp.spiapp.JdbcDoble;
import com.comp.spiapp.repository.CatalogoRepository;
import com.comp.spiapp.repository.EtapaTrazabilidad;
import com.comp.spiapp.repository.TrazabilidadRepository;

class ConsultaServiceTest {
    private final JdbcDoble jdbc = Ejemplos.conTrazabilidad(Ejemplos.conCatalogos(new JdbcDoble()));
    private final ConsultaService servicio =
            new ConsultaService(jdbc::abrir, new CatalogoRepository(), new TrazabilidadRepository());

    /** P08: dos ciclos en un lote, una siembra sin cosecha, una labranza y un lote sin operaciones. */
    @Test
    void muestraCadaLoteConSusCiclosYLosLotesSinRegistrar() throws Exception {
        TrazabilidadCampania resultado = servicio.trazabilidad(Ejemplos.campania());

        assertFalse(resultado.sinOperaciones());
        assertEquals(List.of("EL BAJO", "6 NORTE", "7 NORTE"),
                resultado.lotes().stream().map(TrazabilidadLote::lote).toList(), "por establecimiento y lote");
        assertTrue(resultado.lotes().get(0).sinRegistrar(), "EL BAJO no tiene operaciones");
        assertEquals(1, resultado.lotesSinRegistrar());

        List<EtapaLote> seisNorte = resultado.lotes().get(1).etapas();
        assertEquals(List.of("Siembra", "Trabajo - Pulverización terrestre", "Siembra", "Cosecha"),
                seisNorte.stream().map(e -> e.datos().etapa()).toList(), "en orden cronológico");

        EtapaLote primeraSiembra = seisNorte.get(0);
        assertEquals(1, primeraSiembra.ciclo());
        assertFalse(primeraSiembra.sinCosecha());
        EtapaLote segundaSiembra = seisNorte.get(2);
        assertEquals(2, segundaSiembra.ciclo());
        assertTrue(segundaSiembra.sinCosecha(), "la siembra sin cosechar también figura");

        EtapaLote cosecha = seisNorte.get(3);
        assertEquals(1, cosecha.ciclo(), "la cosecha se vincula con la siembra que la originó");
        assertEquals(Ejemplos.INICIO_SIEMBRA, cosecha.siembraDelCiclo());
        assertEquals(bd("2100.0"), cosecha.rinde(), "42000 kg en 20 ha");

        assertNull(seisNorte.get(1).ciclo(), "un trabajo no abre ciclo");
        List<EtapaLote> sieteNorte = resultado.lotes().get(2).etapas();
        assertEquals("Trabajo - Labranza", sieteNorte.getFirst().datos().etapa(), "la orden sin insumos figura");
        assertEquals("Sin insumos", sieteNorte.getFirst().datos().detalle());

        assertEquals(1, jdbc.conexionesAbiertas());
        assertEquals("cerrar:conexion", jdbc.eventos.getLast());
    }

    /** A1: una campaña sin operaciones se informa y todos sus lotes quedan sin registrar. */
    @Test
    void unaCampaniaSinOperacionesSeInforma() throws Exception {
        jdbc.resultado("FROM v_trazabilidad_campana", List.of()).resultado("NOT EXISTS (SELECT 1 FROM orden_lote_insumos", List.of());

        TrazabilidadCampania resultado = servicio.trazabilidad(Ejemplos.campania());

        assertTrue(resultado.sinOperaciones());
        assertEquals(3, resultado.lotesSinRegistrar());
    }

    @Test
    void sinHectareasONiConCosechasIndistinguiblesElRindeQuedaSinDato() throws Exception {
        jdbc.resultado("FROM cosechas co", List.of(
                Map.of("siembra_id", 10, "fecha_inicio", LocalDate.of(2026, 3, 20), "hectareas", bd("20.00")),
                Map.of("siembra_id", 10, "fecha_inicio", LocalDate.of(2026, 3, 20), "hectareas", bd("5.00"))));

        var cosecha = servicio.trazabilidad(Ejemplos.campania()).lotes().get(1).etapas().get(3);

        assertTrue(cosecha.datos().esCosecha());
        assertNull(cosecha.rinde(), "no se sabe cuál de las dos cosechas es");
    }

    /** El mismo día va primero la siembra, después el trabajo y por último la cosecha. */
    @Test
    void elOrdenCronologicoDesempataPorEtapa() {
        LocalDate dia = LocalDate.of(2025, 11, 1);
        var cosecha = etapa(EtapaTrazabilidad.COSECHA, dia, 10, null);
        var trabajo = etapa(EtapaTrazabilidad.TRABAJO, dia, null, "25-26-0003");
        var siembra = etapa(EtapaTrazabilidad.SIEMBRA, dia, 10, null);
        var anterior = etapa(EtapaTrazabilidad.COSECHA, dia.minusDays(1), 9, null);

        List<EtapaTrazabilidad> ordenadas = new java.util.ArrayList<>(List.of(cosecha, trabajo, siembra, anterior));
        ordenadas.sort(ConsultaService.CRONOLOGICO);

        assertEquals(List.of(anterior, siembra, trabajo, cosecha), ordenadas);
    }

    private static EtapaTrazabilidad etapa(int ordenEtapa, LocalDate fecha, Integer siembraId, String numeroOrden) {
        return new EtapaTrazabilidad(1, "ESTANCIA CHARRA", 1, "6 NORTE", ordenEtapa, "etapa " + ordenEtapa, fecha,
                null, null, null, null, numeroOrden, siembraId);
    }
}
