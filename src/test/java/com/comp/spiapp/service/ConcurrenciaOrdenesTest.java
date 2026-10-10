package com.comp.spiapp.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.comp.spiapp.model.Lote;
import com.comp.spiapp.model.OrdenLote;
import com.comp.spiapp.model.OrdenTrabajo;
import com.comp.spiapp.model.Usuario;
import com.comp.spiapp.repository.CatalogoRepository;
import com.comp.spiapp.repository.CatalogosOrden;
import com.comp.spiapp.repository.DetalleOrdenRepository;
import com.comp.spiapp.repository.ConexionBD;
import com.comp.spiapp.repository.OrdenTrabajoRepository;
import com.comp.spiapp.repository.UsuarioRepository;

/**
 * P09 contra el MySQL real: dos sesiones confirman órdenes de la misma campaña al mismo tiempo.
 *
 * La sesión A bloquea la campaña, inserta su orden y demora el commit 1,5 segundos. Mientras tanto
 * la sesión B intenta numerar en la misma campaña: MySQL la hace esperar en el SELECT ... FOR UPDATE
 * hasta que A confirma, y recién entonces B lee el correlativo, que ya incluye la orden de A.
 *
 * Escribe en la base configurada en config/db.local.properties y al final borra las dos órdenes
 * (sus lotes se borran en cascada). Solo corre si se pide explícitamente:
 *     .\mvnw.cmd test "-Dtest=ConcurrenciaOrdenesTest" "-Dsigtoa.integracion=true"
 */
@EnabledIfSystemProperty(named = "sigtoa.integracion", matches = "true")
class ConcurrenciaOrdenesTest {
    private static final long DEMORA_DEL_COMMIT_MS = 1500;

    @Test
    void dosSesionesSimultaneasObtienenNumerosDistintosYConsecutivos() throws Exception {
        ConexionBD mysql = ConexionBD.desdeConfiguracion();
        Usuario usuario = new UsuarioRepository().buscarPorLogin("demo_encargado").orElse(null);
        Assumptions.assumeTrue(usuario != null, "Faltan los datos de 02_datos_de_prueba.sql");
        CatalogosOrden catalogos;
        try (Connection conexion = mysql.abrir()) {
            catalogos = new CatalogoRepository().cargarOrden(conexion);
        }

        CountDownLatch aTieneElBloqueo = new CountDownLatch(1);
        OrdenTrabajoService sesionA = servicio(() -> conCommitDemorado(mysql.abrir(), aTieneElBloqueo));
        OrdenTrabajoService sesionB = servicio(mysql);
        OrdenTrabajo ordenA = nuevaOrden(catalogos, usuario, "P09 sesión A");
        OrdenTrabajo ordenB = nuevaOrden(catalogos, usuario, "P09 sesión B");

        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<String> numeroA = hilos.submit(() -> sesionA.registrar(ordenA));
            assertTrue(aTieneElBloqueo.await(10, TimeUnit.SECONDS), "La sesión A no llegó a confirmar");
            long inicioB = System.nanoTime();
            Future<String> numeroB = hilos.submit(() -> sesionB.registrar(ordenB));

            String a = numeroA.get(30, TimeUnit.SECONDS);
            String b = numeroB.get(30, TimeUnit.SECONDS);
            long esperaB = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioB);
            assertNotEquals(a, b);
            assertEquals(correlativo(a) + 1, correlativo(b), "B recibe el número siguiente al de A");
            assertTrue(esperaB >= DEMORA_DEL_COMMIT_MS - 300,
                    "B tuvo que esperar a que A liberara la campaña (esperó " + esperaB + " ms)");
            System.out.printf("P09: sesión A = %s, sesión B = %s, B esperó %d ms%n", a, b, esperaB);
        } finally {
            // Se espera a que ambas sesiones terminen para borrar también la que haya quedado guardada.
            hilos.shutdown();
            hilos.awaitTermination(30, TimeUnit.SECONDS);
            borrar(mysql, ordenA.getId(), ordenB.getId());
        }
    }

    private static OrdenTrabajoService servicio(ConexionBD conexiones) {
        return new OrdenTrabajoService(conexiones, new CatalogoRepository(),
                new OrdenTrabajoRepository(), new DetalleOrdenRepository());
    }

    /** Conexión real que, al confirmar, avisa que ya tiene el bloqueo y demora el commit. */
    private static Connection conCommitDemorado(Connection real, CountDownLatch aviso) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (objeto, metodo, argumentos) -> {
                    if (metodo.getName().equals("commit")) {
                        aviso.countDown();
                        Thread.sleep(DEMORA_DEL_COMMIT_MS);
                    }
                    try {
                        return metodo.invoke(real, argumentos);
                    } catch (InvocationTargetException ex) {
                        throw ex.getCause();
                    }
                });
    }

    private static OrdenTrabajo nuevaOrden(CatalogosOrden catalogos, Usuario usuario, String comentario)
            throws Exception {
        var campania = catalogos.campanias().stream()
                .filter(c -> c.getNombre().equals("25-26")).findFirst().orElseThrow();
        Lote lote = catalogos.lotes().getFirst();
        OrdenTrabajo orden = new OrdenTrabajo(null, null, LocalDate.now(), catalogos.labores().getFirst(),
                lote.getEstablecimiento(), catalogos.contratistas().getFirst(), null, null, campania, null,
                comentario, usuario);
        orden.agregarLote(new OrdenLote(null, lote, null, new BigDecimal("1")));
        return orden;
    }

    private static int correlativo(String numero) {
        return Integer.parseInt(numero.substring(numero.length() - 4));
    }

    /** Limpieza: las órdenes de prueba no quedan en la base. */
    private static void borrar(ConexionBD mysql, Integer... ids) throws Exception {
        try (Connection conexion = mysql.abrir();
             var sentencia = conexion.prepareStatement("DELETE FROM ordenes_trabajo WHERE orden_id = ?")) {
            for (Integer id : ids) {
                if (id != null) {
                    sentencia.setInt(1, id);
                    sentencia.executeUpdate();
                }
            }
        }
    }
}
