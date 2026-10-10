package com.comp.spiapp.repository;

import java.util.List;

import com.comp.spiapp.model.Campania;
import com.comp.spiapp.model.Contratista;
import com.comp.spiapp.model.Cultivo;
import com.comp.spiapp.model.Deposito;
import com.comp.spiapp.model.Establecimiento;
import com.comp.spiapp.model.Insumo;
import com.comp.spiapp.model.Labor;
import com.comp.spiapp.model.Lote;

/**
 * Agrupa las listas de opciones que necesita el formulario de
 * órdenes de trabajo
 */
public record CatalogosOrden(
    List<Campania> campanias,
    List<Establecimiento> establecimientos,
    List<Lote> lotes,
    List<Labor> labores,
    List<Contratista> contratistas,
    List<Deposito> depositos,
    List<Cultivo> cultivos,
    List<Insumo> insumos
) {
    public CatalogosOrden {
        campanias = List.copyOf(campanias);
        establecimientos = List.copyOf(establecimientos);
        lotes = List.copyOf(lotes);
        labores = List.copyOf(labores);
        contratistas = List.copyOf(contratistas);
        depositos = List.copyOf(depositos);
        cultivos = List.copyOf(cultivos);
        insumos = List.copyOf(insumos);
    }
}
