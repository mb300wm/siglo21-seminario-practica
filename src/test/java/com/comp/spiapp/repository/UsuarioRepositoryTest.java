package com.comp.spiapp.repository;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Dobles JDBC estrictos: verifican SQL, parámetros, mapeo y cierre, sin usar la base local. */
class UsuarioRepositoryTest {
    @Test
    void consultaParametrizadaMapeaTodosLosCamposYCierraRecursos() throws Exception {
        var jdbc = new JdbcDoble();
        String login = "' OR 1=1 --";
        var usuario = new UsuarioRepository(jdbc::abrir).buscarPorLogin(login).orElseThrow();
        assertTrue(jdbc.sql.contains("WHERE usuario_login = ?"));
        assertFalse(jdbc.sql.contains(login));
        assertEquals(login, jdbc.parametro);
        assertEquals(8, usuario.getId());
        assertEquals("Usuario de prueba", usuario.getNombre());
        assertEquals("demo", usuario.getLogin());
        assertEquals("hash-de-prueba", usuario.getPasswordHash());
        assertFalse(usuario.isActivo());
        assertEquals(List.of("filas", "consulta", "conexion"), jdbc.cerrados);
    }

    @Test
    void devuelveVacioSiNoHayFilaYCierraRecursos() throws Exception {
        var jdbc = new JdbcDoble();
        jdbc.existe = false;
        assertTrue(new UsuarioRepository(jdbc::abrir).buscarPorLogin("nadie").isEmpty());
        assertEquals(List.of("filas", "consulta", "conexion"), jdbc.cerrados);
    }

    @Test
    void cierraRecursosAnteErroresDeConsultaYDeMapeo() {
        var consultaFallida = new JdbcDoble();
        consultaFallida.fallarConsulta = true;
        assertThrows(SQLException.class, () -> new UsuarioRepository(consultaFallida::abrir).buscarPorLogin("demo"));
        assertEquals(List.of("consulta", "conexion"), consultaFallida.cerrados);

        var filaInvalida = new JdbcDoble();
        filaInvalida.nombre = " ";
        assertThrows(SQLException.class, () -> new UsuarioRepository(filaInvalida::abrir).buscarPorLogin("demo"));
        assertEquals(List.of("filas", "consulta", "conexion"), filaInvalida.cerrados);
    }

    private static final class JdbcDoble {
        String sql;
        String parametro;
        String nombre = "Usuario de prueba";
        boolean existe = true;
        boolean fallarConsulta;
        final List<String> cerrados = new ArrayList<>();

        Connection abrir() {
            return doble(Connection.class, (objeto, metodo, argumentos) -> switch (metodo.getName()) {
                case "prepareStatement" -> { sql = (String) argumentos[0]; yield consulta(); }
                case "close" -> { cerrados.add("conexion"); yield null; }
                default -> throw new AssertionError("Método JDBC inesperado: " + metodo.getName());
            });
        }

        PreparedStatement consulta() {
            return doble(PreparedStatement.class, (objeto, metodo, argumentos) -> switch (metodo.getName()) {
                case "setString" -> {
                    assertEquals(1, argumentos[0]);
                    parametro = (String) argumentos[1];
                    yield null;
                }
                case "executeQuery" -> {
                    if (fallarConsulta) { throw new SQLException("Fallo simulado"); }
                    yield filas();
                }
                case "close" -> { cerrados.add("consulta"); yield null; }
                default -> throw new AssertionError("Método JDBC inesperado: " + metodo.getName());
            });
        }

        ResultSet filas() {
            return doble(ResultSet.class, (objeto, metodo, argumentos) -> switch (metodo.getName()) {
                case "next" -> existe;
                case "getInt" -> { assertEquals("usuario_id", argumentos[0]); yield 8; }
                case "getBoolean" -> { assertEquals("activo", argumentos[0]); yield false; }
                case "getString" -> switch ((String) argumentos[0]) {
                    case "nombre" -> nombre;
                    case "usuario_login" -> "demo";
                    case "password_hash" -> "hash-de-prueba";
                    default -> throw new AssertionError("Columna inesperada");
                };
                case "close" -> { cerrados.add("filas"); yield null; }
                default -> throw new AssertionError("Método JDBC inesperado: " + metodo.getName());
            });
        }

        private <T> T doble(Class<T> tipo, java.lang.reflect.InvocationHandler handler) {
            return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, handler));
        }
    }
}
