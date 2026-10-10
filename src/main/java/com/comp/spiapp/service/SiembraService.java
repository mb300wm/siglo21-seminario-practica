package com.comp.spiapp.service;

import java.sql.SQLException;
import java.util.Objects;

import com.comp.spiapp.model.Siembra;
import com.comp.spiapp.model.ValidacionException;
import com.comp.spiapp.repository.*;

/**
 * SiembraService
 * CU-02: valida la siembra y la guarda con el usuario que la registró.
 */
public final class SiembraService {
    private final ConexionBD conexiones;
    private final CatalogoRepository catalogos;
    private final SiembraRepository siembras;

    public SiembraService() {
        this(ConexionBD.desdeConfiguracion(), new CatalogoRepository(), new SiembraRepository());
    }

    public SiembraService(
        ConexionBD conexiones,
        CatalogoRepository catalogos, 
        SiembraRepository siembras
    ) {
        this.conexiones = Objects.requireNonNull(conexiones);
        this.catalogos = Objects.requireNonNull(catalogos);
        this.siembras = Objects.requireNonNull(siembras);
    }

    public Catalogos cargarCatalogos() throws OperacionException {
        try (var conexion = conexiones.abrir()) {
            return catalogos.cargar(conexion);
        } catch (SQLException ex) {
            throw OperacionException.desde(ex, "cargar los datos del formulario");
        }
    }

    /**
     * Valida antes de abrir la conexion, asi una siembra invalida nunca llega a MySQL.
     */
    public int registrar(Siembra siembra) throws ValidacionException, OperacionException {
        Objects.requireNonNull(siembra);
        if (siembra.getId() != null) {
            throw new IllegalArgumentException("La siembra ya fue registrada.");
        }
        siembra.validar();
        try (var conexion = conexiones.abrir()) {
            int id = siembras.insertar(siembra, conexion);
            siembra.asignarId(id);
            return id;
        } catch (SQLException ex) {
            throw OperacionException.desde(ex, "guardar la siembra");
        }
    }
}
