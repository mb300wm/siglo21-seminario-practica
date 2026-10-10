package com.comp.spiapp;

import com.comp.spiapp.repository.UsuarioRepository;
import com.comp.spiapp.service.AutenticacionService;
import com.comp.spiapp.service.ConsultaService;
import com.comp.spiapp.service.CosechaService;
import com.comp.spiapp.service.OrdenTrabajoService;
import com.comp.spiapp.service.PasswordHasher;
import com.comp.spiapp.service.SesionUsuario;
import com.comp.spiapp.service.SiembraService;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    private Navegador navegador;

    @Override
    public void start(Stage stage) throws IOException {
        var autenticacion = new AutenticacionService(new UsuarioRepository(), new PasswordHasher());
        navegador = new Navegador(stage, autenticacion, new SesionUsuario(),
            new SiembraService(), new CosechaService(), new OrdenTrabajoService(), new ConsultaService());
        stage.setMinWidth(720);
        stage.setMinHeight(540);
        stage.setOnCloseRequest(e -> navegador.cerrar());
        navegador.mostrarLogin();
        stage.show();
    }

    @Override
    public void stop() {
        if (navegador != null) {
            navegador.cerrar();
        }
    }
}
