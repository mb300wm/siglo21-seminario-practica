package com.comp.spiapp.service;

import java.sql.SQLException;
import java.util.Objects;

import com.comp.spiapp.model.Usuario;
import com.comp.spiapp.repository.ConsultaUsuario;

public final class AutenticacionService {
    private final ConsultaUsuario usuarios;
    private final PasswordHasher passwords;

    public AutenticacionService(ConsultaUsuario usuarios, PasswordHasher passwords) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.passwords = Objects.requireNonNull(passwords);
    }

    public Usuario autenticar(String login, char[] password) throws AutenticacionException {
        if (login == null || login.isBlank() || password == null || password.length == 0) {
            throw new AutenticacionException("Completá el usuario y la contraseña.");
        }
        if (login.trim().length() > 50 || password.length > PasswordHasher.MAX_PASSWORD_LENGTH) {
            throw new AutenticacionException("El usuario o la contraseña supera la longitud permitida.");
        }
        try {
            var usuario = usuarios.buscarPorLogin(login.trim());
            if (usuario.isEmpty() || !usuario.get().isActivo()
                    || !passwords.verificar(password, usuario.get().getPasswordHash())) {
                throw new AutenticacionException("No se pudo iniciar sesión. Revisá tus credenciales.");
            }
            return usuario.get();
        } catch (SQLException exception) {
            throw new AutenticacionException(
                    "No se pudo consultar el acceso. Revisá la conexión y la configuración de MySQL.");
        }
    }
}
