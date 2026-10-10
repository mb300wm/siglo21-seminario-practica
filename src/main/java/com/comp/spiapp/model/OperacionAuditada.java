package com.comp.spiapp.model;

import java.time.LocalDateTime;

/**
 * OperacionAuditada
 * Operación con registro de quién la creó, la modificó y la dio de baja.
 */
public abstract class OperacionAuditada extends Entidad {
    private final Usuario creadoPor;
    private LocalDateTime creadoEn;
    private Usuario modificadoPor;
    private LocalDateTime modificadoEn;
    private Usuario eliminadoPor;
    private LocalDateTime eliminadoEn;

    protected OperacionAuditada(Integer id, Usuario creadoPor) throws ValidacionException {
        super(id);
        new Errores()
        .obligatorio(creadoPor, "El usuario que registra la operacion")
        .lanzarSiHay();
        this.creadoPor = creadoPor;
        this.creadoEn = LocalDateTime.now();
    }

    /**
     * Cada operación revisa sus propias reglas; el servicio lo llama antes de guardar.
     */
    public abstract void validar() throws ValidacionException;

    public void registrarModificacion(Usuario usuario) throws ValidacionException {
        registrarModificacion(usuario, LocalDateTime.now());
    }

    public void registrarModificacion(Usuario usuario, LocalDateTime momento) 
        throws ValidacionException {
            new Errores()
            .obligatorio(usuario, "El usuario que modifica")
            .obligatorio(momento, "La fecha de modificación")
            .si(estaDadaDeBaja(), "La operacion esta dada de baja y no puede modificarse")
            .lanzarSiHay();
        this.modificadoPor = usuario;
        this.modificadoEn = momento;
    }

    public void darDeBaja(Usuario usuario) throws ValidacionException {
        darDeBaja(usuario, LocalDateTime.now());
    }

    public void darDeBaja(Usuario usuario, LocalDateTime momento) throws ValidacionException {
        new Errores()
            .obligatorio(usuario, "El usuario que da de baja")
            .obligatorio(momento, "La fecha de baja")
            .si(estaDadaDeBaja(), "La operacion ya fue dada de baja.")
            .lanzarSiHay();
        this.eliminadoPor = usuario;
        this.eliminadoEn = momento;
    }

    /**
     * 
     * Para el repositorio: reconstruye la auditoria guardada en la base
     */
    public void restaurarAuditoria(
        LocalDateTime creadoEn, 
        Usuario modificadoPor, 
        LocalDateTime modificadoEn,
        Usuario eliminadoPor,
        LocalDateTime eliminadoEn
    ) throws ValidacionException {
        new Errores()
            .obligatorio(creadoEn, "La fecha de creación")
            .si((modificadoPor == null) != (modificadoEn == null),
                "La modificación debe tener usuario y fecha, o ninguno")
            .si((eliminadoPor == null) != (eliminadoEn == null),
                "La baja debe tener usuario y fecha, o ninguno.")
            .lanzarSiHay();

        this.creadoEn = creadoEn;
        this.modificadoPor = modificadoPor;
        this.modificadoEn = modificadoEn;
        this.eliminadoPor = eliminadoPor;
        this.eliminadoEn = eliminadoEn;
    }

    public boolean estaDadaDeBaja() {
        return eliminadoEn != null;
    }
    
    public Usuario getCreadoPor() {
        return creadoPor;
    }
    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
    public Usuario getModificadoPor() {
        return modificadoPor;
    }
    public LocalDateTime getModificadoEn() {
        return modificadoEn;
    }
    public Usuario getEliminadoPor() {
        return eliminadoPor;
    }
    public LocalDateTime getEliminadoEn() {
        return eliminadoEn;
    }
}
