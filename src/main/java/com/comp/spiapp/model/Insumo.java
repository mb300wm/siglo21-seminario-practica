package com.comp.spiapp.model;

/*
 * Insumo
 */
public final class Insumo extends Catalogo {
    private final GrupoInsumo grupo;
    private final PrincipioActivo principioActivo; // opcional

    public Insumo(
        Integer id, 
        String nombre, 
        GrupoInsumo grupo,
        PrincipioActivo principioActivo
    ) throws ValidacionException {
        super(id, nombre);
        new Errores()
            .obligatorio(grupo, "El grupo de insumo")
            .si(grupo != null 
                && principioActivo != null
                && !grupo.equals(principioActivo.getGrupo()), 
                "El principio activo no pertenece al grupo del insumo.")
            .lanzarSiHay();
        this.grupo = grupo;
        this.principioActivo = principioActivo;
    }

    public GrupoInsumo getGrupo() {
        return grupo;
    }

    public PrincipioActivo getPrincipioActivo() {
        return principioActivo;
    }

    public boolean esSemilla() {
        return grupo.esDeSemilla();
    }
}
