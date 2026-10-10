package com.comp.spiapp.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Configuración local
 */
public final class DatabaseConfig {
    public static final Path LOCAL_FILE = Path.of("config", "db.local.properties");
    private final String url;
    private final String user;
    private final String password;

    public DatabaseConfig(String url, String user, String password) {
        this.url = Objects.requireNonNull(url).trim();
        this.user = Objects.requireNonNull(user).trim();
        this.password = Objects.requireNonNull(password);
        if (!this.url.startsWith("jdbc:mysql://") || this.user.isBlank()) {
            throw new IllegalArgumentException("Revisá db.url y db.user en config/db.local.properties.");
        }
    }

    public static DatabaseConfig load() throws IOException {
        return load(LOCAL_FILE, System.getenv());
    }

    static DatabaseConfig load(Path file, Map<String, String> environment) throws IOException {
        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        return new DatabaseConfig(
                environment.getOrDefault("SIGTOA_DB_URL", properties.getProperty("db.url", "jdbc:mysql://localhost:3306/agro")),
                environment.getOrDefault("SIGTOA_DB_USER", properties.getProperty("db.user", "sigtoa_app")),
                environment.getOrDefault("SIGTOA_DB_PASSWORD", properties.getProperty("db.password", "sigtoa-app-2026")));
    }

    public String url() { return url; }
    public String user() { return user; }
    public String password() { return password; }

    @Override
    public String toString() {
        return "DatabaseConfig[credenciales ocultas]";
    }
}
