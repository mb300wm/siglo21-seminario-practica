package com.comp.spiapp.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.comp.spiapp.repository.EtapaTrazabilidad;

/**
 * Una etapa de la cronología de un lote, con lo que agrega el servicio:
 * el ciclo productivo, la fecha de la siembra de ese ciclo y el rinde de las cosechas.
 */
public record EtapaLote(EtapaTrazabilidad datos, Integer ciclo, LocalDate siembraDelCiclo, BigDecimal rinde,
                        boolean sinCosecha) {
}
