package com.comp.spiapp.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OrdenTrabajo
 */
public final class OrdenTrabajo extends OperacionAuditada {
    private static final Pattern FORMATO_NUMERO = Pattern.compile("^[0-9]{2}-[0-9]{2}-[0-9]{4}$");

    private String numero;
    private final LocalDate fecha;
    private final Labor labor;
    private final Establecimiento establecimiento;
    private final Contratista contratista;
    private final Deposito deposito;        // opcional
    private final String remitoNumero;      // opcional
    private final Campania campania;
    private final String convenio;          // opcional
    private final String comentarios;       // opcional
    private final List<OrdenLote> lotes = new ArrayList<>();
    private final List<OrdenInsumo> insumos = new ArrayList<>();
    private final List<AplicacionInsumo> aplicaciones = new ArrayList<>();
    
    public OrdenTrabajo(
        Integer id, 
        String numero, 
        LocalDate fecha, 
        Labor labor,
        Establecimiento establecimiento, 
        Contratista contratista, 
        Deposito deposito, 
        String remitoNumero,
        Campania campania, 
        String convenio,
        Usuario creadoPor
    ) throws ValidacionException {
        this(id, numero, fecha, labor, establecimiento, contratista, deposito, remitoNumero,
                campania, convenio, null, creadoPor);
    }

    public OrdenTrabajo(Integer id, String numero, LocalDate fecha, Labor labor,
                        Establecimiento establecimiento, Contratista contratista, Deposito deposito,
                        String remitoNumero, Campania campania, String convenio,
                        String comentarios, Usuario creadoPor) throws ValidacionException {
        super(id, creadoPor);
        this.numero = Errores.limpiar(numero);
        this.fecha = fecha;
        this.labor = labor;
        this.establecimiento = establecimiento;
        this.contratista = contratista;
        this.deposito = deposito;
        this.remitoNumero = Errores.limpiar(remitoNumero);
        this.campania = campania;
        this.convenio = Errores.limpiar(convenio);
        this.comentarios = Errores.limpiar(comentarios);
        revisarCampos().lanzarSiHay();
    }

    private Errores revisarCampos() {
        return new Errores()
            .obligatorio(fecha, "La fecha")
            .obligatorio(labor, "La labor")
            .obligatorio(establecimiento, "El establecimiento")
            .obligatorio(contratista, "El contratista")
            .obligatorio(campania, "La campaña")
            .si(numero != null && !FORMATO_NUMERO.matcher(numero).matches(),
                    "El número de orden no cumple el formato NN-NN-NNNN.");
    }

    @Override
    public void validar() throws ValidacionException {
        revisarCampos()
            .si(lotes.isEmpty(), "La orden debe tener al menos un lote.")
            .lanzarSiHay();
    }

    public void asignarNumero(String numero) throws ValidacionException {
        if (this.numero != null) {
            throw new IllegalStateException("La orden ya tiene número.");
        }
        new Errores()
                .si(numero == null || !FORMATO_NUMERO.matcher(numero.trim()).matches(),
                        "El número de orden no cumple el formato NN-NN-NNNN.")
                .lanzarSiHay();
        this.numero = numero.trim();
    }

    public void agregarLote(OrdenLote ordenLote) throws ValidacionException {
        Errores errores = new Errores().obligatorio(ordenLote, "El lote de la orden");
        if (ordenLote != null) {
            errores.si(!establecimiento.equals(ordenLote.getLote().getEstablecimiento()),
                            "El lote " + ordenLote.getLote().getNombre() + " no pertenece al establecimiento de la orden.")
                    .si(lotes.stream().anyMatch(ol -> ol.getLote().equals(ordenLote.getLote())),
                            "El lote " + ordenLote.getLote().getNombre() + " ya está en la orden.");
        }
        errores.lanzarSiHay();
        lotes.add(ordenLote);
    }

    public void agregarInsumo(OrdenInsumo ordenInsumo) throws ValidacionException {
        Errores errores = new Errores().obligatorio(ordenInsumo, "El insumo de la orden");
        if (ordenInsumo != null) {
            errores.si(insumos.stream().anyMatch(oi -> oi.getInsumo().equals(ordenInsumo.getInsumo())),
                "El insumo " + ordenInsumo.getInsumo().getNombre() + " ya esta en la orden");
        }
        errores.lanzarSiHay();
        insumos.add(ordenInsumo);
    }

    public void agregarAplicacion(AplicacionInsumo aplicacion) throws ValidacionException {
        Errores errores = new Errores().obligatorio(aplicacion, "La aplicación");
        if (aplicacion != null) {
            errores.si(!lotes.contains(aplicacion.getOrdenLote()), "El lote de la aplicación no pertenece a la orden.")
                    .si(!insumos.contains(aplicacion.getOrdenInsumo()), "El insumo de la aplicación no pertenece a la orden.")
                    .si(aplicaciones.stream().anyMatch(a -> a.getOrdenLote().equals(aplicacion.getOrdenLote())
                                    && a.getOrdenInsumo().equals(aplicacion.getOrdenInsumo())),
                            "Ya hay una aplicación de ese insumo en ese lote.");
        }
        errores.lanzarSiHay();
        aplicaciones.add(aplicacion);
    }

    /**
     * Corrige la carga antes de guardar: solo se quita un insumo que todavía no está en la base
     * y que ninguna aplicación usa.
     */
    public void quitarInsumo(OrdenInsumo ordenInsumo) throws ValidacionException {
        new Errores()
            .si(!insumos.contains(ordenInsumo), "El insumo no está en la orden.")
            .si(ordenInsumo != null && ordenInsumo.getId() != null,
                "El insumo ya fue guardado y no puede quitarse.")
            .si(aplicaciones.stream().anyMatch(a -> a.getOrdenInsumo().equals(ordenInsumo)),
                "El insumo se usa en una aplicación: primero quitá la aplicación.")
            .lanzarSiHay();
        insumos.remove(ordenInsumo);
    }

    /** Solo se quita una aplicación que todavía no está en la base. */
    public void quitarAplicacion(AplicacionInsumo aplicacion) throws ValidacionException {
        new Errores()
            .si(!aplicaciones.contains(aplicacion), "La aplicación no está en la orden.")
            .si(aplicacion != null && aplicacion.getId() != null,
                "La aplicación ya fue guardada y no puede quitarse.")
            .lanzarSiHay();
        aplicaciones.remove(aplicacion);
    }

    public String getNumero() {
        return numero;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Labor getLabor() {
        return labor;
    }

    public Establecimiento getEstablecimiento() {
        return establecimiento;
    }

    public Contratista getContratista() {
        return contratista;
    }

    public Deposito getDeposito() {
        return deposito;
    }

    public String getRemitoNumero() {
        return remitoNumero;
    }

    public Campania getCampania() {
        return campania;
    }

    public String getConvenio() {
        return convenio;
    }

    public String getComentarios() { return comentarios; }

    public List<OrdenLote> getLotes() {
        return Collections.unmodifiableList(lotes);
    }

    public List<OrdenInsumo> getInsumos() {
        return Collections.unmodifiableList(insumos);
    }

    public List<AplicacionInsumo> getAplicaciones() {
        return Collections.unmodifiableList(aplicaciones);
    }

    
}
