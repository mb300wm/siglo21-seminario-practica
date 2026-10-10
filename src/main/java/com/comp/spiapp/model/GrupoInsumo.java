package com.comp.spiapp.model;

/**
 * GrupoInsumo
 */
public final class GrupoInsumo extends Catalogo {
    private final Cultivo cultivo;

    public GrupoInsumo(Integer id, String nombre, Cultivo cultivo) throws ValidacionException {
        super(id, nombre);
        this.cultivo = cultivo;
    }

    public Cultivo getCultivo() {
        return cultivo;
    }

    public boolean esDeSemilla() {
        return cultivo != null;
    }
}
