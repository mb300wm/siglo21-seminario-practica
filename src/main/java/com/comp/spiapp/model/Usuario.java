package com.comp.spiapp.model;

/**
 * Usuario
 */
public final class Usuario extends Catalogo {
    private final String login;
    private final String passwordHash;
    private final boolean activo;

    public Usuario(Integer id, String nombre, String login, String passwordHash, boolean activo)
        throws ValidacionException {
        super(id, nombre);
        new Errores()
            .texto(login, "El usuario de acceso")
            .texto(passwordHash, "La contraseña cifrada")
            .lanzarSiHay();
        this.login = login.trim();
        this.passwordHash = passwordHash;
        this.activo = activo;
    }

    public String getLogin() {
        return login;
    }
    public String getPasswordHash() {
        return passwordHash;
    }
    public boolean isActivo() {
        return activo;
    }
}
