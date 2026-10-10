package com.comp.spiapp.service;

import java.util.List;

import com.comp.spiapp.model.Acopio;
import com.comp.spiapp.model.Siembra;

/**
 * Siembras vigentes y acopios para el formulario de cosechas
 */
public record OpcionesCosecha(List<Siembra> siembras, List<Acopio> acopios) {
    public OpcionesCosecha {
        siembras = List.copyOf(siembras);
        acopios = List.copyOf(acopios);
    }
}
