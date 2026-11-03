package com.comp.spiapp.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchivoTrazabilidadTest {
    private final ArchivoTrazabilidad archivo = new ArchivoTrazabilidad();

    @TempDir
    private Path carpeta;

    /** Separadores, comillas y saltos de línea dentro de un valor no rompen las columnas. */
    @Test
    void recuperaLoMismoQueGuardo() throws Exception {
        Path csv = carpeta.resolve("reporte.csv");
        String[] dificil = {"6 NORTE - EL CARMEN", "15/10/2025", "Siembra", "Maíz; \"DK\"\nsegunda línea", "", "", "", ""};
        String[] simple = {"EL BAJO - EL CARMEN", "", "Sin registrar", "", "", "", "", ""};

        archivo.guardar(csv, List.of(dificil, simple));
        List<String[]> leidas = archivo.leer(csv);

        assertEquals(2, leidas.size());
        assertArrayEquals(dificil, leidas.get(0));
        assertArrayEquals(simple, leidas.get(1));
    }

    @Test
    void unValorNuloSeGuardaVacio() throws Exception {
        Path csv = carpeta.resolve("nulo.csv");
        archivo.guardar(csv, List.<String[]>of(new String[]{"L", "F", "E", null, "", "", "", ""}));
        assertEquals("", archivo.leer(csv).getFirst()[3]);
    }

    @Test
    void empiezaConLaMarcaQueExcelNecesitaParaLosAcentos() throws Exception {
        Path csv = carpeta.resolve("excel.csv");
        archivo.guardar(csv, List.of());
        assertTrue(Files.readString(csv, StandardCharsets.UTF_8).startsWith("﻿Lote;Fecha;"));
    }

    @Test
    void rechazaUnArchivoConOtrasColumnas() throws Exception {
        Path csv = Files.writeString(carpeta.resolve("otro.csv"), "a;b\n1;2\n");
        assertThrows(IOException.class, () -> archivo.leer(csv));
    }

    @Test
    void rechazaUnaFilaIncompleta() throws Exception {
        Path csv = Files.writeString(carpeta.resolve("corto.csv"),
                "Lote;Fecha;Etapa;Detalle;Cantidad;Rinde;Orden;Ciclo\nsolo;dos\n");
        assertThrows(IOException.class, () -> archivo.leer(csv));
    }

    @Test
    void rechazaComillasSinCerrar() throws Exception {
        Path csv = Files.writeString(carpeta.resolve("comillas.csv"),
                "Lote;Fecha;Etapa;Detalle;Cantidad;Rinde;Orden;Ciclo\n\"abierta\n");
        assertThrows(IOException.class, () -> archivo.leer(csv));
    }

    @Test
    void unArchivoInexistenteLanzaError() {
        assertThrows(IOException.class, () -> archivo.leer(carpeta.resolve("no-existe.csv")));
    }

    @Test
    void noAceptaUnaFilaConOtraCantidadDeColumnas() {
        assertThrows(IllegalArgumentException.class,
                () -> archivo.guardar(carpeta.resolve("mal.csv"), List.<String[]>of(new String[]{"uno"})));
    }
}
