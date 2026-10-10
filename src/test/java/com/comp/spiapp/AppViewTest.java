package com.comp.spiapp;

import com.comp.spiapp.controller.CosechaController;
import com.comp.spiapp.controller.MenuController;
import com.comp.spiapp.controller.OrdenTrabajoController;
import com.comp.spiapp.controller.SiembraController;
import com.comp.spiapp.controller.TrazabilidadController;
import com.comp.spiapp.model.AplicacionInsumo;
import com.comp.spiapp.model.OrdenLote;
import com.comp.spiapp.model.Pesaje;
import com.comp.spiapp.model.Usuario;
import com.comp.spiapp.repository.CatalogoRepository;
import com.comp.spiapp.repository.CosechaRepository;
import com.comp.spiapp.repository.DetalleOrdenRepository;
import com.comp.spiapp.repository.OrdenTrabajoRepository;
import com.comp.spiapp.repository.PesajeRepository;
import com.comp.spiapp.repository.SiembraRepository;
import com.comp.spiapp.repository.TrazabilidadRepository;
import com.comp.spiapp.service.AutenticacionService;
import com.comp.spiapp.service.ConsultaService;
import com.comp.spiapp.service.CosechaService;
import com.comp.spiapp.service.OrdenTrabajoService;
import com.comp.spiapp.service.PasswordHasher;
import com.comp.spiapp.service.SesionUsuario;
import com.comp.spiapp.service.SiembraService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** Recorre vistas y controladores reales, sin abrir ventanas ni consultar MySQL. */
class AppViewTest {
    private final JdbcDoble jdbc = Ejemplos.conTrazabilidad(Ejemplos.conOrdenes(new JdbcDoble()));
    private final SiembraService siembras =
            new SiembraService(jdbc::abrir, new CatalogoRepository(), new SiembraRepository());
    private final CosechaService cosechas = new CosechaService(jdbc::abrir, new SiembraRepository(),
            new CatalogoRepository(), new CosechaRepository(), new PesajeRepository());
    private final OrdenTrabajoService ordenes = new OrdenTrabajoService(jdbc::abrir, new CatalogoRepository(),
            new OrdenTrabajoRepository(), new DetalleOrdenRepository());
    private final ConsultaService consultas =
            new ConsultaService(jdbc::abrir, new CatalogoRepository(), new TrazabilidadRepository());

    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.countDown();
        });
        assertTrue(started.await(10, TimeUnit.SECONDS));
    }

    @AfterAll
    static void stopToolkit() { Platform.exit(); }

    @Test
    void loginAsincronoAbreMenuYCerrarSesionBorraLaIdentidadYLosCampos() throws Exception {
        PasswordHasher passwords = new PasswordHasher();
        Usuario usuario = new Usuario(3, "Mateo", "mateo", passwords.generar("clave de prueba 42".toCharArray()), true);
        var sesion = new SesionUsuario();
        var consultando = new CountDownLatch(1);
        var continuar = new CountDownLatch(1);
        var enMenu = new CountDownLatch(1);
        var llamadas = new AtomicInteger();
        var hiloFx = new AtomicReference<Boolean>();
        var servicio = new AutenticacionService(login -> {
            llamadas.incrementAndGet();
            hiloFx.set(Platform.isFxApplicationThread());
            consultando.countDown();
            try {
                if (!continuar.await(10, TimeUnit.SECONDS)) { throw new AssertionError("Espera agotada"); }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
            return Optional.of(usuario);
        }, passwords);
        Stage ventana = enFx(() -> {
            Stage stage = new Stage();
            Navegador navegador = new Navegador(stage, servicio, sesion, siembras, cosechas, ordenes, consultas);
            navegador.mostrarLogin();
            stage.sceneProperty().addListener((observable, anterior, actual) -> {
                if (actual.lookup("#usuarioLabel") != null) { enMenu.countDown(); }
            });
            completarLogin(stage.getScene(), "mateo", "clave de prueba 42");
            return stage;
        });
        try {
            assertTrue(consultando.await(10, TimeUnit.SECONDS));
            enFx(() -> {
                Scene scene = ventana.getScene();
                assertTrue(((Button) scene.lookup("#ingresarButton")).isDisabled());
                assertTrue(((TextField) scene.lookup("#usuarioField")).isDisabled());
                assertEquals("", ((PasswordField) scene.lookup("#passwordField")).getText());
                ((Button) scene.lookup("#ingresarButton")).fire();
                assertFalse(sesion.estaIniciada());
                return null;
            });
            continuar.countDown();
            assertTrue(enMenu.await(10, TimeUnit.SECONDS));
            enFx(() -> {
                Scene menu = ventana.getScene();
                menu.getRoot().applyCss();
                menu.getRoot().layout();
                assertSame(usuario, sesion.requerirUsuario());
                assertEquals("Mateo (mateo)", ((Label) menu.lookup("#usuarioLabel")).getText());
                for (String id : new String[]{"siembras", "cosechas", "ordenes", "aplicaciones", "trazabilidad"}) {
                    assertFalse(((Button) menu.lookup("#" + id + "Button")).isDisabled());
                }
                ((Button) menu.lookup("#cerrarSesionButton")).fire();
                assertFalse(sesion.estaIniciada());
                assertEquals("", ((TextField) ventana.getScene().lookup("#usuarioField")).getText());
                assertEquals("", ((PasswordField) ventana.getScene().lookup("#passwordField")).getText());
                assertFalse(((Button) ventana.getScene().lookup("#ingresarButton")).isDisabled());
                return null;
            });
            assertEquals(1, llamadas.get());
            assertEquals(Boolean.FALSE, hiloFx.get());
        } finally {
            continuar.countDown();
            enFx(() -> { ventana.close(); return null; });
        }
    }

    @Test
    void errorDeAccesoPermiteReintentarSinCrearSesion() throws Exception {
        var sesion = new SesionUsuario();
        var terminado = new CountDownLatch(1);
        Stage ventana = enFx(() -> {
            Stage stage = new Stage();
            var servicio = new AutenticacionService(login -> Optional.empty(), new PasswordHasher());
            new Navegador(stage, servicio, sesion, siembras, cosechas, ordenes, consultas).mostrarLogin();
            Button ingresar = (Button) stage.getScene().lookup("#ingresarButton");
            ingresar.disabledProperty().addListener((observable, antes, ahora) -> {
                if (!ahora) { terminado.countDown(); }
            });
            completarLogin(stage.getScene(), "nadie", "incorrecta");
            return stage;
        });
        try {
            assertTrue(terminado.await(10, TimeUnit.SECONDS));
            enFx(() -> {
                assertFalse(sesion.estaIniciada());
                assertTrue(((Label) ventana.getScene().lookup("#mensajeLabel")).getText().contains("credenciales"));
                assertFalse(((PasswordField) ventana.getScene().lookup("#passwordField")).isDisabled());
                assertEquals("", ((PasswordField) ventana.getScene().lookup("#passwordField")).getText());
                return null;
            });
        } finally {
            enFx(() -> { ventana.close(); return null; });
        }
    }

    @Test
    void menuRequiereUnaSesion() throws Exception {
        enFx(() -> {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("menu.fxml"));
            loader.setController(new MenuController(new SesionUsuario(), () -> fail("No debería cerrar sesión"),
                    () -> fail("No debería abrir siembra"), () -> fail("No debería abrir cosecha"),
                    () -> fail("No debería abrir órdenes"), () -> fail("No debería abrir aplicaciones"),
                    () -> fail("No debería abrir trazabilidad")));
            assertThrows(java.io.IOException.class, loader::load);
            return null;
        });
    }

    @Test
    void elMenuAbreSiembraYCosechaYVuelve() throws Exception {
        var sesion = new SesionUsuario();
        var passwords = new PasswordHasher();
        var usuario = new Usuario(7, "Mateo", "mateo", passwords.generar("clave".toCharArray()), true);
        var servicio = new AutenticacionService(login -> Optional.of(usuario), passwords);
        Stage ventana = enFx(() -> {
            Stage stage = new Stage();
            new Navegador(stage, servicio, sesion, siembras, cosechas, ordenes, consultas).mostrarLogin();
            completarLogin(stage.getScene(), "mateo", "clave");
            return stage;
        });
        try {
            esperar(() -> ventana.getScene().lookup("#siembrasButton") != null);
            enFx(() -> {
                ((Button) ventana.getScene().lookup("#siembrasButton")).fire();
                assertEquals("SIGTOA — Registrar siembra", ventana.getTitle());
                preparar(ventana.getScene());
                ((Button) ventana.getScene().lookup("#volverButton")).fire();
                assertEquals("SIGTOA — Menú principal", ventana.getTitle());
                ((Button) ventana.getScene().lookup("#cosechasButton")).fire();
                assertEquals("SIGTOA — Registrar cosecha", ventana.getTitle());
                preparar(ventana.getScene());
                ((Button) ventana.getScene().lookup("#volverButton")).fire();
                ((Button) ventana.getScene().lookup("#ordenesButton")).fire();
                assertEquals("SIGTOA — Orden de trabajo", ventana.getTitle());
                preparar(ventana.getScene());
                ((Button) ventana.getScene().lookup("#volverButton")).fire();
                ((Button) ventana.getScene().lookup("#aplicacionesButton")).fire();
                assertEquals("SIGTOA — Aplicar insumos", ventana.getTitle());
                preparar(ventana.getScene());
                ((Button) ventana.getScene().lookup("#volverButton")).fire();
                ((Button) ventana.getScene().lookup("#trazabilidadButton")).fire();
                assertEquals("SIGTOA — Trazabilidad por campaña", ventana.getTitle());
                assertTrue(sesion.estaIniciada());
                return null;
            });
        } finally {
            enFx(() -> { ventana.close(); return null; });
        }
    }

    /** P02 y alternativa A1: un error indica el campo y no borra lo cargado. */
    @Test
    void siembraConservaLoCargadoYGuardaConElUsuarioDeLaSesion() throws Exception {
        Scene escena = enFx(() -> cargar("siembra.fxml",
                new SiembraController(siembras, sesionIniciada(), () -> fail("No debería volver"))));
        esperar(() -> !combo(escena, "#campaniaCombo").getItems().isEmpty());
        enFx(() -> {
            campo(escena, "#tractorField").setText("Tractor de prueba");
            campo(escena, "#semillaKgField").setText("seiscientos");
            boton(escena, "#guardarButton").fire();
            assertTrue(mensaje(escena).contains("La semilla total (kg) debe ser un número"));
            assertTrue(conError(escena, "#semillaKgField"));
            assertEquals("Tractor de prueba", campo(escena, "#tractorField").getText());

            campo(escena, "#semillaKgField").setText("600");
            boton(escena, "#guardarButton").fire();
            assertTrue(mensaje(escena).contains("La campaña es un dato obligatorio."));
            assertTrue(conError(escena, "#campaniaCombo"));
            assertFalse(conError(escena, "#semillaKgField"));
            assertTrue(jdbc.inserciones.isEmpty());

            combo(escena, "#campaniaCombo").getSelectionModel().selectFirst();
            combo(escena, "#establecimientoCombo").getSelectionModel().selectFirst();
            assertEquals(2, combo(escena, "#loteCombo").getItems().size(), "solo los lotes de ESTANCIA CHARRA");
            combo(escena, "#loteCombo").getSelectionModel().selectFirst();
            combo(escena, "#semillaCombo").getSelectionModel().selectFirst();
            assertEquals("Maíz", ((Label) escena.lookup("#cultivoLabel")).getText());
            ((DatePicker) escena.lookup("#fechaInicioPicker")).setValue(LocalDate.of(2025, 10, 15));
            ((DatePicker) escena.lookup("#fechaFinPicker")).setValue(LocalDate.of(2025, 10, 16));
            boton(escena, "#guardarButton").fire();
            return null;
        });
        esperar(() -> mensaje(escena).contains("registrada"));
        enFx(() -> {
            assertEquals("Siembra registrada con el número 100.", mensaje(escena));
            var insercion = jdbc.inserciones.getFirst();
            assertEquals(7, insercion.parametro(22));
            assertEquals("Tractor de prueba", insercion.parametro(7));
            assertNull(insercion.parametro(6));
            assertNotNull(combo(escena, "#campaniaCombo").getValue(), "la campaña se conserva para la próxima hoja");
            assertNull(combo(escena, "#loteCombo").getValue());
            assertEquals("", campo(escena, "#tractorField").getText());
            return null;
        });
    }

    /** P03, alternativa A2 y transacción de CU-03 desde la pantalla. */
    @Test
    @SuppressWarnings("unchecked")
    void cosechaCalculaNetosTotalYRindeYGuardaLosPesajes() throws Exception {
        Scene escena = enFx(() -> cargar("cosecha.fxml",
                new CosechaController(cosechas, sesionIniciada(), () -> fail("No debería volver"))));
        esperar(() -> !combo(escena, "#siembraCombo").getItems().isEmpty());
        enFx(() -> {
            var tabla = (TableView<Pesaje>) escena.lookup("#pesajesTable");
            assertEquals("sin dato", texto(escena, "#rindeLabel"));

            agregarPesaje(escena, "10000", "10000");
            assertTrue(mensaje(escena).contains("El peso bruto debe ser mayor que la tara."));
            assertTrue(conError(escena, "#brutoField"));
            assertTrue(tabla.getItems().isEmpty());

            agregarPesaje(escena, "30000", "10000");
            agregarPesaje(escena, "32000", "10000");
            campo(escena, "#hectareasField").setText("20");
            assertEquals("42.000 kg", texto(escena, "#pesoTotalLabel"));
            assertEquals("2.100 kg/ha", texto(escena, "#rindeLabel"));
            var neto = (TableColumn<Pesaje, String>) tabla.getColumns().get(3);
            assertEquals("22.000", neto.getCellData(1));

            tabla.getSelectionModel().select(0);
            boton(escena, "#quitarPesajeButton").fire();
            assertEquals(1, tabla.getItems().size());
            assertEquals(1, tabla.getItems().getFirst().getOrdenLlegada(), "se renumera el orden de llegada");
            assertEquals("22.000 kg", texto(escena, "#pesoTotalLabel"));
            agregarPesaje(escena, "30000", "10000");
            assertEquals(2, tabla.getItems().get(1).getOrdenLlegada());

            boton(escena, "#guardarButton").fire();
            assertTrue(mensaje(escena).contains("La siembra es un dato obligatorio."));
            assertTrue(conError(escena, "#siembraCombo"));
            assertEquals(2, tabla.getItems().size(), "los pesajes cargados se conservan");

            combo(escena, "#siembraCombo").getSelectionModel().selectFirst();
            ((DatePicker) escena.lookup("#fechaInicioPicker")).setValue(LocalDate.of(2026, 3, 20));
            ((DatePicker) escena.lookup("#fechaFinPicker")).setValue(LocalDate.of(2026, 3, 21));
            boton(escena, "#guardarButton").fire();
            return null;
        });
        esperar(() -> mensaje(escena).contains("Cosecha registrada"));
        enFx(() -> {
            assertEquals("Cosecha registrada con el número 100: 2 pesajes, 42.000 kg.", mensaje(escena));
            assertTrue(jdbc.eventos.contains("commit"));
            assertEquals(3, jdbc.inserciones.size());
            assertTrue(((TableView<?>) escena.lookup("#pesajesTable")).getItems().isEmpty());
            assertEquals("0 kg", texto(escena, "#pesoTotalLabel"));
            return null;
        });
    }

    /** P06 y P07 desde la pantalla: cabecera, lotes, número generado, insumo y aplicación con su dosis. */
    @Test
    @SuppressWarnings("unchecked")
    void ordenNuevaSeGuardaYDespuesRecibeInsumosYAplicaciones() throws Exception {
        Scene escena = enFx(() -> cargar("orden.fxml",
                new OrdenTrabajoController(ordenes, sesionIniciada(), () -> fail("No debería volver"), false)));
        esperar(() -> !combo(escena, "#laborCombo").getItems().isEmpty());
        enFx(() -> {
            assertFalse(escena.lookup("#ordenExistenteBox").isVisible());
            assertTrue(escena.lookup("#insumosBox").isDisabled(), "CU-05 espera a que se guarde la orden");

            boton(escena, "#guardarOrdenButton").fire();
            assertTrue(mensaje(escena).contains("La fecha es un dato obligatorio."));
            assertTrue(mensaje(escena).contains("La orden debe tener al menos un lote."));
            assertTrue(conError(escena, "#lotesTable"));
            assertTrue(jdbc.inserciones.isEmpty());

            ((DatePicker) escena.lookup("#fechaPicker")).setValue(LocalDate.of(2025, 11, 20));
            for (String id : new String[]{"#campaniaCombo", "#laborCombo", "#establecimientoCombo", "#contratistaCombo"}) {
                combo(escena, id).getSelectionModel().selectFirst();
            }
            assertEquals(2, combo(escena, "#loteCombo").getItems().size(), "solo los lotes de ESTANCIA CHARRA");
            combo(escena, "#loteCombo").getSelectionModel().selectFirst();
            assertEquals("20", campo(escena, "#superficieField").getText(), "propone las hectáreas del lote");
            boton(escena, "#agregarLoteButton").fire();
            combo(escena, "#loteCombo").getSelectionModel().selectFirst();
            boton(escena, "#agregarLoteButton").fire();
            assertTrue(mensaje(escena).contains("ya está en la orden"));
            assertEquals(1, ((TableView<?>) escena.lookup("#lotesTable")).getItems().size());

            boton(escena, "#guardarOrdenButton").fire();
            return null;
        });
        esperar(() -> texto(escena, "#numeroLabel").equals("25-26-0007"));
        enFx(() -> {
            assertTrue(((GridPane) escena.lookup("#cabeceraGrid")).isDisabled(), "la cabecera guardada no cambia");
            assertFalse(escena.lookup("#insumosBox").isDisabled());

            combo(escena, "#insumoCombo").getSelectionModel().selectFirst();
            campo(escena, "#cantidadField").setText("40");
            ((ComboBox<String>) escena.lookup("#unidadCombo")).getEditor().setText("L");
            boton(escena, "#agregarInsumoButton").fire();

            combo(escena, "#aplicacionLoteCombo").getSelectionModel().selectFirst();
            combo(escena, "#aplicacionInsumoCombo").getSelectionModel().selectFirst();
            campo(escena, "#totalField").setText("0");
            boton(escena, "#agregarAplicacionButton").fire();
            assertTrue(mensaje(escena).contains("El total aplicado debe ser mayor que cero."));
            assertTrue(conError(escena, "#totalField"));

            campo(escena, "#totalField").setText("40");
            boton(escena, "#agregarAplicacionButton").fire();
            var aplicaciones = (TableView<AplicacionInsumo>) escena.lookup("#aplicacionesTable");
            var dosis = (TableColumn<AplicacionInsumo, String>) aplicaciones.getColumns().get(3);
            assertEquals("2 L/ha", dosis.getCellData(0));

            boton(escena, "#guardarAplicacionesButton").fire();
            return null;
        });
        esperar(() -> mensaje(escena).contains("Se guardaron"));
        enFx(() -> {
            assertEquals("Se guardaron 2 registros nuevos en la orden 25-26-0007.", mensaje(escena));
            assertEquals(2, jdbc.eventos.stream().filter("commit"::equals).count());
            return null;
        });
    }

    /** CU-05 desde el menú: abre una orden guardada y muestra sus aplicaciones con la dosis. */
    @Test
    @SuppressWarnings("unchecked")
    void aplicarInsumosAbreUnaOrdenExistente() throws Exception {
        Scene escena = enFx(() -> cargar("orden.fxml",
                new OrdenTrabajoController(ordenes, sesionIniciada(), () -> fail("No debería volver"), true)));
        esperar(() -> !combo(escena, "#ordenCombo").getItems().isEmpty());
        enFx(() -> {
            assertTrue(escena.lookup("#ordenExistenteBox").isVisible());
            assertTrue(boton(escena, "#guardarOrdenButton").isDisabled());
            combo(escena, "#ordenCombo").getSelectionModel().selectFirst();
            boton(escena, "#abrirOrdenButton").fire();
            return null;
        });
        esperar(() -> texto(escena, "#numeroLabel").equals("25-26-0001"));
        enFx(() -> {
            var lotes = (TableView<OrdenLote>) escena.lookup("#lotesTable");
            assertEquals(2, lotes.getItems().size());
            var aplicaciones = (TableView<AplicacionInsumo>) escena.lookup("#aplicacionesTable");
            assertEquals(2, aplicaciones.getItems().size());
            var dosis = (TableColumn<AplicacionInsumo, String>) aplicaciones.getColumns().get(3);
            assertEquals("2 L/ha", dosis.getCellData(1), "30 L en 15 ha");
            assertEquals("ESTANCIA CHARRA", combo(escena, "#establecimientoCombo").getValue().toString());
            assertFalse(escena.lookup("#aplicacionesBox").isDisabled());
            return null;
        });
    }

    /** P08 desde la pantalla: lotes agrupados, ciclos, siembra sin cosecha, rinde y lote sin registrar. */
    @Test
    void trazabilidadMuestraLosLotesConSusCiclos() throws Exception {
        Scene escena = enFx(() -> cargar("trazabilidad.fxml",
                new TrazabilidadController(consultas, sesionIniciada(), () -> fail("No debería volver"))));
        esperar(() -> !combo(escena, "#campaniaCombo").getItems().isEmpty());
        enFx(() -> {
            boton(escena, "#consultarButton").fire();
            assertTrue(mensaje(escena).contains("Elegí la campaña"));
            combo(escena, "#campaniaCombo").getSelectionModel().selectFirst();
            boton(escena, "#consultarButton").fire();
            return null;
        });
        esperar(() -> mensaje(escena).contains("sin registrar."));
        enFx(() -> {
            assertEquals("Campaña 25-26: 2 lotes con operaciones y 1 sin registrar.", mensaje(escena));
            var tabla = (TreeTableView<?>) escena.lookup("#trazabilidadTable");
            var lotes = tabla.getRoot().getChildren();
            assertEquals(3, lotes.size());
            assertEquals("EL BAJO · EL CARMEN", celda(tabla, lotes.get(0), 0));
            assertEquals("Sin registrar", celda(tabla, lotes.get(0), 1));

            var seisNorte = lotes.get(1).getChildren();
            assertEquals(4, seisNorte.size());
            assertEquals("15/10/2025", celda(tabla, seisNorte.get(0), 0));
            assertEquals("Ciclo 1", celda(tabla, seisNorte.get(0), 6));
            assertEquals("25-26-0001 · Contratista de prueba", celda(tabla, seisNorte.get(1), 5));
            assertEquals("Ciclo 2 · sin cosecha registrada", celda(tabla, seisNorte.get(2), 6));
            assertEquals("42.000 kg", celda(tabla, seisNorte.get(3), 3));
            assertEquals("2.100 kg/ha", celda(tabla, seisNorte.get(3), 4));
            assertEquals("Ciclo 1 (siembra del 15/10/2025)", celda(tabla, seisNorte.get(3), 6));
            assertEquals("Trabajo - Labranza", celda(tabla, lotes.get(2).getChildren().getFirst(), 1));
            return null;
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String celda(TreeTableView<?> tabla, TreeItem<?> fila, int columna) {
        return String.valueOf(((javafx.scene.control.TreeTableColumn) tabla.getColumns().get(columna))
                .getCellData((TreeItem) fila));
    }

    private static SesionUsuario sesionIniciada() throws Exception {
        var sesion = new SesionUsuario();
        sesion.iniciar(Ejemplos.usuario());
        return sesion;
    }

    private static Scene cargar(String recurso, Object controlador) throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource(recurso));
        loader.setController(controlador);
        Scene escena = new Scene(loader.load(), 850, 570);
        preparar(escena);
        return escena;
    }

    /** Crea los skins: el contenido de un ScrollPane solo se puede buscar después. */
    private static void preparar(Scene escena) {
        escena.getRoot().applyCss();
        escena.getRoot().layout();
    }

    private static void agregarPesaje(Scene escena, String bruto, String tara) {
        campo(escena, "#brutoField").setText(bruto);
        campo(escena, "#taraField").setText(tara);
        boton(escena, "#agregarPesajeButton").fire();
    }

    @SuppressWarnings("unchecked")
    private static <T> ComboBox<T> combo(Scene escena, String id) {
        return (ComboBox<T>) escena.lookup(id);
    }

    private static TextField campo(Scene escena, String id) {
        return (TextField) escena.lookup(id);
    }

    private static Button boton(Scene escena, String id) {
        return (Button) escena.lookup(id);
    }

    private static String texto(Scene escena, String id) {
        return ((Label) escena.lookup(id)).getText();
    }

    private static String mensaje(Scene escena) {
        return texto(escena, "#mensajeLabel");
    }

    private static boolean conError(Scene escena, String id) {
        return escena.lookup(id).getStyleClass().contains("campo-error");
    }

    private void completarLogin(Scene scene, String usuario, String clave) {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        ((TextField) scene.lookup("#usuarioField")).setText(usuario);
        ((PasswordField) scene.lookup("#passwordField")).setText(clave);
        ((Button) scene.lookup("#ingresarButton")).fire();
    }

    /** Espera a que termine una tarea en segundo plano, consultando la pantalla desde el hilo de JavaFX. */
    private static void esperar(Callable<Boolean> condicion) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!enFx(condicion)) {
            if (System.nanoTime() > limite) {
                throw new AssertionError("La pantalla no llegó al estado esperado.");
            }
            Thread.sleep(25);
        }
    }

    private static <T> T enFx(Callable<T> accion) throws Exception {
        FutureTask<T> tarea = new FutureTask<>(accion);
        Platform.runLater(tarea);
        return tarea.get(15, TimeUnit.SECONDS);
    }
}
