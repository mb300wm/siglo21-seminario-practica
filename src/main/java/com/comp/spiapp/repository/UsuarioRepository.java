package com.comp.spiapp.repository;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import com.comp.spiapp.model.Usuario;
import com.comp.spiapp.model.ValidacionException;

public final class UsuarioRepository implements ConsultaUsuario {
    private final ConexionBD conexiones;

    public UsuarioRepository() {
        this(ConexionBD.desdeConfiguracion());
    }

    public UsuarioRepository(ConexionBD conexiones) {
        this.conexiones = Objects.requireNonNull(conexiones);
    }

    @Override 
    public Optional<Usuario> buscarPorLogin(String login) throws SQLException {
        String sql = """
                SELECT usuario_id, nombre, usuario_login, password_hash, activo
                FROM usuarios
                WHERE usuario_login = ?
                """;
        try (var conexion = conexiones.abrir();
             var consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, login);

            try (var filas = consulta.executeQuery()) {
                if (!filas.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Usuario(
                    filas.getInt("usuario_id"), filas.getString("nombre"),
                    filas.getString("usuario_login"), filas.getString("password_hash"),
                    filas.getBoolean("activo")));
            }
        } catch (ValidacionException ex) {
            throw new SQLException("El registro de usuario contiene datos inválidos.", ex);
        }
    }
}
