package com.comp.spiapp;

import com.comp.spiapp.controller.CosechaController;
import com.comp.spiapp.controller.LoginController;
import com.comp.spiapp.controller.MenuController;
import com.comp.spiapp.controller.OrdenTrabajoController;
import com.comp.spiapp.controller.SiembraController;
import com.comp.spiapp.controller.TrazabilidadController;
import com.comp.spiapp.model.Usuario;
import com.comp.spiapp.service.AutenticacionService;
import com.comp.spiapp.service.ConsultaService;
import com.comp.spiapp.service.CosechaService;
import com.comp.spiapp.service.OrdenTrabajoService;
import com.comp.spiapp.service.SesionUsuario;
import com.comp.spiapp.service.SiembraService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

/** Conserva una sola ventana y una sola sesión durante el recorrido. */
public final class Navegador {
    private final Stage ventana;
    private final AutenticacionService autenticacion;
    private final SesionUsuario sesion;
    private final SiembraService siembras;
    private final CosechaService cosechas;
    private final OrdenTrabajoService ordenes;
    private final ConsultaService consultas;
    private LoginController loginActual;

    public Navegador(Stage ventana, AutenticacionService autenticacion, SesionUsuario sesion,
                     SiembraService siembras, CosechaService cosechas,
                     OrdenTrabajoService ordenes, ConsultaService consultas) {
        this.ventana = ventana;
        this.autenticacion = autenticacion;
        this.sesion = sesion;
        this.siembras = siembras;
        this.cosechas = cosechas;
        this.ordenes = ordenes;
        this.consultas = consultas;
    }

    public void mostrarLogin() throws IOException {
        cerrar();
        LoginController login = new LoginController(autenticacion, this::ingresarAlMenu);
        Scene escena = cargar("login.fxml", login);
        loginActual = login;
        ventana.setTitle("SIGTOA — Iniciar sesión");
        ventana.setScene(escena);
    }

    private void ingresarAlMenu(Usuario usuario) {
        sesion.iniciar(usuario);
        try {
            mostrarMenu();
            loginActual.descartar();
            loginActual = null;
        } catch (IOException | RuntimeException exception) {
            sesion.cerrar();
            throw new IllegalStateException("No se pudo abrir el menú.", exception);
        }
    }

    private void mostrarMenu() throws IOException {
        var menu = new MenuController(sesion, this::cerrarSesion, this::abrirSiembra, this::abrirCosecha,
                this::abrirOrdenNueva, this::abrirAplicaciones, this::abrirTrazabilidad);
        Scene escena = cargar("menu.fxml", menu);
        ventana.setScene(escena);
        ventana.setTitle("SIGTOA — Menú principal");
    }

    private void abrirSiembra() {
        abrir("siembra.fxml", new SiembraController(siembras, sesion, this::volverAlMenu),
                "SIGTOA — Registrar siembra");
    }

    private void abrirCosecha() {
        abrir("cosecha.fxml", new CosechaController(cosechas, sesion, this::volverAlMenu),
                "SIGTOA — Registrar cosecha");
    }

    private void abrirOrdenNueva() {
        abrir("orden.fxml", new OrdenTrabajoController(ordenes, sesion, this::volverAlMenu, false),
                "SIGTOA — Orden de trabajo");
    }

    private void abrirAplicaciones() {
        abrir("orden.fxml", new OrdenTrabajoController(ordenes, sesion, this::volverAlMenu, true),
                "SIGTOA — Aplicar insumos");
    }

    private void abrirTrazabilidad() {
        abrir("trazabilidad.fxml", new TrazabilidadController(consultas, sesion, this::volverAlMenu),
                "SIGTOA — Trazabilidad por campaña");
    }

    /** Si una pantalla no puede abrirse, el usuario sigue donde estaba. */
    private void abrir(String recurso, Object controlador, String titulo) {
        try {
            Scene escena = cargar(recurso, controlador);
            ventana.setScene(escena);
            ventana.setTitle(titulo);
        } catch (IOException exception) {
            avisar("No se pudo abrir la pantalla. Volvé a intentar.");
        }
    }

    private void volverAlMenu() {
        try {
            mostrarMenu();
        } catch (IOException exception) {
            avisar("No se pudo volver al menú. Reiniciá la aplicación.");
        }
    }

    private void cerrarSesion() {
        try {
            mostrarLogin();
        } catch (IOException exception) {
            sesion.cerrar();
            avisar("La sesión se cerró, pero no se pudo abrir el acceso. Reiniciá la aplicación.");
            ventana.close();
        }
    }

    private void avisar(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje);
        alerta.initOwner(ventana);
        alerta.showAndWait();
    }

    private Scene cargar(String recurso, Object controlador) throws IOException {
        var ubicacion = App.class.getResource(recurso);
        if (ubicacion == null) {
            throw new IOException("Falta una vista de la aplicación.");
        }
        FXMLLoader loader = new FXMLLoader(ubicacion);
        loader.setController(controlador);
        return new Scene(loader.load(), 850, 570);
    }

    public void cerrar() {
        if (loginActual != null) {
            loginActual.descartar();
            loginActual = null;
        }
        sesion.cerrar();
    }
}
