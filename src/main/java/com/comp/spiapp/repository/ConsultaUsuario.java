package com.comp.spiapp.repository;

import java.sql.SQLException;
import java.util.Optional;

import com.comp.spiapp.model.Usuario;

@FunctionalInterface
public interface ConsultaUsuario {

    Optional<Usuario> buscarPorLogin(String login) throws SQLException;
}