package com.comp.spiapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.comp.spiapp.model.Campania;
import com.comp.spiapp.model.Lote;
import com.comp.spiapp.repository.CatalogoRepository;
import com.comp.spiapp.repository.EtapaTrazabilidad;
import com.comp.spiapp.repository.ConexionBD;
import com.comp.spiapp.repository.HectareasCosecha;
import com.comp.spiapp.repository.TrazabilidadRepository;

/**
 * CU-07: trazabilidad por campaña. Reúne la vista v_trazabilidad_campana, los trabajos sin insumos
 * y el listado de lotes, como indica la sección 20.2 del TP2.
 */
public final class ConsultaService {
    /** Cronología: por fecha y, el mismo día, siembra antes que trabajo y trabajo antes que cosecha. */
    static final Comparator<EtapaTrazabilidad> CRONOLOGICO = Comparator
            .comparing(EtapaTrazabilidad::fecha)
            .thenComparingInt(EtapaTrazabilidad::ordenEtapa)
            .thenComparing(EtapaTrazabilidad::siembraId, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(EtapaTrazabilidad::numeroOrden, Comparator.nullsLast(Comparator.naturalOrder()));

    private final ConexionBD conexiones;
    private final CatalogoRepository catalogos;
    private final TrazabilidadRepository trazabilidad;

    public ConsultaService() {
        this(ConexionBD.desdeConfiguracion(), new CatalogoRepository(), new TrazabilidadRepository());
    }

    public ConsultaService(ConexionBD conexiones, CatalogoRepository catalogos,
                           TrazabilidadRepository trazabilidad) {
        this.conexiones = Objects.requireNonNull(conexiones);
        this.catalogos = Objects.requireNonNull(catalogos);
        this.trazabilidad = Objects.requireNonNull(trazabilidad);
    }

    /** Paso 1 del CU-07: la lista de campañas registradas. */
    public List<Campania> campanias() throws OperacionException {
        try (var conexion = conexiones.abrir()) {
            return catalogos.campanias(conexion);
        } catch (SQLException ex) {
            throw OperacionException.desde(ex, "cargar las campañas");
        }
    }

    /** Pasos 2 y 3 del CU-07: la cronología de cada lote, incluidos los que no tienen operaciones. */
    public TrazabilidadCampania trazabilidad(Campania campania) throws OperacionException {
        Objects.requireNonNull(campania);
        try (var conexion = conexiones.abrir()) {
            List<EtapaTrazabilidad> etapas = new ArrayList<>(trazabilidad.etapas(campania, conexion));
            etapas.addAll(trazabilidad.trabajosSinInsumos(campania, conexion));
            List<HectareasCosecha> hectareas = trazabilidad.hectareasDeCosechas(campania, conexion);
            List<Lote> lotes = catalogos.lotes(conexion, catalogos.establecimientos(conexion));
            return armar(campania, lotes, etapas, hectareas);
        } catch (SQLException ex) {
            throw OperacionException.desde(ex, "consultar la trazabilidad de la campaña");
        }
    }

    private static TrazabilidadCampania armar(Campania campania, List<Lote> lotes, List<EtapaTrazabilidad> etapas,
                                              List<HectareasCosecha> hectareas) {
        Map<Integer, List<EtapaTrazabilidad>> porLote = new HashMap<>();
        for (EtapaTrazabilidad etapa : etapas) {
            porLote.computeIfAbsent(etapa.loteId(), id -> new ArrayList<>()).add(etapa);
        }
        Map<ClaveCosecha, BigDecimal> hectareasPorCosecha = hectareasPorCosecha(hectareas);

        List<Lote> ordenados = new ArrayList<>(lotes);
        ordenados.sort(Comparator.comparing((Lote lote) -> lote.getEstablecimiento().getNombre())
                .thenComparing(Lote::getNombre));
        List<TrazabilidadLote> resultado = new ArrayList<>();
        for (Lote lote : ordenados) {
            List<EtapaTrazabilidad> delLote = porLote.getOrDefault(lote.getId(), List.of());
            resultado.add(new TrazabilidadLote(lote.getEstablecimiento().getNombre(), lote.getNombre(),
                    cronologia(delLote, hectareasPorCosecha)));
        }
        return new TrazabilidadCampania(campania, resultado);
    }

    /**
     * Ordena las etapas del lote y numera sus ciclos: cada siembra abre un ciclo y cada cosecha se
     * vincula con el ciclo de la siembra que la originó (siembra_id), sin elegir uno arbitrariamente.
     */
    private static List<EtapaLote> cronologia(List<EtapaTrazabilidad> etapas,
                                              Map<ClaveCosecha, BigDecimal> hectareas) {
        List<EtapaTrazabilidad> ordenadas = new ArrayList<>(etapas);
        ordenadas.sort(CRONOLOGICO);

        Map<Integer, Integer> cicloPorSiembra = new HashMap<>();
        Map<Integer, LocalDate> fechaPorSiembra = new HashMap<>();
        Set<Integer> siembrasCosechadas = new HashSet<>();
        for (EtapaTrazabilidad etapa : ordenadas) {
            if (etapa.esSiembra()) {
                cicloPorSiembra.put(etapa.siembraId(), cicloPorSiembra.size() + 1);
                fechaPorSiembra.put(etapa.siembraId(), etapa.fecha());
            } else if (etapa.esCosecha()) {
                siembrasCosechadas.add(etapa.siembraId());
            }
        }

        List<EtapaLote> resultado = new ArrayList<>();
        for (EtapaTrazabilidad etapa : ordenadas) {
            Integer siembra = etapa.siembraId();
            Integer ciclo = siembra == null ? null : cicloPorSiembra.get(siembra);
            LocalDate siembraDelCiclo = etapa.esCosecha() ? fechaPorSiembra.get(siembra) : null;
            BigDecimal rinde = etapa.esCosecha() ? rinde(etapa, hectareas) : null;
            boolean sinCosecha = etapa.esSiembra() && !siembrasCosechadas.contains(siembra);
            resultado.add(new EtapaLote(etapa, ciclo, siembraDelCiclo, rinde, sinCosecha));
        }
        return resultado;
    }

    /** Igual que v_rendimiento_lote: kg / ha con un decimal; sin hectáreas, sin dato. */
    private static BigDecimal rinde(EtapaTrazabilidad cosecha, Map<ClaveCosecha, BigDecimal> hectareas) {
        BigDecimal superficie = hectareas.get(new ClaveCosecha(cosecha.siembraId(), cosecha.fecha()));
        if (superficie == null || superficie.signum() <= 0 || cosecha.cantidad() == null) {
            return null;
        }
        return cosecha.cantidad().divide(superficie, 1, RoundingMode.HALF_UP);
    }

    /**
     * La vista no expone el ID de la cosecha: se vincula por siembra y fecha de inicio. Si dos cosechas
     * de la misma siembra empiezan el mismo día, no se puede saber cuál es cuál y el rinde queda sin dato.
     */
    private static Map<ClaveCosecha, BigDecimal> hectareasPorCosecha(List<HectareasCosecha> cosechas) {
        Map<ClaveCosecha, BigDecimal> porClave = new HashMap<>();
        Set<ClaveCosecha> repetidas = new HashSet<>();
        for (HectareasCosecha cosecha : cosechas) {
            var clave = new ClaveCosecha(cosecha.siembraId(), cosecha.fechaInicio());
            if (porClave.containsKey(clave)) {
                repetidas.add(clave);
            }
            porClave.put(clave, cosecha.hectareas());
        }
        repetidas.forEach(porClave::remove);
        return porClave;
    }

    private record ClaveCosecha(Integer siembraId, LocalDate fecha) {
    }
}
