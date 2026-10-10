package com.comp.spiapp.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import com.comp.spiapp.model.Cosecha;
import com.comp.spiapp.model.ValidacionException;
import com.comp.spiapp.repository.CatalogoRepository;
import com.comp.spiapp.repository.CosechaRepository;
import com.comp.spiapp.repository.ConexionBD;
import com.comp.spiapp.repository.PesajeRepository;
import com.comp.spiapp.repository.SiembraRepository;

/**
 * CU-03: guarda la cosecha y todos sus pesajes en una misma transacción
 */
public final class CosechaService {
    private final ConexionBD conexiones;
    private final SiembraRepository siembras;
    private final CatalogoRepository catalogos;
    private final CosechaRepository cosechas;
    private final PesajeRepository pesajes;

    public CosechaService() {
        this(ConexionBD.desdeConfiguracion(),
            new SiembraRepository(), new CatalogoRepository(),
            new CosechaRepository(), new PesajeRepository());
    }

    public CosechaService(
        ConexionBD conexiones,
        SiembraRepository siembras, 
        CatalogoRepository catalogos,
        CosechaRepository cosechas, 
        PesajeRepository pesajes
    ) {
        this.conexiones = Objects.requireNonNull(conexiones);
        this.siembras = Objects.requireNonNull(siembras);
        this.catalogos = Objects.requireNonNull(catalogos);
        this.cosechas = Objects.requireNonNull(cosechas);
        this.pesajes = Objects.requireNonNull(pesajes);
    }

    public OpcionesCosecha cargarOpciones() throws OperacionException {
        try (var conexion = conexiones.abrir()) {
            return new OpcionesCosecha(siembras.listarVigentes(conexion), catalogos.acopios(conexion));
        } catch (SQLException ex) {
            throw OperacionException.desde(ex, "cargar las siembras registradas");
        }
    }

    /**
     * Si falla cualquier inserción se revierte todo, nunca queda una cosecha sin los camiones cargados.
     */
    public int registrar(Cosecha cosecha) throws ValidacionException, OperacionException {
        Objects.requireNonNull(cosecha);
        if (cosecha.getId() != null) {
            throw new IllegalArgumentException("La cosecha ya fue registrada.");
        }
        cosecha.validar();
        try (Connection conexion = conexiones.abrir()) {
            conexion.setAutoCommit(false);
            try {
                int id = cosechas.insertar(cosecha, conexion);
                pesajes.insertarTodos(id, cosecha.getPesajes(), conexion);
                conexion.commit();
                cosecha.asignarId(id);
                return id;
            } catch (SQLException | RuntimeException ex) {
                revertir(conexion, ex);
                throw ex;
            }
        } catch (SQLException ex) {
            throw OperacionException.desde(ex, "guardar la cosecha");
        }
    }

    /** 
     * Si el rollback también falla, ese error se adjunta sin ocultar el original
     */
    private static void revertir(Connection conexion, Exception original) {
        try {
            conexion.rollback();
        } catch (SQLException ex) {
            original.addSuppressed(ex);
        }
    }
}
