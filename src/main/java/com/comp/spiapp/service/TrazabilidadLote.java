package com.comp.spiapp.service;

import java.util.List;

/** Cronología de un lote en la campaña; sin etapas, el lote figura como "sin registrar". */
public record TrazabilidadLote(String establecimiento, String lote, List<EtapaLote> etapas) {
    public TrazabilidadLote {
        etapas = List.copyOf(etapas);
    }

    public boolean sinRegistrar() {
        return etapas.isEmpty();
    }
}
