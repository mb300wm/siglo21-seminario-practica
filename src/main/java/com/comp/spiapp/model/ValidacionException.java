package com.comp.spiapp.model;

import java.util.List;

public class ValidacionException extends Exception {
    private static final long serialVersionUID = 1L;

    private final transient List<String> errores;

    public ValidacionException(String mensaje) {
        this(List.of(mensaje));
    }

    public ValidacionException(List<String> errores) {
        super(String.join("\n", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
