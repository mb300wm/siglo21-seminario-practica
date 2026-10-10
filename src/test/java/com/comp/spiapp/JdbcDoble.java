package com.comp.spiapp;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base de datos simulada para las pruebas: registra sentencias, parámetros, commit/rollback
 * y cierres, sin conectarse a MySQL. Un método JDBC no previsto hace fallar la prueba.
 */
public final class JdbcDoble {
    /** Una ejecución de INSERT o UPDATE con sus parámetros por posición (un valor null es un setNull). */
    public record Insercion(String sql, Map<Integer, Object> parametros) {
        public Object parametro(int indice) {
            if (!parametros.containsKey(indice)) {
                throw new AssertionError("No se cargó el parámetro " + indice);
            }
            return parametros.get(indice);
        }
    }

    public final List<Insercion> inserciones = Collections.synchronizedList(new ArrayList<>());
    public final List<String> consultas = Collections.synchronizedList(new ArrayList<>());
    public final List<String> eventos = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, List<Map<String, Object>>> resultados = new LinkedHashMap<>();
    private int conexionesAbiertas;
    private int fallarEnInsercion;
    private String estadoDelFallo;
    private String mensajeDelFallo;
    private int insercionesIntentadas;
    private int siguienteClave = 100;

    /**
     * Las consultas cuyo SQL contiene el fragmento devuelven estas filas; el resto, ninguna.
     * Si coinciden varios fragmentos gana el más largo, que es el más específico.
     */
    public JdbcDoble resultado(String fragmentoSql, List<Map<String, Object>> filas) {
        resultados.put(fragmentoSql, filas);
        return this;
    }

    /** Hace fallar el n-ésimo INSERT (empezando en 1), como un corte de conexión. */
    public JdbcDoble fallarEnInsercion(int numero) {
        return fallarEnInsercion(numero, "08S01", "Fallo simulado de conexión");
    }

    /** Permite simular el rechazo de un disparador (SQLSTATE 45000) con su mensaje. */
    public JdbcDoble fallarEnInsercion(int numero, String estadoSql, String mensaje) {
        fallarEnInsercion = numero;
        estadoDelFallo = estadoSql;
        mensajeDelFallo = mensaje;
        return this;
    }

    public synchronized int conexionesAbiertas() {
        return conexionesAbiertas;
    }

    public synchronized Connection abrir() {
        conexionesAbiertas++;
        return doble(Connection.class, (objeto, metodo, argumentos) -> switch (metodo.getName()) {
            case "prepareStatement" -> sentencia((String) argumentos[0]);
            case "setAutoCommit" -> registrar("autocommit:" + argumentos[0]);
            case "commit" -> registrar("commit");
            case "rollback" -> registrar("rollback");
            case "close" -> registrar("cerrar:conexion");
            default -> throw new AssertionError("Método JDBC inesperado: Connection." + metodo.getName());
        });
    }

    private PreparedStatement sentencia(String sql) {
        Map<Integer, Object> parametros = new HashMap<>();
        int[] clave = {0};
        return doble(PreparedStatement.class, (objeto, metodo, argumentos) -> switch (metodo.getName()) {
            case "setInt", "setString", "setBigDecimal", "setObject" -> {
                parametros.put((Integer) argumentos[0], argumentos[1]);
                yield null;
            }
            case "setNull" -> {
                parametros.put((Integer) argumentos[0], null);
                yield null;
            }
            case "executeUpdate" -> {
                synchronized (this) {
                    insercionesIntentadas++;
                    if (insercionesIntentadas == fallarEnInsercion) {
                        throw new SQLException(mensajeDelFallo, estadoDelFallo);
                    }
                    clave[0] = siguienteClave++;
                }
                inserciones.add(new Insercion(sql, new HashMap<>(parametros)));
                parametros.clear();
                yield 1;
            }
            case "executeQuery" -> {
                consultas.add(sql);
                eventos.add("consulta:" + sql);
                yield filas(filasPara(sql));
            }
            case "getGeneratedKeys" -> filas(List.of(Map.of("1", clave[0])));
            case "close" -> registrar("cerrar:sentencia");
            default -> throw new AssertionError("Método JDBC inesperado: PreparedStatement." + metodo.getName());
        });
    }

    private List<Map<String, Object>> filasPara(String sql) {
        String elegido = null;
        for (String fragmento : resultados.keySet()) {
            if (sql.contains(fragmento) && (elegido == null || fragmento.length() > elegido.length())) {
                elegido = fragmento;
            }
        }
        return elegido == null ? List.of() : resultados.get(elegido);
    }

    private ResultSet filas(List<Map<String, Object>> filas) {
        int[] actual = {-1};
        return doble(ResultSet.class, (objeto, metodo, argumentos) -> {
            String nombre = metodo.getName();
            if (nombre.equals("next")) {
                return ++actual[0] < filas.size();
            }
            if (nombre.equals("close")) {
                return registrar("cerrar:filas");
            }
            Map<String, Object> fila = filas.get(actual[0]);
            String columna = String.valueOf(argumentos[0]);
            if (!fila.containsKey(columna)) {
                throw new AssertionError("Columna inesperada: " + columna);
            }
            Object valor = fila.get(columna);
            return switch (nombre) {
                case "getInt" -> valor == null ? 0 : (Integer) valor;
                case "getBoolean" -> valor != null && (Boolean) valor;
                case "getString" -> (String) valor;
                case "getBigDecimal" -> (BigDecimal) valor;
                case "getObject" -> ((Class<?>) argumentos[1]).cast(valor);
                default -> throw new AssertionError("Método JDBC inesperado: ResultSet." + nombre);
            };
        });
    }

    private Object registrar(String evento) {
        eventos.add(evento);
        return null;
    }

    private static <T> T doble(Class<T> tipo, InvocationHandler manejador) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, manejador));
    }
}
