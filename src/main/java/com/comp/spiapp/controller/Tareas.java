package com.comp.spiapp.controller;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javafx.concurrent.Task;

/**
 * Ejecuta una consulta a MySQL fuera del hilo de JavaFX y vuelve a él con el resultado o el error
 */
final class Tareas {
    private Tareas() {}

    static <T> void ejecutar(
        String nombre, 
        Callable<T> trabajo, 
        Consumer<T> alTerminar, 
        Consumer<Throwable> alFallar
    ) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };
        tarea.setOnSucceeded(e -> alTerminar.accept(tarea.getValue()));
        tarea.setOnFailed(e -> alFallar.accept(tarea.getException()));
        Thread hilo = new Thread(tarea, nombre);
        hilo.setDaemon(true);
        hilo.start();
    }
}
