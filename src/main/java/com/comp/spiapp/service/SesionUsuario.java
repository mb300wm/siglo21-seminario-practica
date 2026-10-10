package com.comp.spiapp.service;

import com.comp.spiapp.model.Usuario;

public final class SesionUsuario {
    private Usuario usuario;

    public void iniciar(Usuario usuario) {
        if (usuario == null || !usuario.isActivo() || usuario.getId() == null) {
            throw new IllegalArgumentException("La sesión requiere un usuario activo y persistido.");
        }
        this.usuario = usuario;
    }

    public boolean estaIniciada() {
        return usuario != null;
    }

    public Usuario requerirUsuario() {
        if (usuario == null) {
            throw new IllegalStateException("No hay una sesión iniciada.");
        }
        return usuario;
    }

    public void cerrar() {
        usuario = null;
    }
}
