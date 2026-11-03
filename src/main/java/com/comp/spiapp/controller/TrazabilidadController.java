package com.comp.spiapp.controller;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.prefs.Preferences;

import com.comp.spiapp.model.Campania;
import com.comp.spiapp.repository.EtapaTrazabilidad;
import com.comp.spiapp.service.ConsultaService;
import com.comp.spiapp.service.EtapaLote;
import com.comp.spiapp.service.SesionUsuario;
import com.comp.spiapp.service.TrazabilidadCampania;
import com.comp.spiapp.service.TrazabilidadLote;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.stage.FileChooser;

/**
 * CU-07: el actor elige una campaña y ve, agrupada por lote y en orden cronológico,
 * cada siembra, trabajo y cosecha. Los lotes sin operaciones figuran como "sin registrar".
 */
public final class TrazabilidadController {
    private static final String SIN_REGISTRAR = "Sin registrar";
    /** Carpeta del último CSV guardado o abierto; Java la recuerda entre ejecuciones. */
    private static final Preferences PREFERENCIAS = Preferences.userNodeForPackage(TrazabilidadController.class);
    private static final String ULTIMA_CARPETA = "ultimaCarpetaCsv";

    private final ConsultaService servicio;
    private final SesionUsuario sesion;
    private final Runnable alVolver;
    private List<String[]> filas = List.of();
    private String campaniaMostrada;

    @FXML private ComboBox<Campania> campaniaCombo;
    @FXML private Button consultarButton;
    @FXML private Button exportarButton;
    @FXML private Button limpiarButton;
    @FXML private TreeTableView<Fila> trazabilidadTable;
    @FXML private TreeTableColumn<Fila, String> loteFechaColumn;
    @FXML private TreeTableColumn<Fila, String> etapaColumn;
    @FXML private TreeTableColumn<Fila, String> detalleColumn;
    @FXML private TreeTableColumn<Fila, String> cantidadColumn;
    @FXML private TreeTableColumn<Fila, String> rindeColumn;
    @FXML private TreeTableColumn<Fila, String> ordenColumn;
    @FXML private TreeTableColumn<Fila, String> cicloColumn;
    @FXML private Label mensajeLabel;

    /** Una fila de la tabla: el lote (nodo) o una de sus etapas (hoja), ya en texto. */
    record Fila(String loteFecha, String etapa, String detalle, String cantidad, String rinde, String orden,
                String ciclo) {
    }

    public TrazabilidadController(ConsultaService servicio, SesionUsuario sesion, Runnable alVolver) {
        this.servicio = servicio;
        this.sesion = sesion;
        this.alVolver = alVolver;
    }

    @FXML
    private void initialize() {
        sesion.requerirUsuario();
        columna(loteFechaColumn, Fila::loteFecha);
        columna(etapaColumn, Fila::etapa);
        columna(detalleColumn, Fila::detalle);
        columna(cantidadColumn, Fila::cantidad);
        columna(rindeColumn, Fila::rinde);
        columna(ordenColumn, Fila::orden);
        columna(cicloColumn, Fila::ciclo);
        trazabilidadTable.setShowRoot(false);

        consultarButton.setDisable(true);
        Formulario.informar(mensajeLabel, List.of("Cargando campañas..."), true);
        Tareas.ejecutar("sigtoa-campanias", servicio::campanias, campanias -> {
            campaniaCombo.setItems(FXCollections.observableArrayList(campanias));
            consultarButton.setDisable(false);
            mensajeLabel.setText(campanias.isEmpty() ? "No hay campañas registradas." : "");
        }, error -> Formulario.informar(mensajeLabel, Formulario.mensajesDe(error), false));
    }

    @FXML
    private void consultar() {
        Campania campania = campaniaCombo.getValue();
        if (campania == null) {
            Formulario.informar(mensajeLabel, List.of("Elegí la campaña que querés consultar."), false);
            return;
        }
        consultarButton.setDisable(true);
        Formulario.informar(mensajeLabel, List.of("Consultando la campaña " + campania.getNombre() + "..."), true);
        Tareas.ejecutar("sigtoa-trazabilidad", () -> servicio.trazabilidad(campania), resultado -> {
            consultarButton.setDisable(false);
            mostrar(resultado);
        }, error -> {
            consultarButton.setDisable(false);
            Formulario.informar(mensajeLabel, Formulario.mensajesDe(error), false);
        });
    }

    private void mostrar(TrazabilidadCampania resultado) {
        campaniaMostrada = resultado.campania().getNombre();
        if (resultado.sinOperaciones()) {
            filas = List.of();
            trazabilidadTable.setRoot(new TreeItem<>());
            exportarButton.setDisable(true);
            limpiarButton.setDisable(false);
            Formulario.informar(mensajeLabel, List.of("La campaña " + campaniaMostrada
                    + " no tiene operaciones registradas."), false);
            return;
        }
        filas = filas(resultado);
        trazabilidadTable.setRoot(arbol(filas));
        exportarButton.setDisable(false);
        limpiarButton.setDisable(false);
        int conOperaciones = resultado.lotes().size() - (int) resultado.lotesSinRegistrar();
        int[] etapas = resultado.etapasPorTipo();
        Formulario.informar(mensajeLabel, List.of("Campaña " + campaniaMostrada + ": "
                + conOperaciones + " lotes con operaciones y " + resultado.lotesSinRegistrar()
                + " sin registrar. Siembras: " + etapas[0] + ", trabajos: " + etapas[1]
                + ", cosechas: " + etapas[2] + "."), true);
    }

    private String limpiarNombre(String t) {
        StringBuilder res = new StringBuilder();

        for (char c : t.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                res.append(c);
            } else {
                res.append('_');
            }
        }

        return res.toString();
    }

    @FXML
    private void exportar() {
        FileChooser selector = selectorCsv("Exportar trazabilidad");
        String nombre = limpiarNombre(campaniaMostrada);
        selector.setInitialFileName("trazabilidad-" + nombre + ".csv");
        File destino = selector.showSaveDialog(trazabilidadTable.getScene().getWindow());
        if (destino != null) {
            recordarCarpeta(destino);
            exportarA(destino.toPath());
        }
    }

    /** Guarda la consulta en pantalla en el archivo elegido. */
    public void exportarA(Path destino) {
        List<String[]> reporte = filas;
        Tareas.ejecutar("sigtoa-exportar", () -> {
            servicio.exportar(destino, reporte);
            return reporte.size();
        }, cantidad -> Formulario.informar(mensajeLabel,
                List.of("Reporte guardado en " + destino.getFileName() + ": " + cantidad + " filas."), true),
                error -> Formulario.informar(mensajeLabel, Formulario.mensajesDe(error), false));
    }

    @FXML
    private void abrir() {
        File origen = selectorCsv("Abrir reporte de trazabilidad")
                .showOpenDialog(trazabilidadTable.getScene().getWindow());
        if (origen != null) {
            recordarCarpeta(origen);
            abrirDesde(origen.toPath());
        }
    }

    /** Muestra un reporte guardado. Funciona sin conexión a MySQL. */
    public void abrirDesde(Path origen) {
        Tareas.ejecutar("sigtoa-abrir", () -> servicio.importar(origen), reporte -> {
            filas = reporte;
            trazabilidadTable.setRoot(arbol(reporte));
            exportarButton.setDisable(true);
            limpiarButton.setDisable(false);
            Formulario.informar(mensajeLabel,
                    List.of("Reporte abierto desde " + origen.getFileName() + ": " + reporte.size() + " filas."), true);
        }, error -> Formulario.informar(mensajeLabel, Formulario.mensajesDe(error), false));
    }

    private static FileChooser selectorCsv(String titulo) {
        FileChooser selector = new FileChooser();
        selector.setTitle(titulo);
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));
        File carpeta = new File(PREFERENCIAS.get(ULTIMA_CARPETA, System.getProperty("user.home")));
        if (carpeta.isDirectory()) {
            selector.setInitialDirectory(carpeta);
        }
        return selector;
    }

    private static void recordarCarpeta(File archivo) {
        if (archivo.getParent() != null) {
            PREFERENCIAS.put(ULTIMA_CARPETA, archivo.getParent());
        }
    }

    /** Pasa el resultado a filas de texto: es lo mismo que se ve en pantalla y que se guarda en el CSV. */
    private static List<String[]> filas(TrazabilidadCampania resultado) {
        List<String[]> filas = new ArrayList<>();
        for (TrazabilidadLote lote : resultado.lotes()) {
            String nombre = lote.lote() + " - " + lote.establecimiento();
            if (lote.sinRegistrar()) {
                filas.add(new String[]{nombre, "", SIN_REGISTRAR, "", "", "", "", ""});
            }
            for (EtapaLote etapa : lote.etapas()) {
                filas.add(fila(nombre, etapa));
            }
        }
        return filas;
    }

    private static String[] fila(String lote, EtapaLote etapaLote) {
        EtapaTrazabilidad datos = etapaLote.datos();
        String cantidad = datos.cantidad() == null ? ""
                : Formato.numero(datos.cantidad(), 3) + (datos.unidad() == null ? "" : " " + datos.unidad());
        String rinde = !datos.esCosecha() ? "" : etapaLote.rinde() == null ? "sin dato" : kgHa(etapaLote.rinde());
        String orden = datos.numeroOrden() == null ? "" : datos.numeroOrden() + " - " + datos.contratista();
        return new String[]{lote, Formato.fecha(datos.fecha()), datos.etapa(), datos.detalle(), cantidad, rinde,
                orden, ciclo(etapaLote)};
    }

    /** Agrupa por lote: un nodo por cada lote y una hoja por cada etapa. */
    private static TreeItem<Fila> arbol(List<String[]> filas) {
        TreeItem<Fila> raiz = new TreeItem<>();
        TreeItem<Fila> nodo = null;
        String loteActual = null;
        for (String[] fila : filas) {
            boolean sinRegistrar = SIN_REGISTRAR.equals(fila[2]);
            if (!fila[0].equals(loteActual)) {
                loteActual = fila[0];
                nodo = new TreeItem<>(new Fila(loteActual, sinRegistrar ? SIN_REGISTRAR : "", "", "", "", "", ""));
                nodo.setExpanded(true);
                raiz.getChildren().add(nodo);
            }
            if (!sinRegistrar) {
                nodo.getChildren().add(new TreeItem<>(
                        new Fila(fila[1], fila[2], fila[3], fila[4], fila[5], fila[6], fila[7])));
            }
        }
        return raiz;
    }

    private static String ciclo(EtapaLote etapa) {
        if (etapa.ciclo() == null) {
            return "";
        }
        String ciclo = "Ciclo " + etapa.ciclo();
        if (etapa.sinCosecha()) {
            return ciclo + " - sin cosecha registrada";
        }
        if (etapa.siembraDelCiclo() != null) {
            return ciclo + " (siembra del " + Formato.fecha(etapa.siembraDelCiclo()) + ")";
        }
        return ciclo;
    }

    private static String kgHa(BigDecimal rinde) {
        return Formato.numero(rinde) + " kg/ha";
    }

    /** Deja la pantalla como al entrar, sin volver al menú. */
    @FXML
    private void limpiar() {
        filas = List.of();
        campaniaMostrada = null;
        campaniaCombo.setValue(null);
        trazabilidadTable.setRoot(null);
        exportarButton.setDisable(true);
        limpiarButton.setDisable(true);
        mensajeLabel.setText("");
    }

    @FXML
    private void volver() {
        alVolver.run();
    }

    private static void columna(TreeTableColumn<Fila, String> columna, Function<Fila, String> valor) {
        columna.setCellValueFactory(celda -> new SimpleStringProperty(
                celda.getValue().getValue() == null ? "" : valor.apply(celda.getValue().getValue())));
    }
}
