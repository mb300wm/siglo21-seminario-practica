package com.comp.spiapp.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Guarda y recupera el reporte de CU-07 en un archivo CSV que Excel abre directamente.
 * Cada fila es un arreglo de largo fijo (las columnas no cambian) y las filas van en un ArrayList
 * porque su cantidad depende de la campaña.
 */
public final class ArchivoTrazabilidad {
    private static final String[] COLUMNAS =
            {"Lote", "Fecha", "Etapa", "Detalle", "Cantidad", "Rinde", "Orden", "Ciclo"};
    private static final char SEPARADOR = ';';
    private static final char COMILLA = '"';
    /** Marca de UTF-8: sin ella Excel muestra mal los acentos. */
    private static final char BOM = '﻿';

    public void guardar(Path destino, List<String[]> filas) throws IOException {
        StringBuilder texto = new StringBuilder().append(BOM);
        agregarLinea(texto, COLUMNAS);
        for (String[] fila : filas) {
            if (fila.length != COLUMNAS.length) {
                throw new IllegalArgumentException("La fila debe tener " + COLUMNAS.length + " columnas");
            }
            agregarLinea(texto, fila);
        }
        Files.writeString(destino, texto, StandardCharsets.UTF_8);
    }

    /** @throws IOException si no se puede leer o no tiene el formato del reporte */
    public List<String[]> leer(Path origen) throws IOException {
        String texto = Files.readString(origen, StandardCharsets.UTF_8);
        if (!texto.isEmpty() && texto.charAt(0) == BOM) {
            texto = texto.substring(1);
        }
        List<String[]> registros = separar(texto);
        if (registros.isEmpty() || !Arrays.equals(registros.getFirst(), COLUMNAS)) {
            throw new IOException("El archivo no tiene las columnas del reporte");
        }
        List<String[]> filas = new ArrayList<>(registros.subList(1, registros.size()));
        for (String[] fila : filas) {
            if (fila.length != COLUMNAS.length) {
                throw new IOException("Una fila no tiene " + COLUMNAS.length + " columnas");
            }
        }
        return filas;
    }

    private static void agregarLinea(StringBuilder texto, String[] campos) {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                texto.append(SEPARADOR);
            }
            texto.append(escapar(campos[i]));
        }
        texto.append("\r\n");
    }

    /** Entre comillas solo cuando el valor tiene caracteres que romperían el formato. */
    private static String escapar(String campo) {
        if (campo == null) {
            return "";
        }
        if (campo.indexOf(SEPARADOR) < 0 && campo.indexOf(COMILLA) < 0
                && campo.indexOf('\n') < 0 && campo.indexOf('\r') < 0) {
            return campo;
        }
        return COMILLA + campo.replace("\"", "\"\"") + COMILLA;
    }

    /** Recorre el texto carácter a carácter: un separador entre comillas es parte del valor. */
    private static List<String[]> separar(String texto) throws IOException {
        List<String[]> registros = new ArrayList<>();
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (entreComillas) {
                if (c != COMILLA) {
                    campo.append(c);
                } else if (i + 1 < texto.length() && texto.charAt(i + 1) == COMILLA) {
                    campo.append(COMILLA);
                    i++;
                } else {
                    entreComillas = false;
                }
            } else if (c == COMILLA) {
                entreComillas = true;
            } else if (c == SEPARADOR) {
                cerrarCampo(campos, campo);
            } else if (c == '\n') {
                cerrarRegistro(registros, campos, campo);
            } else if (c != '\r') {
                campo.append(c);
            }
        }
        if (entreComillas) {
            throw new IOException("Hay un valor con las comillas sin cerrar");
        }
        if (!campos.isEmpty() || campo.length() > 0) {
            cerrarRegistro(registros, campos, campo);
        }
        return registros;
    }

    private static void cerrarCampo(List<String> campos, StringBuilder campo) {
        campos.add(campo.toString());
        campo.setLength(0);
    }

    private static void cerrarRegistro(List<String[]> registros, List<String> campos, StringBuilder campo) {
        cerrarCampo(campos, campo);
        registros.add(campos.toArray(new String[0]));
        campos.clear();
    }
}
