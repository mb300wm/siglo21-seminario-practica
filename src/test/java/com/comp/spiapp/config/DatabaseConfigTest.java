package com.comp.spiapp.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {
    @TempDir Path directory;

    @Test
    void permitsLocalDefaultsWhenNoFileExists() throws Exception {
        DatabaseConfig config = DatabaseConfig.load(directory.resolve("missing.properties"), Map.of());
        assertEquals("jdbc:mysql://localhost:3306/agro", config.url());
        assertEquals("sigtoa_app", config.user());
        assertEquals("sigtoa-app-2026", config.password());
    }

    @Test
    void readsUtf8AndPreservesEscapedPasswordCharacters() throws Exception {
        Path file = directory.resolve("db.properties");
        Files.writeString(file, "db.user= usuario \ndb.password=\\ espacio-é\\\\fin\n", StandardCharsets.UTF_8);
        DatabaseConfig config = DatabaseConfig.load(file, Map.of());
        assertEquals("usuario", config.user());
        assertEquals(" espacio-é\\fin", config.password());
    }

    @Test
    void environmentOverridesFileIncludingAnExplicitEmptyPassword() throws Exception {
        Path file = directory.resolve("db.properties");
        Files.writeString(file, "db.url=jdbc:mysql://localhost:3306/agro\ndb.user=root\ndb.password=ejemplo\n");
        DatabaseConfig config = DatabaseConfig.load(file, Map.of(
                "SIGTOA_DB_URL", "jdbc:mysql://localhost:3307/agro_prueba",
                "SIGTOA_DB_USER", "prueba", "SIGTOA_DB_PASSWORD", ""));
        assertEquals("jdbc:mysql://localhost:3307/agro_prueba", config.url());
        assertEquals("prueba", config.user());
        assertEquals("", config.password());
    }

    @Test
    void rejectsAnInvalidUrlOrMissingUserBeforeOpeningAConnection() {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseConfig("root@localhost:3306", "root", ""));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseConfig("jdbc:mysql://localhost/agro", "  ", ""));
    }

    @Test
    void diagnosticsDoNotExposeConnectionValues() {
        DatabaseConfig config = new DatabaseConfig("jdbc:mysql://localhost/agro", "usuario-prueba", "secreto-prueba");
        assertFalse(config.toString().contains(config.password()));
        assertFalse(config.toString().contains(config.url()));
        assertFalse(config.toString().contains(config.user()));
    }
}
