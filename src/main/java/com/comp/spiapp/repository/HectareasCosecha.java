package com.comp.spiapp.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Hectáreas de una cosecha, para calcular el rinde que la vista no trae. */
public record HectareasCosecha(int siembraId, LocalDate fechaInicio, BigDecimal hectareas) {
}
