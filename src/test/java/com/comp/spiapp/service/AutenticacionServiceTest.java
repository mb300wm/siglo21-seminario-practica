package com.comp.spiapp.service;

import com.comp.spiapp.model.Usuario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class AutenticacionServiceTest {
    private static final PasswordHasher PASSWORDS = new PasswordHasher();
    private static final char[] CLAVE = "Mi clave de prueba 42".toCharArray();
    private static Usuario activo;
    private static Usuario inactivo;

    @BeforeAll
    static void prepararUsuarios() throws Exception {
        String hash = PASSWORDS.generar(CLAVE);
        activo = new Usuario(3, "Mateo", "mateo", hash, true);
        inactivo = new Usuario(4, "Histórico", "historico", hash, false);
    }

    @Test
    void admiteAlActivoConClaveCorrectaYRecortaSoloElLogin() throws Exception {
        var servicio = new AutenticacionService(login -> {
            assertEquals("mateo", login);
            return Optional.of(activo);
        }, PASSWORDS);
        assertSame(activo, servicio.autenticar(" mateo ", CLAVE));
    }

    @Test
    void rechazaInexistenteInactivoYClaveIncorrectaConElMismoMensaje() {
        String inexistente = error(new AutenticacionService(login -> Optional.empty(), PASSWORDS), CLAVE);
        String deshabilitado = error(new AutenticacionService(login -> Optional.of(inactivo), PASSWORDS), CLAVE);
        String claveErronea = error(new AutenticacionService(login -> Optional.of(activo), PASSWORDS), "otra".toCharArray());
        assertEquals(inexistente, deshabilitado);
        assertEquals(inexistente, claveErronea);
    }

    @Test
    void rechazaCamposVaciosYLargosAntesDeConsultarLaBase() {
        var servicio = new AutenticacionService(login -> {
            fail("No debería consultar MySQL");
            return Optional.empty();
        }, PASSWORDS);
        assertThrows(AutenticacionException.class, () -> servicio.autenticar(" ", CLAVE));
        assertThrows(AutenticacionException.class, () -> servicio.autenticar(null, CLAVE));
        assertThrows(AutenticacionException.class, () -> servicio.autenticar("mateo", new char[0]));
        assertThrows(AutenticacionException.class, () -> servicio.autenticar("mateo", null));
        assertThrows(AutenticacionException.class, () -> servicio.autenticar("x".repeat(51), CLAVE));
        assertThrows(AutenticacionException.class, () -> servicio.autenticar("mateo", new char[257]));
    }

    @Test
    void ocultaDetallesDelErrorJdbc() {
        var servicio = new AutenticacionService(login -> {
            throw new SQLException("Detalles internos: credencial-db-sensible");
        }, PASSWORDS);
        String mensaje = error(servicio, CLAVE);
        assertTrue(mensaje.contains("MySQL"));
        assertFalse(mensaje.contains("credencial-db-sensible"));
    }

    private String error(AutenticacionService servicio, char[] clave) {
        return assertThrows(AutenticacionException.class, () -> servicio.autenticar("mateo", clave)).getMessage();
    }
}
