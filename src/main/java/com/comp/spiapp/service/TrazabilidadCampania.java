package com.comp.spiapp.service;

import java.util.List;

import com.comp.spiapp.model.Campania;
import com.comp.spiapp.repository.EtapaTrazabilidad;

/** Resultado de CU-07: todos los lotes de la empresa con su cronología en la campaña elegida. */
public record TrazabilidadCampania(Campania campania, List<TrazabilidadLote> lotes) {
    public TrazabilidadCampania {
        lotes = List.copyOf(lotes);
    }

    /** Alternativa A1: la campaña no tiene ninguna operación registrada. */
    public boolean sinOperaciones() {
        return lotes.stream().allMatch(TrazabilidadLote::sinRegistrar);
    }

    /** Siembras, trabajos y cosechas de la campaña: cada posición es orden_etapa - 1. */
    public int[] etapasPorTipo() {
        int[] cantidades = new int[EtapaTrazabilidad.COSECHA];
        for (TrazabilidadLote lote : lotes) {
            for (EtapaLote etapa : lote.etapas()) {
                cantidades[etapa.datos().ordenEtapa() - 1]++;
            }
        }
        return cantidades;
    }

    public long lotesSinRegistrar() {
        return lotes.stream().filter(TrazabilidadLote::sinRegistrar).count();
    }
}
