package com.comp.spiapp.service;

import java.nio.CharBuffer;

import com.password4j.Argon2Function;
import com.password4j.Password;
import com.password4j.types.Argon2;

public final class PasswordHasher {
    public static final int MAX_PASSWORD_LENGTH = 256;

    private static final Argon2Function ARGON2 =
        Argon2Function.getInstance(19456, 2, 1, 32, Argon2.ID, 19);

    public String generar(char[] password) {
        if (password == null || password.length == 0 || password.length > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener entre 1 y 256 caracteres.");
        }
        return Password.hash(CharBuffer.wrap(password)).addRandomSalt(16).with(ARGON2).getResult();
    }

    public boolean verificar(char[] password, String hash) {
        if (password == null || password.length == 0 || password.length > MAX_PASSWORD_LENGTH
            || hash == null || !hash.startsWith("$argon2id")) {
            return false;
        }
        try {
            return Password.check(CharBuffer.wrap(password), hash).with(ARGON2);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
