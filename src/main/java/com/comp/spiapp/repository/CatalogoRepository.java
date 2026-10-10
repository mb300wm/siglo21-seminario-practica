package com.comp.spiapp.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comp.spiapp.model.Acopio;
import com.comp.spiapp.model.Campania;
import com.comp.spiapp.model.Contratista;
import com.comp.spiapp.model.Cultivo;
import com.comp.spiapp.model.Deposito;
import com.comp.spiapp.model.Establecimiento;
import com.comp.spiapp.model.GrupoInsumo;
import com.comp.spiapp.model.Insumo;
import com.comp.spiapp.model.Labor;
import com.comp.spiapp.model.Lote;
import com.comp.spiapp.model.ValidacionException;

public final class CatalogoRepository {

    public Catalogos cargar(Connection conexion) throws SQLException {
        List<Establecimiento> establecimientos = establecimientos(conexion);
        return new Catalogos(
            campanias(conexion), 
            establecimientos, 
            lotes(conexion, establecimientos), 
            semillas(conexion), 
            acopios(conexion)
        );
    }

    public List<Campania> campanias(Connection conexion) throws SQLException {
        String sql = "SELECT campana_id, nombre FROM campanas ORDER BY nombre DESC";
        List<Campania> campanias = new ArrayList<>();
        try (var consulta = conexion.prepareStatement(sql); var filas = consulta.executeQuery()) {
                while (filas.next()) {
                    campanias.add(
                        new Campania(
                            filas.getInt("campana_id"), 
                            filas.getString("nombre"))
                        );
                }
        } catch (ValidacionException ex) {
            throw datosInvalidos(ex);
        }
        return campanias;
    }

    public List<Establecimiento> establecimientos(Connection conexion) throws SQLException {
        String sql = "SELECT establecimiento_id, nombre FROM establecimientos ORDER BY nombre";
        List<Establecimiento> establecimientos = new ArrayList<>();
        try (var consulta = conexion.prepareStatement(sql); var filas = consulta.executeQuery()) {
                while (filas.next()) {
                    establecimientos.add(
                        new Establecimiento(
                            filas.getInt("establecimiento_id"), 
                            filas.getString("nombre"))
                        );
                }
        } catch (ValidacionException ex) {
            throw datosInvalidos(ex);
        }
        return establecimientos;
    }

    public List<Lote> lotes(
        Connection conexion, 
        List<Establecimiento> establecimientos
    ) throws SQLException {
        Map<Integer, Establecimiento> porId = new HashMap<>();
        for (Establecimiento establecimiento : establecimientos) {
            porId.put(establecimiento.getId(), establecimiento);
        }
        String sql = "SELECT lote_id, establecimiento_id, nombre, hectareas FROM lotes ORDER BY nombre";
        List<Lote> lotes = new ArrayList<>();
        try (var consulta = conexion.prepareStatement(sql); var filas = consulta.executeQuery()) {
            while (filas.next()) {
                lotes.add(new Lote(filas.getInt("lote_id"), 
                    filas.getString("nombre"),
                    porId.get(filas.getInt("establecimiento_id")), 
                    filas.getBigDecimal("hectareas")));
            }
        } catch (ValidacionException ex) {
            throw datosInvalidos(ex);
        }
        return lotes;
    }

    public List<Insumo> semillas(Connection conexion) throws SQLException {
        String sql = """
                SELECT i.insumo_id, i.nombre, g.grupo_id, g.nombre AS grupo, c.cultivo_id, c.nombre AS cultivo
                FROM insumos i
                JOIN grupos_insumo g ON g.grupo_id = i.grupo_id
                JOIN cultivos c ON c.cultivo_id = g.cultivo_id
                ORDER BY c.nombre, i.nombre
                """;

        List<Insumo> semillas = new ArrayList<>();
        try (var consulta = conexion.prepareStatement(sql); var filas = consulta.executeQuery()) {
            while (filas.next()) {
                var cultivo = new Cultivo(filas.getInt("cultivo_id"), filas.getString("cultivo"));
                var grupo = new GrupoInsumo(filas.getInt("grupo_id"), filas.getString("grupo"), cultivo);
                semillas.add(new Insumo(filas.getInt("insumo_id"), filas.getString("nombre"), grupo, null));
            }
        } catch (ValidacionException ex) {
            throw datosInvalidos(ex);
        }
        return semillas;
    }

    public List<Acopio> acopios(Connection conexion) throws SQLException {
        String sql = "SELECT acopio_id, nombre FROM acopios ORDER BY nombre";
        List<Acopio> acopios = new ArrayList<>();
        try (var consulta = conexion.prepareStatement(sql); var filas = consulta.executeQuery()) {
            while (filas.next()) {
                acopios.add(new Acopio(filas.getInt("acopio_id"), filas.getString("nombre")));
            }
        } catch (ValidacionException ex) {
            throw datosInvalidos(ex);
        }
        return acopios;
    }

    /** Opciones del formulario de órdenes de trabajo (CU-04 y CU-05), en una sola conexión. */
    public CatalogosOrden cargarOrden(Connection conexion) throws SQLException {
        List<Establecimiento> establecimientos = establecimientos(conexion);
        return new CatalogosOrden(
            campanias(conexion),
            establecimientos,
            lotes(conexion, establecimientos),
            labores(conexion),
            contratistas(conexion),
            depositos(conexion),
            cultivos(conexion),
            insumos(conexion)
        );
    }

    public List<Labor> labores(Connection conexion) throws SQLException {
        return simples(conexion, "SELECT labor_id AS id, nombre FROM labores ORDER BY nombre", Labor::new);
    }

    public List<Contratista> contratistas(Connection conexion) throws SQLException {
        return simples(conexion, "SELECT contratista_id AS id, nombre FROM contratistas ORDER BY nombre",
            Contratista::new);
    }

    public List<Deposito> depositos(Connection conexion) throws SQLException {
        return simples(conexion, "SELECT deposito_id AS id, nombre FROM depositos ORDER BY nombre", Deposito::new);
    }

    public List<Cultivo> cultivos(Connection conexion) throws SQLException {
        return simples(conexion, "SELECT cultivo_id AS id, nombre FROM cultivos ORDER BY nombre", Cultivo::new);
    }

    /** Todos los insumos con su grupo; el cultivo solo existe en los grupos de semillas. */
    public List<Insumo> insumos(Connection conexion) throws SQLException {
        String sql = """
                SELECT i.insumo_id, i.nombre, g.grupo_id, g.nombre AS grupo, c.cultivo_id, c.nombre AS cultivo
                FROM insumos i
                JOIN grupos_insumo g ON g.grupo_id = i.grupo_id
                LEFT JOIN cultivos c ON c.cultivo_id = g.cultivo_id
                ORDER BY g.nombre, i.nombre
                """;
        List<Insumo> insumos = new ArrayList<>();
        try (var consulta = conexion.prepareStatement(sql); var filas = consulta.executeQuery()) {
            while (filas.next()) {
                Integer cultivoId = filas.getObject("cultivo_id", Integer.class);
                var cultivo = cultivoId == null ? null : new Cultivo(cultivoId, filas.getString("cultivo"));
                var grupo = new GrupoInsumo(filas.getInt("grupo_id"), filas.getString("grupo"), cultivo);
                insumos.add(new Insumo(filas.getInt("insumo_id"), filas.getString("nombre"), grupo, null));
            }
        } catch (ValidacionException ex) {
            throw datosInvalidos(ex);
        }
        return insumos;
    }

    /** Crea un catálogo de solo id y nombre; por ejemplo {@code Labor::new}. */
    @FunctionalInterface
    private interface FabricaCatalogo<T> {
        T crear(Integer id, String nombre) throws ValidacionException;
    }

    /** Lectura común a los catálogos que solo tienen id y nombre. */
    private <T> List<T> simples(Connection conexion, String sql, FabricaCatalogo<T> fabrica) throws SQLException {
        List<T> elementos = new ArrayList<>();
        try (var consulta = conexion.prepareStatement(sql); var filas = consulta.executeQuery()) {
            while (filas.next()) {
                elementos.add(fabrica.crear(filas.getInt("id"), filas.getString("nombre")));
            }
        } catch (ValidacionException ex) {
            throw datosInvalidos(ex);
        }
        return elementos;
    }
    
    private SQLException datosInvalidos(ValidacionException ex) {
        return new SQLException("Un catálogo de la base contiene datos inválidos.", ex);
    }
}
