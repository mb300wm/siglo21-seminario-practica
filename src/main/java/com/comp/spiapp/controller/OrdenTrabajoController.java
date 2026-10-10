package com.comp.spiapp.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.comp.spiapp.model.AplicacionInsumo;
import com.comp.spiapp.model.Campania;
import com.comp.spiapp.model.Contratista;
import com.comp.spiapp.model.Cultivo;
import com.comp.spiapp.model.Deposito;
import com.comp.spiapp.model.Establecimiento;
import com.comp.spiapp.model.Insumo;
import com.comp.spiapp.model.Labor;
import com.comp.spiapp.model.Lote;
import com.comp.spiapp.model.OrdenInsumo;
import com.comp.spiapp.model.OrdenLote;
import com.comp.spiapp.model.OrdenTrabajo;
import com.comp.spiapp.model.Usuario;
import com.comp.spiapp.model.ValidacionException;
import com.comp.spiapp.repository.CatalogosOrden;
import com.comp.spiapp.repository.ResumenOrden;
import com.comp.spiapp.service.OrdenTrabajoService;
import com.comp.spiapp.service.SesionUsuario;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * CU-04 y CU-05 en la misma pantalla, como en el prototipo del TP2: primero se guarda la orden con
 * sus lotes y recién entonces se habilitan los insumos a remitir y las aplicaciones por lote.
 * Desde "Aplicar insumos" se abre una orden ya guardada y se pasa directo a esa segunda parte.
 */
public final class OrdenTrabajoController {
    // Dependencias y estado de la pantalla
    private final OrdenTrabajoService servicio;
    private final SesionUsuario sesion;
    private final Runnable alVolver;
    private final boolean abrirExistente;
    private final Formulario formulario = new Formulario();
    private final Formulario formularioLote = new Formulario();
    private final Formulario formularioInsumo = new Formulario();
    private final Formulario formularioAplicacion = new Formulario();
    private final ObservableList<OrdenLote> lotesPendientes = FXCollections.observableArrayList();
    private List<Lote> lotes = List.of();
    /** Null hasta que la orden se guarda (CU-04) o se abre una existente (CU-05). */
    private OrdenTrabajo orden;

    // Controles, cabecera de la orden
    @FXML private Label tituloLabel;
    @FXML private HBox ordenExistenteBox;
    @FXML private ComboBox<ResumenOrden> ordenCombo;
    @FXML private Label numeroLabel;
    @FXML private GridPane cabeceraGrid;
    @FXML private DatePicker fechaPicker;
    @FXML private ComboBox<Campania> campaniaCombo;
    @FXML private ComboBox<Labor> laborCombo;
    @FXML private ComboBox<Establecimiento> establecimientoCombo;
    @FXML private ComboBox<Contratista> contratistaCombo;
    @FXML private ComboBox<Deposito> depositoCombo;
    @FXML private TextField remitoField;
    @FXML private TextField convenioField;
    @FXML private TextArea comentariosArea;
    // Controles, lotes
    @FXML private HBox loteEntradaBox;
    @FXML private ComboBox<Lote> loteCombo;
    @FXML private ComboBox<Cultivo> cultivoCombo;
    @FXML private TextField superficieField;
    @FXML private TableView<OrdenLote> lotesTable;
    @FXML private TableColumn<OrdenLote, String> loteColumn;
    @FXML private TableColumn<OrdenLote, String> cultivoColumn;
    @FXML private TableColumn<OrdenLote, String> superficieColumn;
    @FXML private Button guardarOrdenButton;
    // Controles, insumos a remitir
    @FXML private VBox insumosBox;
    @FXML private ComboBox<Insumo> insumoCombo;
    @FXML private TextField cantidadField;
    @FXML private ComboBox<String> unidadCombo;
    @FXML private TableView<OrdenInsumo> insumosTable;
    @FXML private TableColumn<OrdenInsumo, String> insumoColumn;
    @FXML private TableColumn<OrdenInsumo, String> grupoColumn;
    @FXML private TableColumn<OrdenInsumo, String> cantidadColumn;
    // Controles, aplicaciones por lote
    @FXML private VBox aplicacionesBox;
    @FXML private ComboBox<OrdenLote> aplicacionLoteCombo;
    @FXML private ComboBox<OrdenInsumo> aplicacionInsumoCombo;
    @FXML private TextField totalField;
    @FXML private TableView<AplicacionInsumo> aplicacionesTable;
    @FXML private TableColumn<AplicacionInsumo, String> aplicacionLoteColumn;
    @FXML private TableColumn<AplicacionInsumo, String> aplicacionInsumoColumn;
    @FXML private TableColumn<AplicacionInsumo, String> totalColumn;
    @FXML private TableColumn<AplicacionInsumo, String> dosisColumn;
    @FXML private Button guardarAplicacionesButton;
    // Mensajes al usuario
    @FXML private Label mensajeLabel;

    public OrdenTrabajoController(OrdenTrabajoService servicio, SesionUsuario sesion, Runnable alVolver,
                                  boolean abrirExistente) {
        this.servicio = servicio;
        this.sesion = sesion;
        this.alVolver = alVolver;
        this.abrirExistente = abrirExistente;
    }

    @FXML
    private void initialize() {
        sesion.requerirUsuario();
        formulario.campo("La fecha", fechaPicker)
                .campo("La labor", laborCombo)
                .campo("El establecimiento", establecimientoCombo)
                .campo("El contratista", contratistaCombo)
                .campo("La campaña", campaniaCombo)
                .campo("La orden debe tener al menos un lote", lotesTable);
        formularioLote.campo("La superficie", superficieField).campo("El lote", loteCombo);
        formularioInsumo.campo("La cantidad a remitir", cantidadField).campo("El insumo", insumoCombo);
        formularioAplicacion.campo("El total aplicado", totalField)
                .campo("El lote", aplicacionLoteCombo)
                .campo("La aplicación necesita una superficie", aplicacionLoteCombo)
                .campo("Ya hay una aplicación", aplicacionLoteCombo)
                .campo("El insumo", aplicacionInsumoCombo);

        configurarListas();
        establecimientoCombo.valueProperty().addListener(
                (obs, anterior, actual) ->
                        cambiarEstablecimiento(anterior, actual));
        loteCombo.valueProperty().addListener((obs, anterior, actual) ->
                superficieField.setText(actual == null ? "" : Formato.editable(actual.getHectareas())));
        unidadCombo.setItems(FXCollections.observableArrayList("L", "kg"));

        insumosBox.setDisable(true);
        aplicacionesBox.setDisable(true);
        guardarOrdenButton.setDisable(true);
        ordenExistenteBox.setVisible(abrirExistente);
        ordenExistenteBox.setManaged(abrirExistente);
        if (abrirExistente) {
            tituloLabel.setText("Aplicar insumos");
            bloquearCabecera();
        }
        Formulario.informar(mensajeLabel, List.of("Cargando datos del formulario..."), true);
        Tareas.ejecutar("sigtoa-catalogos-orden", servicio::cargarCatalogos, this::mostrarCatalogos,
                error -> Formulario.informar(mensajeLabel, Formulario.mensajesDe(error), false));
    }

    private void configurarListas() {
        insumoCombo.setConverter(texto(insumo ->
                insumo.getNombre() + " (" + insumo.getGrupo().getNombre() + ")"));
        aplicacionLoteCombo.setConverter(texto(ordenLote -> ordenLote.getLote().getNombre()));
        aplicacionInsumoCombo.setConverter(texto(ordenInsumo -> ordenInsumo.getInsumo().getNombre()));

        lotesTable.setItems(lotesPendientes);
        columna(loteColumn, ordenLote -> ordenLote.getLote().getNombre());
        columna(cultivoColumn, ordenLote -> ordenLote.getCultivo() == null
                ? "—" : ordenLote.getCultivo().getNombre());
        columna(superficieColumn, ordenLote -> ordenLote.getSuperficieHa() == null
                ? "sin dato" : Formato.numero(ordenLote.getSuperficieHa()) + " ha");

        columna(insumoColumn, ordenInsumo ->
                ordenInsumo.getInsumo().getNombre());
        columna(grupoColumn, ordenInsumo ->
                ordenInsumo.getInsumo().getGrupo().getNombre());
        columna(cantidadColumn, ordenInsumo ->
                ordenInsumo.getCantidadARemitir() == null ? "sin dato"
                : Formato.numero(ordenInsumo.getCantidadARemitir(), 3) + " " + unidad(ordenInsumo));

        columna(aplicacionLoteColumn, aplicacion ->
                aplicacion.getOrdenLote().getLote().getNombre());
        columna(aplicacionInsumoColumn, aplicacion ->
                aplicacion.getOrdenInsumo().getInsumo().getNombre());
        columna(totalColumn, aplicacion ->
                Formato.numero(aplicacion.getTotal(), 3) + " " + unidad(aplicacion.getOrdenInsumo()));
        columna(dosisColumn, OrdenTrabajoController::dosis);
    }

    private void mostrarCatalogos(CatalogosOrden catalogos) {
        campaniaCombo.setItems(FXCollections.observableArrayList(catalogos.campanias()));
        laborCombo.setItems(FXCollections.observableArrayList(catalogos.labores()));
        establecimientoCombo.setItems(FXCollections.observableArrayList(catalogos.establecimientos()));
        contratistaCombo.setItems(FXCollections.observableArrayList(catalogos.contratistas()));
        depositoCombo.setItems(FXCollections.observableArrayList(catalogos.depositos()));
        cultivoCombo.setItems(FXCollections.observableArrayList(catalogos.cultivos()));
        insumoCombo.setItems(FXCollections.observableArrayList(catalogos.insumos()));
        lotes = catalogos.lotes();
        if (!abrirExistente) {
            guardarOrdenButton.setDisable(false);
            mensajeLabel.setText("");
            return;
        }
        Tareas.ejecutar("sigtoa-listar-ordenes", servicio::listarOrdenes, ordenes -> {
            ordenCombo.setItems(FXCollections.observableArrayList(ordenes));
            if (ordenes.isEmpty()) {
                Formulario.informar(mensajeLabel, List.of("No hay órdenes de trabajo registradas."), false);
            } else {
                mensajeLabel.setText("");
            }
        }, error -> Formulario.informar(mensajeLabel, Formulario.mensajesDe(error), false));
    }

    /** Los lotes de la orden tienen que ser del mismo establecimiento (CU-04, paso 2). */
    private void cambiarEstablecimiento(Establecimiento anterior, Establecimiento actual) {
        if (orden != null) {
            return;
        }
        loteCombo.setItems(FXCollections.observableArrayList(lotes.stream()
                .filter(lote -> lote.getEstablecimiento().equals(actual))
                .toList()));
        if (anterior != null && !lotesPendientes.isEmpty()) {
            lotesPendientes.clear();
            Formulario.informar(mensajeLabel,
                    List.of("Se quitaron los lotes cargados porque eran de otro establecimiento."), false);
        }
    }

    @FXML
    private void agregarLote() {
        formularioLote.limpiar();
        BigDecimal superficie = formularioLote.decimal(superficieField, "La superficie");
        Lote lote = loteCombo.getValue();
        List<String> errores = new ArrayList<>(formularioLote.errores());
        // El dominio también lo controla al guardar; acá se avisa apenas se carga.
        if (lote != null && lotesPendientes.stream().anyMatch(ordenLote -> ordenLote.getLote().equals(lote))) {
            errores.add("El lote " + lote.getNombre() + " ya está en la orden.");
        }
        if (!errores.isEmpty()) {
            mostrarErrores(formularioLote, errores);
            return;
        }
        try {
            lotesPendientes.add(new OrdenLote(null, lote, cultivoCombo.getValue(), superficie));
        } catch (ValidacionException ex) {
            mostrarErrores(formularioLote, ex.getErrores());
            return;
        }
        loteCombo.setValue(null);
        cultivoCombo.setValue(null);
        superficieField.clear();
        mensajeLabel.setText("");
    }

    @FXML
    private void quitarLote() {
        OrdenLote seleccionado = lotesTable.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Formulario.informar(mensajeLabel, List.of("Seleccioná el lote que querés quitar."), false);
            return;
        }
        lotesPendientes.remove(seleccionado);
    }

    /** CU-04: valida la cabecera y los lotes, y guarda; el número se genera en el servicio. */
    @FXML
    private void guardarOrden() {
        formulario.limpiar();
        formularioLote.limpiar();
        List<String> errores = new ArrayList<>();
        OrdenTrabajo nueva = null;
        try {
            nueva = new OrdenTrabajo(null, null, fechaPicker.getValue(), laborCombo.getValue(),
                    establecimientoCombo.getValue(), contratistaCombo.getValue(), depositoCombo.getValue(),
                    remitoField.getText(), campaniaCombo.getValue(), convenioField.getText(),
                    comentariosArea.getText(), sesion.requerirUsuario());
            for (OrdenLote ordenLote : lotesPendientes) {
                nueva.agregarLote(ordenLote);
            }
            nueva.validar();
        } catch (ValidacionException ex) {
            errores.addAll(ex.getErrores());
            // Si la cabecera falló, el dominio todavía no revisó los lotes: se avisan juntos (A1).
            if (nueva == null && lotesPendientes.isEmpty()) {
                errores.add("La orden debe tener al menos un lote.");
            }
        }
        if (!errores.isEmpty()) {
            mostrarErrores(formulario, errores);
            return;
        }

        OrdenTrabajo aGuardar = nueva;
        guardarOrdenButton.setDisable(true);
        Formulario.informar(mensajeLabel, List.of("Guardando la orden..."), true);
        Tareas.ejecutar("sigtoa-guardar-orden", () -> servicio.registrar(aGuardar), numero -> {
            mostrarOrden(aGuardar);
            Formulario.informar(mensajeLabel, List.of("Orden " + numero
                    + " guardada. Ya podés cargar los insumos a remitir y las aplicaciones."), true);
        }, error -> {
            guardarOrdenButton.setDisable(false);
            mostrarErrores(formulario, Formulario.mensajesDe(error));
        });
    }

    /** CU-05 desde el menú: abre una orden guardada con todos sus detalles. */
    @FXML
    private void abrirOrden() {
        ResumenOrden elegida = ordenCombo.getValue();
        if (elegida == null) {
            Formulario.informar(mensajeLabel, List.of("Elegí la orden a la que querés aplicar insumos."), false);
            return;
        }
        Formulario.informar(mensajeLabel, List.of("Abriendo la orden " + elegida.numero() + "..."), true);
        Tareas.ejecutar("sigtoa-abrir-orden", () -> servicio.buscar(elegida.id()), abierta -> {
            mostrarOrden(abierta);
            llenarCabecera(abierta);
            mensajeLabel.setText("");
        }, error -> Formulario.informar(mensajeLabel, Formulario.mensajesDe(error), false));
    }

    private void mostrarOrden(OrdenTrabajo guardada) {
        orden = guardada;
        numeroLabel.setText(guardada.getNumero());
        lotesTable.setItems(FXCollections.observableArrayList(guardada.getLotes()));
        bloquearCabecera();
        insumosBox.setDisable(false);
        aplicacionesBox.setDisable(false);
        refrescarDetalle();
    }

    private void llenarCabecera(OrdenTrabajo abierta) {
        fechaPicker.setValue(abierta.getFecha());
        campaniaCombo.setValue(abierta.getCampania());
        laborCombo.setValue(abierta.getLabor());
        establecimientoCombo.setValue(abierta.getEstablecimiento());
        contratistaCombo.setValue(abierta.getContratista());
        depositoCombo.setValue(abierta.getDeposito());
        remitoField.setText(abierta.getRemitoNumero());
        convenioField.setText(abierta.getConvenio());
        comentariosArea.setText(abierta.getComentarios());
    }

    private void bloquearCabecera() {
        cabeceraGrid.setDisable(true);
        loteEntradaBox.setDisable(true);
        guardarOrdenButton.setDisable(true);
    }

    @FXML
    private void agregarInsumo() {
        formularioInsumo.limpiar();
        BigDecimal cantidad = formularioInsumo.decimal(cantidadField, "La cantidad a remitir");
        if (formularioInsumo.hayErrores()) {
            mostrarErrores(formularioInsumo, formularioInsumo.errores());
            return;
        }
        try {
            orden.agregarInsumo(new OrdenInsumo(null, insumoCombo.getValue(), cantidad,
                    unidadCombo.getEditor().getText()));
        } catch (ValidacionException ex) {
            mostrarErrores(formularioInsumo, ex.getErrores());
            return;
        }
        insumoCombo.setValue(null);
        cantidadField.clear();
        mensajeLabel.setText("");
        refrescarDetalle();
    }

    @FXML
    private void quitarInsumo() {
        quitar(insumosTable.getSelectionModel().getSelectedItem(), "el insumo", orden::quitarInsumo);
    }

    /** P07: la dosis se calcula con el total y la superficie, el insumo tiene que ser de esta orden. */
    @FXML
    private void agregarAplicacion() {
        formularioAplicacion.limpiar();
        BigDecimal total = formularioAplicacion.decimal(totalField, "El total aplicado");
        if (formularioAplicacion.hayErrores()) {
            mostrarErrores(formularioAplicacion, formularioAplicacion.errores());
            return;
        }
        try {
            orden.agregarAplicacion(new AplicacionInsumo(null, aplicacionLoteCombo.getValue(),
                    aplicacionInsumoCombo.getValue(), total));
        } catch (ValidacionException ex) {
            mostrarErrores(formularioAplicacion, ex.getErrores());
            return;
        }
        totalField.clear();
        mensajeLabel.setText("");
        refrescarDetalle();
    }

    @FXML
    private void quitarAplicacion() {
        quitar(aplicacionesTable.getSelectionModel().getSelectedItem(), "la aplicación", orden::quitarAplicacion);
    }

    /** CU-05: guarda lo nuevo en una transacción y registra quién modificó la orden (RNF10). */
    @FXML
    private void guardarAplicaciones() {
        Usuario usuario = sesion.requerirUsuario();
        guardarAplicacionesButton.setDisable(true);
        Formulario.informar(mensajeLabel, List.of("Guardando insumos y aplicaciones..."), true);
        Tareas.ejecutar("sigtoa-guardar-aplicaciones", () -> servicio.registrarAplicaciones(orden, usuario),
                guardados -> {
                    guardarAplicacionesButton.setDisable(false);
                    refrescarDetalle();
                    Formulario.informar(mensajeLabel, List.of("Se guardaron " + guardados
                            + " registros nuevos en la orden " + orden.getNumero() + "."), true);
                }, error -> {
                    guardarAplicacionesButton.setDisable(false);
                    Formulario.informar(mensajeLabel, Formulario.mensajesDe(error), false);
                });
    }

    private void refrescarDetalle() {
        insumosTable.setItems(FXCollections.observableArrayList(orden.getInsumos()));
        aplicacionesTable.setItems(FXCollections.observableArrayList(orden.getAplicaciones()));
        aplicacionLoteCombo.setItems(FXCollections.observableArrayList(orden.getLotes()));
        aplicacionInsumoCombo.setItems(FXCollections.observableArrayList(orden.getInsumos()));
    }

    @FunctionalInterface
    private interface Quitar<T> {
        void aplicar(T elemento) throws ValidacionException;
    }

    private <T> void quitar(T seleccionado, String nombre, Quitar<T> accion) {
        if (seleccionado == null) {
            Formulario.informar(mensajeLabel, List.of("Seleccioná " + nombre + " que querés quitar."), false);
            return;
        }
        try {
            accion.aplicar(seleccionado);
        } catch (ValidacionException ex) {
            Formulario.informar(mensajeLabel, ex.getErrores(), false);
            return;
        }
        mensajeLabel.setText("");
        refrescarDetalle();
    }

    private void mostrarErrores(Formulario origen, List<String> errores) {
        origen.marcar(errores);
        Formulario.informar(mensajeLabel, errores, false);
    }

    @FXML
    private void volver() {
        alVolver.run();
    }

    private static String dosis(AplicacionInsumo aplicacion) {
        try {
            return Formato.numero(aplicacion.calcularDosis(), 3) + " " + unidad(aplicacion.getOrdenInsumo()) + "/ha";
        } catch (ValidacionException ex) {
            return "sin dato";
        }
    }

    private static String unidad(OrdenInsumo ordenInsumo) {
        return ordenInsumo.getUnidad() == null ? "" : ordenInsumo.getUnidad();
    }

    private static <T> void columna(TableColumn<T, String> columna, Function<T, String> valor) {
        columna.setCellValueFactory(fila -> new SimpleStringProperty(valor.apply(fila.getValue())));
    }

    private static <T> StringConverter<T> texto(Function<T, String> descripcion) {
        return new StringConverter<>() {
            @Override
            public String toString(T valor) {
                return valor == null ? "" : descripcion.apply(valor);
            }

            @Override
            public T fromString(String texto) {
                throw new UnsupportedOperationException("Lista de solo selección.");
            }
        };
    }
}
