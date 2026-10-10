package com.comp.spiapp.repository;

import java.util.List;

import com.comp.spiapp.model.Acopio;
import com.comp.spiapp.model.Campania;
import com.comp.spiapp.model.Establecimiento;
import com.comp.spiapp.model.Insumo;
import com.comp.spiapp.model.Lote;

/**
 * Agrupa las listas de opciones que necesita el formulario
 * de Registrar Siembra
 */
public record Catalogos(
    List<Campania> campanias,
    List<Establecimiento> establecimientos,
    List<Lote> lotes,
    List<Insumo> semillas,
    List<Acopio> acopios
) {
    public Catalogos {
        campanias = List.copyOf(campanias);
        establecimientos = List.copyOf(establecimientos);
        lotes = List.copyOf(lotes);
        semillas = List.copyOf(semillas);
        acopios = List.copyOf(acopios);
    }
}
