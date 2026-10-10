package com.comp.spiapp.controller;

import java.util.Arrays;
import java.util.function.Consumer;

import com.comp.spiapp.model.Usuario;
import com.comp.spiapp.service.AutenticacionException;
import com.comp.spiapp.service.AutenticacionService;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * CU-01: login
 */
public final class LoginController {
    private final AutenticacionService autenticacion;

    // Se ejecuta cuando el usuario inicia sesión correctamente
    // Recibe el objeto Usuario autenticado
    private final Consumer<Usuario> alIngresar;

    @FXML private TextField usuarioField;
    @FXML private PasswordField passwordField;
    @FXML private Button ingresarButton;
    @FXML private Label mensajeLabel;

    public LoginController(
            AutenticacionService autenticacion,
            Consumer<Usuario> alIngresar
    ) {
        this.autenticacion = autenticacion;
        this.alIngresar = alIngresar;
    }

    @FXML
    private void ingresar() {
        String login = usuarioField.getText();

        // A la contraseña la guardamos como un array de chars
        // para después escribirle arriba luego de usarla.
        char[] password = passwordField.getText().toCharArray();
        passwordField.clear();

        mensajeLabel.setText("");
        ocupado(true);

        // Esto lo hacemos asi para que no se congele la interfaz
        // cuando se intenta iniciar sesión.

        Task<Usuario> tarea = new Task<>() {
            @Override
            protected Usuario call() throws Exception {
                try {
                    return autenticacion.autenticar(login, password);
                } finally {
                    // Borrar la contraseña de la memoria
                    Arrays.fill(password, '\0');
                }
            }
        };

        // Autenticacion exitosa
        tarea.setOnSucceeded(e -> {
            ocupado(false);

            try {
                Usuario usuario = tarea.getValue();
                alIngresar.accept(usuario);
            } catch (RuntimeException ex) {
                mensajeLabel.setText(
                        "No se pudo abrir el menú. Vuelve a intentar."
                );
            }
        });

        // Autenticacion fallida
        tarea.setOnFailed(e -> {
            ocupado(false);

            Throwable ex = tarea.getException();

            if (ex instanceof AutenticacionException) {
                mensajeLabel.setText(ex.getMessage());
                passwordField.requestFocus();
            } else {
                mensajeLabel.setText(
                        "Ocurrió un error inesperado."
                );
                ex.printStackTrace();
            }
        });

        // Se ejecuta fuera del hilo de javafx
        Thread hilo = new Thread(tarea, "sigtoa-login");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void ocupado(boolean valor) {
        ingresarButton.setDisable(valor);
        usuarioField.setDisable(valor);
        passwordField.setDisable(valor);
    }

    public void descartar() {
        usuarioField.clear();
        passwordField.clear();
    }
}
