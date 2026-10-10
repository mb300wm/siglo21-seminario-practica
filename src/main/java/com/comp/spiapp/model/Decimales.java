package com.comp.spiapp.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Decimales {
    static final int ESCALA_PESO = 2;
    static final int ESCALA_HECTAREAS = 2;
    static final int ESCALA_CANTIDAD = 3;
    static final int ESCALA_HUMEDAD = 2;
    static final int ESCALA_RINDE = 1;
    static final int ESCALA_DOSIS = 3;
    static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private Decimales() {
    }

    static BigDecimal escalar(BigDecimal valor, int escala) {
        return valor == null ? null : valor.setScale(escala, REDONDEO);
    }
}
