package com.comp.spiapp.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.comp.spiapp.model.ValidacionException;
import com.comp.spiapp.service.OperacionException;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Convierte el texto de los campos y señala en pantalla los que tienen errores.
 * Cada control se registra con la etiqueta con que empiezan sus mensajes ("La fecha de fin...")
 */
final class Formulario {
    static final String CAMPO_CON_ERROR = "campo-error";

    private final Map<String, Node> campos = new LinkedHashMap<>();
    private final List<String> errores = new ArrayList<>();

    Formulario campo(String etiqueta, Node control) {
        campos.put(etiqueta, control);
        return this;
    }

    void limpiar() {
        errores.clear();
        for (Node control : campos.values()) {
            control.getStyleClass().remove(CAMPO_CON_ERROR);
        }
    }

    BigDecimal decimal(TextField campo, String etiqueta) {
        try {
            return leerDecimal(campo.getText());
        } catch (NumberFormatException ex) {
            errores.add(etiqueta + " debe ser un número, por ejemplo 20,5");
            return null;
        }
    }

    Integer entero(TextField campo, String etiqueta) {
        try {
            return leerEntero(campo.getText());
        } catch (NumberFormatException ex) {
            errores.add(etiqueta + " debe ser un número entero.");
            return null;
        }
    }

    boolean hayErrores() {
        return !errores.isEmpty();
    }

    List<String> errores() {
        return List.copyOf(errores);
    }

    void marcar(List<String> mensajes) {
        for (String mensaje : mensajes) {
            for (Map.Entry<String, Node> campo : campos.entrySet()) {
                if (mensaje.startsWith(campo.getKey())) {
                    if (!campo.getValue().getStyleClass().contains(CAMPO_CON_ERROR)) {
                        campo.getValue().getStyleClass().add(CAMPO_CON_ERROR);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Traduce el error de una operación a mensajes de pantalla, sin exponer detalles internos
     */
    static List<String> mensajesDe(Throwable error) {
        if (error instanceof ValidacionException validacion) {
            return validacion.getErrores();
        }
        if (error instanceof OperacionException operacion) {
            return List.of(operacion.getMessage());
        }
        return List.of("Ocurrió un error inesperado. Vuelve a intentar.");
    }

    /** 
     * Muestra un resultado en verde si salio bien, en rojo si hay errores.
     */
    static void informar(Label etiqueta, List<String> mensajes, boolean exito) {
        etiqueta.getStyleClass().removeAll("mensaje", "exito");
        etiqueta.getStyleClass().add(exito ? "exito" : "mensaje");
        etiqueta.setText(String.join("\n", mensajes));
    }

    static BigDecimal leerDecimal(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return new BigDecimal(texto.trim().replace(',', '.'));
    }

    static Integer leerEntero(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return Integer.valueOf(texto.trim());
    }
}
