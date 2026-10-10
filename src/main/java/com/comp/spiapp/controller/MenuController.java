package com.comp.spiapp.controller;

import com.comp.spiapp.service.SesionUsuario;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public final class MenuController {
    private final SesionUsuario sesion;
    private final Runnable alCerrarSesion;
    private final Runnable alRegistrarSiembra;
    private final Runnable alRegistrarCosecha;
    private final Runnable alRegistrarOrden;
    private final Runnable alAplicarInsumos;
    private final Runnable alConsultarTrazabilidad;
    @FXML private Label usuarioLabel;

    public MenuController(SesionUsuario sesion, Runnable alCerrarSesion,
                          Runnable alRegistrarSiembra, Runnable alRegistrarCosecha,
                          Runnable alRegistrarOrden, Runnable alAplicarInsumos,
                          Runnable alConsultarTrazabilidad) {
        this.sesion = sesion;
        this.alCerrarSesion = alCerrarSesion;
        this.alRegistrarSiembra = alRegistrarSiembra;
        this.alRegistrarCosecha = alRegistrarCosecha;
        this.alRegistrarOrden = alRegistrarOrden;
        this.alAplicarInsumos = alAplicarInsumos;
        this.alConsultarTrazabilidad = alConsultarTrazabilidad;
    }

    @FXML
    private void initialize() {
        var usuario = sesion.requerirUsuario();
        usuarioLabel.setText(usuario.getNombre() + " (" + usuario.getLogin() + ")");
    }

    @FXML
    private void registrarSiembra() {
        alRegistrarSiembra.run();
    }

    @FXML
    private void registrarCosecha() {
        alRegistrarCosecha.run();
    }

    @FXML
    private void registrarOrden() {
        alRegistrarOrden.run();
    }

    @FXML
    private void aplicarInsumos() {
        alAplicarInsumos.run();
    }

    @FXML
    private void consultarTrazabilidad() {
        alConsultarTrazabilidad.run();
    }

    @FXML
    private void cerrarSesion() {
        alCerrarSesion.run();
    }
}
