package com.comp.spiapp.model;

/**
 * PrincipioActivo
 */

public final class PrincipioActivo extends Catalogo {
    private final GrupoInsumo grupo;

    public PrincipioActivo(Integer id, String nombre, GrupoInsumo grupo) throws ValidacionException {
        super(id, nombre);
        new Errores().obligatorio(grupo, "El grupo de insumo").lanzarSiHay();
        this.grupo = grupo;
    }

    public GrupoInsumo getGrupo() {
        return grupo;
    }
}
