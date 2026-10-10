package com.comp.spiapp.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

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

/**
 * CU-07: el actor elige una campaña y ve, agrupada por lote y en orden cronológico,
 * cada siembra, trabajo y cosecha. Los lotes sin operaciones figuran como "sin registrar".
 */
public final class TrazabilidadController {
    private final ConsultaService servicio;
    private final SesionUsuario sesion;
    private final Runnable alVolver;

    @FXML private ComboBox<Campania> campaniaCombo;
    @FXML private Button consultarButton;
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
        TreeItem<Fila> raiz = new TreeItem<>();
        if (resultado.sinOperaciones()) {
            trazabilidadTable.setRoot(raiz);
            Formulario.informar(mensajeLabel, List.of("La campaña " + resultado.campania().getNombre()
                    + " no tiene operaciones registradas."), false);
            return;
        }
        for (TrazabilidadLote lote : resultado.lotes()) {
            TreeItem<Fila> nodo = new TreeItem<>(new Fila(lote.lote() + " · " + lote.establecimiento(),
                    lote.sinRegistrar() ? "Sin registrar" : "", "", "", "", "", ""));
            for (EtapaLote etapa : lote.etapas()) {
                nodo.getChildren().add(new TreeItem<>(fila(etapa)));
            }
            nodo.setExpanded(true);
            raiz.getChildren().add(nodo);
        }
        trazabilidadTable.setRoot(raiz);
        int conOperaciones = resultado.lotes().size() - (int) resultado.lotesSinRegistrar();
        Formulario.informar(mensajeLabel, List.of("Campaña " + resultado.campania().getNombre() + ": "
                + conOperaciones + " lotes con operaciones y " + resultado.lotesSinRegistrar()
                + " sin registrar."), true);
    }

    private static Fila fila(EtapaLote etapaLote) {
        EtapaTrazabilidad datos = etapaLote.datos();
        String cantidad = datos.cantidad() == null ? ""
                : Formato.numero(datos.cantidad(), 3) + (datos.unidad() == null ? "" : " " + datos.unidad());
        String rinde = !datos.esCosecha() ? "" : etapaLote.rinde() == null ? "sin dato" : kgHa(etapaLote.rinde());
        String orden = datos.numeroOrden() == null ? "" : datos.numeroOrden() + " · " + datos.contratista();
        return new Fila(Formato.fecha(datos.fecha()), datos.etapa(), datos.detalle(), cantidad, rinde, orden,
                ciclo(etapaLote));
    }

    private static String ciclo(EtapaLote etapa) {
        if (etapa.ciclo() == null) {
            return "";
        }
        String ciclo = "Ciclo " + etapa.ciclo();
        if (etapa.sinCosecha()) {
            return ciclo + " · sin cosecha registrada";
        }
        if (etapa.siembraDelCiclo() != null) {
            return ciclo + " (siembra del " + Formato.fecha(etapa.siembraDelCiclo()) + ")";
        }
        return ciclo;
    }

    private static String kgHa(BigDecimal rinde) {
        return Formato.numero(rinde) + " kg/ha";
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
