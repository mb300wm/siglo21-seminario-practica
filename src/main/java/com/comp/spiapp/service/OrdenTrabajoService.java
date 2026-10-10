package com.comp.spiapp.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.comp.spiapp.model.AplicacionInsumo;
import com.comp.spiapp.model.OrdenInsumo;
import com.comp.spiapp.model.OrdenTrabajo;
import com.comp.spiapp.model.Usuario;
import com.comp.spiapp.model.ValidacionException;
import com.comp.spiapp.repository.*;

/**
 * OrdenTrabajoService
 */
public final class OrdenTrabajoService {
    private static final Pattern PREFIJO_CAMPANIA = Pattern.compile("^[0-9]{2}-[0-9]{2}$");
    private static final int ULTIMO_CORRELATIVO = 9999;

    private final ConexionBD conexiones;
    private final CatalogoRepository catalogos;
    private final OrdenTrabajoRepository ordenes;
    private final DetalleOrdenRepository detalles;

    public OrdenTrabajoService() {
        this(ConexionBD.desdeConfiguracion(), new CatalogoRepository(),
            new OrdenTrabajoRepository(), new DetalleOrdenRepository());
    }

    public OrdenTrabajoService(
        ConexionBD conexiones,
        CatalogoRepository catalogos,
        OrdenTrabajoRepository ordenes,
        DetalleOrdenRepository detalles
    ) {
        this.conexiones = Objects.requireNonNull(conexiones);
        this.catalogos = Objects.requireNonNull(catalogos);
        this.ordenes = Objects.requireNonNull(ordenes);
        this.detalles = Objects.requireNonNull(detalles);
    }

    public CatalogosOrden cargarCatalogos() throws OperacionException {
        try (var conexion = conexiones.abrir()) {
            return catalogos.cargarOrden(conexion);
        } catch (SQLException ex) {
            throw OperacionException.desde(ex, "cargar los datos del formulario");
        }
    }

    public List<ResumenOrden> listarOrdenes() throws OperacionException {
        try (var conexion = conexiones.abrir()) {
            return ordenes.listarVigentes(conexion);
        } catch (SQLException ex) {
            throw OperacionException.desde(ex, "cargar las órdenes de trabajo");
        }
    }

    /** Orden vigente completa: cabecera, lotes, insumos y aplicaciones. */
    public OrdenTrabajo buscar(int ordenId) throws ValidacionException, OperacionException {
        try (var conexion = conexiones.abrir()) {
            var orden = ordenes.buscarCabecera(ordenId, conexion);
            if (orden.isEmpty()) {
                throw new ValidacionException("La orden ya no existe o fue dada de baja.");
            }
            detalles.cargar(orden.get(), conexion);
            return orden.get();
        } catch (SQLException ex) {
            throw OperacionException.desde(ex, "abrir la orden de trabajo");
        }
    }


    /**
     * CU-04, paso 3: bloquea la campaña, genera el número y guarda la cabecera con sus lotes en una
     * misma transacción. El bloqueo dura hasta el commit, así dos sesiones nunca obtienen el mismo número.
     * El número y los ids se asignan recién después del commit: si algo falla, la orden queda como estaba.
     */
    public String registrar(OrdenTrabajo orden) throws ValidacionException, OperacionException {
        Objects.requireNonNull(orden);
        if (orden.getId() != null || orden.getNumero() != null) {
            throw new IllegalArgumentException("La orden ya fue registrada.");
        }
        orden.validar();
        try (Connection conexion = conexiones.abrir()) {
            conexion.setAutoCommit(false);
            try {
                String numero = siguienteNumero(orden, conexion);
                int id = ordenes.insertar(orden, numero, conexion);
                List<Integer> idsLotes = detalles.insertarLotes(id, orden.getLotes(), conexion);
                conexion.commit();

                orden.asignarId(id);
                orden.asignarNumero(numero);
                for (int i = 0; i < idsLotes.size(); i++) {
                    orden.getLotes().get(i).asignarId(idsLotes.get(i));
                }
                return numero;
            } catch (SQLException | ValidacionException | RuntimeException ex) {
                revertir(conexion, ex);
                throw ex;
            }
        } catch (SQLException ex) {
            throw OperacionException.desde(ex, "guardar la orden de trabajo");
        }
    }

    /**
     * El bloqueo va primero: en MySQL la primera lectura común de la transacción fija la "foto" de los
     * datos, y tiene que tomarse después de que la otra sesión confirmó su orden. Si el MAX se leyera
     * antes de bloquear, dos sesiones simultáneas calcularían el mismo número (prueba P09).
     */
    private String siguienteNumero(OrdenTrabajo orden, Connection conexion)
            throws SQLException, ValidacionException {
        String prefijo = ordenes.bloquearCampania(orden.getCampania(), conexion);
        if (!PREFIJO_CAMPANIA.matcher(prefijo).matches()) {
            throw new ValidacionException("La campaña " + prefijo
                    + " no tiene el formato AA-AA que usa el número de orden.");
        }
        int siguiente = ordenes.ultimoCorrelativo(orden.getCampania(), conexion) + 1;
        if (siguiente > ULTIMO_CORRELATIVO) {
            throw new ValidacionException("La campaña " + prefijo + " ya usó los 9999 números de orden.");
        }
        return "%s-%04d".formatted(prefijo, siguiente);
    }

    /**
     * CU-05: guarda los insumos y las aplicaciones que todavía no tienen id, y registra la modificación
     * con el usuario de la sesión (RNF10). Todo en una transacción.
     */
    public int registrarAplicaciones(OrdenTrabajo orden, Usuario usuario)
            throws ValidacionException, OperacionException {
        Objects.requireNonNull(orden);
        Objects.requireNonNull(usuario);
        if (orden.getId() == null) {
            throw new IllegalArgumentException("Primero hay que guardar la orden.");
        }
        List<OrdenInsumo> insumosNuevos = orden.getInsumos().stream()
            .filter(i -> i.getId() == null).toList();

        List<AplicacionInsumo> aplicacionesNuevas =
                orden.getAplicaciones().stream()
                    .filter(a -> a.getId() == null).toList();
        
        if (insumosNuevos.isEmpty() && aplicacionesNuevas.isEmpty()) {
            throw new ValidacionException("No hay insumos ni aplicaciones nuevos para guardar.");
        }
        LocalDateTime momento = LocalDateTime.now();

        try (Connection conexion = conexiones.abrir()) {
            conexion.setAutoCommit(false);
            try {
                if (!ordenes.registrarModificacion(orden.getId(), usuario, momento, conexion)) {
                    throw new ValidacionException("La orden fue dada de baja y ya no admite cambios.");
                }
                // IdentityHashMap: los objetos nuevos todavía no tienen id, se distinguen por referencia.
                // https://docs.oracle.com/javase/8/docs/api/java/util/IdentityHashMap.html
                Map<OrdenInsumo, Integer> idsInsumos = new IdentityHashMap<>();
                for (OrdenInsumo insumo : insumosNuevos) {
                    idsInsumos.put(insumo, detalles.insertarInsumo(orden.getId(), insumo, conexion));
                }
                
                Map<AplicacionInsumo, Integer> idsAplicaciones = new IdentityHashMap<>();

                for (AplicacionInsumo aplicacion : aplicacionesNuevas) {
                    OrdenInsumo insumo = aplicacion.getOrdenInsumo();

                    int ordenInsumoId = insumo.getId() != null ? insumo.getId() : idsInsumos.get(insumo);

                    idsAplicaciones.put(aplicacion, detalles.insertarAplicacion(orden.getId(),
                            aplicacion.getOrdenLote().getId(), ordenInsumoId, aplicacion.getTotal(), conexion));
                }
                conexion.commit();

                idsInsumos.forEach((insumo, id) -> insumo.asignarId(id));
                idsAplicaciones.forEach((aplicacion, id) -> aplicacion.asignarId(id));
                orden.registrarModificacion(usuario, momento);
                return insumosNuevos.size() + aplicacionesNuevas.size();
            } catch (SQLException | ValidacionException | RuntimeException ex) {
                revertir(conexion, ex);
                throw ex;
            }
        } catch (SQLException ex) {
            throw OperacionException.desde(ex, "guardar los insumos y las aplicaciones");
        }
    }

    /** Si el rollback también falla, ese error se adjunta sin ocultar el original. */
    private static void revertir(Connection conexion, Exception original) {
        try {
            conexion.rollback();
        } catch (SQLException ex) {
            original.addSuppressed(ex);
        }
    }
}
