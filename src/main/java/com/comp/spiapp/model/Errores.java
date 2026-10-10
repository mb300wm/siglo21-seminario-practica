package com.comp.spiapp.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
   Acumula mensajes de validación y los lanza juntos como una {@link ValidacionException}.
*/
final class Errores {
    private final List<String> mensajes = new ArrayList<>();

    Errores si(boolean condicion, String mensaje) {
        if (condicion) {
            mensajes.add(mensaje);
        }
        return this;
    }

    Errores obligatorio(Object valor, String campo) {
        return si(valor == null, campo + " es un dato obligatorio.");
    }

    Errores texto(String valor, String campo) {
        return si(valor == null || valor.isBlank(), campo + " es un dato obligatorio.");
    }

    /** Obligatorio y mayor que cero. */
    Errores positivo(BigDecimal valor, String campo) {
        if (valor == null) {
            return obligatorio(null, campo);
        }
        return si(valor.signum() <= 0, campo + " debe ser mayor que cero.");
    }

    /** Puede faltar, pero si se completa debe ser mayor que cero. */
    Errores positivoOpcional(BigDecimal valor, String campo) {
        return si(valor != null && valor.signum() <= 0, campo + " debe ser mayor que cero.");
    }

    boolean hayErrores() {
        return !mensajes.isEmpty();
    }

    void lanzarSiHay() throws ValidacionException {
        if (!mensajes.isEmpty()) {
            throw new ValidacionException(mensajes);
        }
    }

    /** Recorta el texto; los textos vacíos se guardan como {@code null}. */
    static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
