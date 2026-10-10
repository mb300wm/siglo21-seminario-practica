package com.comp.spiapp.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.comp.spiapp.model.Acopio;
import com.comp.spiapp.model.Cosecha;
import com.comp.spiapp.model.Pesaje;
import com.comp.spiapp.model.Siembra;
import com.comp.spiapp.model.ValidacionException;
import com.comp.spiapp.service.CosechaService;
import com.comp.spiapp.service.OpcionesCosecha;
import com.comp.spiapp.service.SesionUsuario;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * CU-03: transcribe la hoja de cosecha. Los pesajes se agregan en el orden de llegada
 * de los camiones y el neto, el total y el rinde se recalculan en cada cambio
 */
public final class CosechaController {
    private final CosechaService servicio;
    private final SesionUsuario sesion;
    private final Runnable alVolver;
    private final Formulario formulario = new Formulario();
    private final Formulario formularioPesaje = new Formulario();
    private final ObservableList<Pesaje> pesajes = FXCollections.observableArrayList();

    @FXML private ComboBox<Siembra> siembraCombo;
    @FXML private DatePicker fechaInicioPicker;
    @FXML private DatePicker fechaFinPicker;
    @FXML private TextField hectareasField;
    @FXML private TextField humedadField;
    @FXML private ComboBox<Acopio> acopioCombo;
    @FXML private TextField brutoField;
    @FXML private TextField taraField;
    @FXML private TableView<Pesaje> pesajesTable;
    @FXML private TableColumn<Pesaje, String> ordenColumn;
    @FXML private TableColumn<Pesaje, String> brutoColumn;
    @FXML private TableColumn<Pesaje, String> taraColumn;
    @FXML private TableColumn<Pesaje, String> netoColumn;
    @FXML private Label pesoTotalLabel;
    @FXML private Label rindeLabel;
    @FXML private TextArea comentariosArea;
    @FXML private Button guardarButton;
    @FXML private Label mensajeLabel;

    public CosechaController(CosechaService servicio, SesionUsuario sesion, Runnable alVolver) {
        this.servicio = servicio;
        this.sesion = sesion;
        this.alVolver = alVolver;
    }

    @FXML
    private void initialize() {
        sesion.requerirUsuario();
        formulario.campo("La siembra", siembraCombo)
            .campo("La fecha de inicio", fechaInicioPicker)
            .campo("La fecha de fin", fechaFinPicker)
            .campo("Las hectareas", hectareasField)
            .campo("La humedad", humedadField)
            .campo("Debe agregarse al menos un pesaje", pesajesTable);
        formularioPesaje.campo("El peso bruto", brutoField)
            .campo("La tara", taraField);

        siembraCombo.setCellFactory(lista -> new CeldaSiembra());
        siembraCombo.setButtonCell(new CeldaSiembra());

        ordenColumn.setCellValueFactory(fila ->
                texto(String.valueOf(fila.getValue().getOrdenLlegada())));

        brutoColumn.setCellValueFactory(fila ->
                texto(Formato.numero(fila.getValue().getPesoBruto())));

        taraColumn.setCellValueFactory(fila ->
                texto(Formato.numero(fila.getValue().getPesoTara())));

        netoColumn.setCellValueFactory(fila ->
                texto(Formato.numero(fila.getValue().calcularNeto())));

        pesajesTable.setItems(pesajes);

        hectareasField.textProperty().addListener((obs, anterior, actual) -> actualizarTotales());
        actualizarTotales();

        guardarButton.setDisable(true);

        Formulario.informar(mensajeLabel, List.of("Cargando siembras registradas..."), true);

        Tareas.ejecutar("sigtoa-opciones-cosecha", 
            servicio::cargarOpciones, 
            this::mostrarOpciones, 
            error -> Formulario.informar(mensajeLabel, 
                Formulario.mensajesDe(error), false));
    }

    private void mostrarOpciones(OpcionesCosecha opciones) {
        siembraCombo.setItems(FXCollections.observableArrayList(opciones.siembras()));
        acopioCombo.setItems(FXCollections.observableArrayList(opciones.acopios()));
        guardarButton.setDisable(false);
        if (opciones.siembras().isEmpty()) {
            Formulario.informar(mensajeLabel, List.of("No hay siembras registradas. Registrá primero la siembra."), false);
        } else {
            mensajeLabel.setText("");
        }
    }

    /**
     * A2: un pesaje invalido no se agrega ni se calcula su neto
     */
    @FXML
    private void agregarPesaje() {
        formularioPesaje.limpiar();
        BigDecimal bruto = formularioPesaje.decimal(brutoField, "El peso bruto");
        BigDecimal tara = formularioPesaje.decimal(taraField, "La tara");
        if (formularioPesaje.hayErrores()) {
            mostrarErrores(formularioPesaje, formularioPesaje.errores());
            return;
        }
        try {
            pesajes.add(new Pesaje(null, pesajes.size() + 1, bruto, tara));
        } catch (ValidacionException ex) {
            mostrarErrores(formularioPesaje, ex.getErrores());
            return;
        }
        brutoField.clear();
        taraField.clear();
        brutoField.requestFocus();
        mensajeLabel.setText("");
        actualizarTotales();
    }

    @FXML
    private void quitarPesaje() {
        int seleccionado = pesajesTable.getSelectionModel().getSelectedIndex();
        if (seleccionado < 0) {
            Formulario.informar(mensajeLabel, List.of("Seleccioná el pesaje que querés quitar."), false);
            return;
        }
        List<Pesaje> restantes = new ArrayList<>(pesajes);
        restantes.remove(seleccionado);
        List<Pesaje> renumerados = new ArrayList<>();
        try {
            for (int i = 0; i < restantes.size(); i++) {
                Pesaje pesaje = restantes.get(i);
                renumerados.add(new Pesaje(null, i + 1, pesaje.getPesoBruto(), pesaje.getPesoTara()));
            }
        } catch (ValidacionException ex) {
            throw new IllegalStateException("Un pesaje ya validado dejó de ser válido.", ex);
        }
        pesajes.setAll(renumerados);
        actualizarTotales();
    }

    private void actualizarTotales() {
        BigDecimal total = BigDecimal.ZERO;
        for (Pesaje pesaje : pesajes) {
            total = total.add(pesaje.calcularNeto());
        }
        pesoTotalLabel.setText(Formato.numero(total) + " kg");

        BigDecimal hectareas;
        try {
            hectareas = Formulario.leerDecimal(hectareasField.getText());
        } catch (NumberFormatException ex) {
            hectareas = null;
        }
        if (pesajes.isEmpty() || hectareas == null || hectareas.signum() <= 0) {
            rindeLabel.setText("sin dato");
        } else {
            rindeLabel.setText(Formato.numero(total.divide(hectareas, 1, RoundingMode.HALF_UP)) + " kg/ha");
        }
    }

    @FXML
    private void guardar() {
        formulario.limpiar();
        formularioPesaje.limpiar();
        BigDecimal hectareas = formulario.decimal(hectareasField, "Las hectareas");
        BigDecimal humedad = formulario.decimal(humedadField, "La humedad");
        if (formulario.hayErrores()) {
            mostrarErrores(formulario, formulario.errores());
            return;
        }

        Cosecha cosecha;
        try {
            cosecha = new Cosecha(null, siembraCombo.getValue(), fechaInicioPicker.getValue(),
                    fechaFinPicker.getValue(), hectareas, humedad, acopioCombo.getValue(),
                    comentariosArea.getText(), sesion.requerirUsuario());
            for (Pesaje pesaje : pesajes) {
                cosecha.agregarPesaje(pesaje);
            }
            cosecha.validar();
        } catch (ValidacionException ex) {
            mostrarErrores(formulario, ex.getErrores());
            return;
        }

        guardarButton.setDisable(true);
        Formulario.informar(mensajeLabel, List.of("Guardando la cosecha y sus pesajes…"), true);
        Tareas.ejecutar("sigtoa-guardar-cosecha", () -> servicio.registrar(cosecha), id -> {
            guardarButton.setDisable(false);
            String resumen = "Cosecha registrada con el número " + id + ": " + cosecha.getPesajes().size()
                    + " pesajes, " + Formato.numero(cosecha.calcularPesoTotal()) + " kg.";
            limpiar();
            Formulario.informar(mensajeLabel, List.of(resumen), true);
        }, error -> {
            guardarButton.setDisable(false);
            mostrarErrores(formulario, Formulario.mensajesDe(error));
        });
    }


    private void mostrarErrores(Formulario origen, List<String> errores) {
        origen.marcar(errores);
        Formulario.informar(mensajeLabel, errores, false);
    }

    private void limpiar() {
        siembraCombo.setValue(null);
        fechaInicioPicker.setValue(null);
        fechaFinPicker.setValue(null);
        acopioCombo.setValue(null);
        for (TextField campo : List.of(hectareasField, humedadField, brutoField, taraField)) {
            campo.clear();
        }
        comentariosArea.clear();
        pesajes.clear();
        actualizarTotales();
    }

    @FXML
    private void volver() {
        alVolver.run();
    }
    
    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor);
    }

    private static final class CeldaSiembra extends ListCell<Siembra> {
        @Override
        protected void updateItem(Siembra siembra, boolean vacia) {
            super.updateItem(siembra, vacia);
            setText(vacia || siembra == null ? null : Formato.siembra(siembra));
        }
    }
}
