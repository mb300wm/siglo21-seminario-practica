package com.comp.spiapp.controller;

import java.math.BigDecimal;
import java.util.List;

import com.comp.spiapp.model.Campania;
import com.comp.spiapp.model.Establecimiento;
import com.comp.spiapp.model.Insumo;
import com.comp.spiapp.model.Lote;
import com.comp.spiapp.model.Siembra;
import com.comp.spiapp.model.ValidacionException;
import com.comp.spiapp.repository.Catalogos;
import com.comp.spiapp.service.SesionUsuario;
import com.comp.spiapp.service.SiembraService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * CU-02: pantalla de registro de siembra. 
 * Un error nunca borra lo que ya se cargó
 */
public final class SiembraController {
    private final SiembraService servicio;
    private final SesionUsuario sesion;
    private final Runnable alVolver;
    private final Formulario formulario = new Formulario();
    private List<Lote> lotes = List.of();

    @FXML private ComboBox<Campania> campaniaCombo;
    @FXML private ComboBox<Establecimiento> establecimientoCombo;
    @FXML private ComboBox<Lote> loteCombo;
    @FXML private ComboBox<Insumo> semillaCombo;
    @FXML private Label cultivoLabel;
    @FXML private DatePicker fechaInicioPicker;
    @FXML private DatePicker fechaFinPicker;
    @FXML private TextField semillaKgField;
    @FXML private TextField hectareasField;
    @FXML private TextField tractorField;
    @FXML private TextField marchaTrabajoField;
    @FXML private TextField velocidadField;
    @FXML private TextField sembradoraField;
    @FXML private TextField engranajeConductorField;
    @FXML private TextField engranajeConducidoField;
    @FXML private TextField placaField;
    @FXML private TextField granoPorMetroField;
    @FXML private TextField calibreField;
    @FXML private TextField desvioField;
    @FXML private TextField fertilizanteField;
    @FXML private TextField fertilizanteKgHaField;
    @FXML private TextField marchaCajaField;
    @FXML private TextArea comentariosArea;
    @FXML private Button guardarButton;
    @FXML private Label mensajeLabel;

    public SiembraController(SiembraService servicio, SesionUsuario sesion, Runnable alVolver) {
        this.servicio = servicio;
        this.sesion = sesion;
        this.alVolver = alVolver;
    }

    @FXML
    private void initialize() {
        sesion.requerirUsuario();
        formulario.campo("La campaña", campaniaCombo)
                .campo("El lote", loteCombo)
                .campo("La semilla total", semillaKgField)
                .campo("La semilla", semillaCombo)
                .campo("El insumo seleccionado", semillaCombo)
                .campo("La fecha de inicio", fechaInicioPicker)
                .campo("La fecha de fin", fechaFinPicker)
                .campo("Las hectáreas del monitor", hectareasField)
                .campo("La marcha de trabajo", marchaTrabajoField)
                .campo("La velocidad de siembra", velocidadField)
                .campo("El engranaje conductor", engranajeConductorField)
                .campo("El engranaje conducido", engranajeConducidoField)
                .campo("Los granos por metro", granoPorMetroField)
                .campo("El calibre", calibreField)
                .campo("El desvío", desvioField)
                .campo("El fertilizante (kg/ha)", fertilizanteKgHaField)
                .campo("La marcha de la caja de fertilizante", marchaCajaField);

        establecimientoCombo.valueProperty().addListener(
                (obs, anterior, actual) ->
                        filtrarLotes(actual));
        semillaCombo.valueProperty().addListener(
                (obs, anterior, actual) -> cultivoLabel.setText(
                actual == null ? "Según la semilla elegida" : actual.getGrupo().getCultivo().getNombre()));

        guardarButton.setDisable(true);
        Formulario.informar(mensajeLabel, List.of("Cargando campañas, lotes y semillas..."), true);
        Tareas.ejecutar("sigtoa-catalogos-siembra", servicio::cargarCatalogos, this::mostrarCatalogos,
                error -> Formulario.informar(mensajeLabel, Formulario.mensajesDe(error), false));
    }

    private void mostrarCatalogos(Catalogos catalogos) {
        campaniaCombo.setItems(FXCollections.observableArrayList(catalogos.campanias()));
        establecimientoCombo.setItems(FXCollections.observableArrayList(catalogos.establecimientos()));
        semillaCombo.setItems(FXCollections.observableArrayList(catalogos.semillas()));
        lotes = catalogos.lotes();
        mensajeLabel.setText("");
        guardarButton.setDisable(false);
    }

    private void filtrarLotes(Establecimiento establecimiento) {
        loteCombo.setItems(FXCollections.observableArrayList(lotes.stream()
            .filter(lote -> lote.getEstablecimiento().equals(establecimiento))
            .toList()));
    }

    @FXML
    private void guardar() {
        formulario.limpiar();
        BigDecimal semillaKg = formulario.decimal(semillaKgField, "La semilla total (kg)");
        BigDecimal hectareas = formulario.decimal(hectareasField, "Las hectáreas del monitor");
        Integer marchaTrabajo = formulario.entero(marchaTrabajoField, "La marcha de trabajo");
        BigDecimal velocidad = formulario.decimal(velocidadField, "La velocidad de siembra");
        Integer conductor = formulario.entero(engranajeConductorField, "El engranaje conductor");
        Integer conducido = formulario.entero(engranajeConducidoField, "El engranaje conducido");
        BigDecimal granoPorMetro = formulario.decimal(granoPorMetroField, "Los granos por metro");
        BigDecimal calibre = formulario.decimal(calibreField, "El calibre");
        BigDecimal desvio = formulario.decimal(desvioField, "El desvío");
        BigDecimal fertilizanteKgHa = formulario.decimal(fertilizanteKgHaField, "El fertilizante (kg/ha)");
        Integer marchaCaja = formulario.entero(marchaCajaField, "La marcha de la caja de fertilizante");
        if (formulario.hayErrores()) {
            mostrarErrores(formulario.errores());
            return;
        }

        Siembra siembra;
        try {
            siembra = new Siembra(
                null, 
                campaniaCombo.getValue(), 
                loteCombo.getValue(), 
                semillaCombo.getValue(),
                fechaInicioPicker.getValue(), 
                fechaFinPicker.getValue(), 
                semillaKg, 
                hectareas,
                tractorField.getText(), 
                sembradoraField.getText(), 
                marchaTrabajo, 
                velocidad,
                conductor, 
                conducido, 
                placaField.getText(), 
                granoPorMetro, 
                calibre, 
                desvio,
                fertilizanteField.getText(), 
                fertilizanteKgHa, 
                marchaCaja, 
                comentariosArea.getText(),
                sesion.requerirUsuario()
            );
        } catch (ValidacionException ex) {
            mostrarErrores(ex.getErrores());
            return;
        }

        guardarButton.setDisable(true);
        Formulario.informar(mensajeLabel, List.of("Guardando la siembra..."), true);
        Tareas.ejecutar("sigtoa-guardar-siembra", () -> servicio.registrar(siembra), id -> {
            guardarButton.setDisable(false);
            limpiarParaOtraSiembra();
            Formulario.informar(mensajeLabel, List.of("Siembra registrada con el número " + id + "."), true);
        }, error -> {
            guardarButton.setDisable(false);
            mostrarErrores(Formulario.mensajesDe(error));
        });
    }

    private void mostrarErrores(List<String> errores) {
        formulario.marcar(errores);
        Formulario.informar(mensajeLabel, errores, false);
    }

    private void limpiarParaOtraSiembra() {
        loteCombo.setValue(null);
        semillaCombo.setValue(null);
        fechaInicioPicker.setValue(null);
        fechaFinPicker.setValue(null);
        for (TextField campo : List.of(
            semillaKgField, 
            hectareasField, 
            tractorField, 
            marchaTrabajoField,
            velocidadField, 
            sembradoraField, 
            engranajeConductorField, 
            engranajeConducidoField, 
            placaField,
            granoPorMetroField, 
            calibreField, 
            desvioField, 
            fertilizanteField, 
            fertilizanteKgHaField,
            marchaCajaField)
        ) {
            campo.clear();
        }
        comentariosArea.clear();
    }

    @FXML
    private void volver() {
        alVolver.run();
    }
}
