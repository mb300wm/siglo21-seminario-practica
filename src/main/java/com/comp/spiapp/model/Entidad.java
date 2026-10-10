package com.comp.spiapp.model;

public abstract class Entidad {
    private Integer id;

    protected Entidad(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void asignarId(int id) {
        if (this.id != null) {
            throw new IllegalStateException("El id ya fue asignado.");
        }
        this.id = id;
    }

    /** Dos objetos son iguales si son del mismo tipo y tienen el mismo id; sin ID, solo si son el mismo objeto. */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (otro == null || getClass() != otro.getClass()) {
            return false;
        }
        return id != null && id.equals(((Entidad) otro).id);
    }

    /** Constante por tipo: no cambia cuando se asigna el ID. */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
