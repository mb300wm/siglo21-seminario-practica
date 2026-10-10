package com.comp.spiapp.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    private final PasswordHasher passwords = new PasswordHasher();

    @Test
    void guardaSalAleatoriaYVerificaLaContraseniaCompleta() {
        char[] password = " Ñandú largo 123! ".toCharArray();
        String primero = passwords.generar(password);
        String segundo = passwords.generar(password);
        assertNotEquals(primero, segundo);
        assertTrue(primero.length() <= 255);
        assertTrue(passwords.verificar(password, primero));
        assertTrue(passwords.verificar(password, segundo));
        assertFalse(passwords.verificar("Ñandú largo 123!".toCharArray(), primero));
        assertFalse(passwords.verificar("incorrecta".toCharArray(), primero));
    }

    @Test
    void rechazaMarcadoresHistoricosHashesMalformadosYCostosNoAdmitidos() {
        char[] password = "clave".toCharArray();
        for (String hash : new String[]{"SIN_LOGIN_DATOS_DE_PRUEBA", "clave", "", "$argon2id$basura",
                "$argon2id$v=19$m=2147483647,t=99999,p=1$AAAA$BBBB"}) {
            assertFalse(passwords.verificar(password, hash));
        }
        assertFalse(passwords.verificar(password, null));
        assertFalse(passwords.verificar(null, "cualquier hash"));
        assertThrows(IllegalArgumentException.class, () -> passwords.generar(new char[0]));
        assertThrows(IllegalArgumentException.class, () -> passwords.generar(new char[257]));
    }
}
