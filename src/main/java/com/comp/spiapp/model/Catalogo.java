package com.comp.spiapp.model;

public abstract class Catalogo extends Entidad {
    private final String nombre;

    protected Catalogo(Integer id, String nombre) throws ValidacionException {
        super(id);
        new Errores().texto(nombre, "El nombre").lanzarSiHay();
        this.nombre = nombre.trim();
    }

    public String getNombre() {return nombre;}

    @Override
    public String toString() {
        return nombre;
    }
}
