package com.comp.spiapp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CatalogoTest {
    @Test
    void recortaElNombre() throws ValidacionException {
        assertEquals("Soja", new Cultivo(null, "  Soja ").getNombre());
    }

    @Test
    void rechazaNombreVacio() {
        ValidacionException e = assertThrows(ValidacionException.class, () -> new Cultivo(null, "  "));
        assertEquals("El nombre es un dato obligatorio.", e.getMessage());
    }

    @Test
    void elIdSeAsignaUnaSolaVez() throws ValidacionException {
        Cultivo cultivo = new Cultivo(null, "Maíz");
        cultivo.asignarId(5);
        assertEquals(5, cultivo.getId());
        assertThrows(IllegalStateException.class, () -> cultivo.asignarId(6));
    }

    @Test
    void sonIgualesPorTipoEId() throws ValidacionException {
        assertEquals(new Cultivo(1, "Soja"), new Cultivo(1, "Otro nombre"));
        assertNotEquals(new Cultivo(1, "Soja"), new Cultivo(2, "Soja"));
        assertNotEquals(new Cultivo(1, "Soja"), new Labor(1, "Soja"));
    }

    @Test
    void sinIdSoloSonIgualesSiEsElMismoObjeto() throws ValidacionException {
        Cultivo a = new Cultivo(null, "Soja");
        assertEquals(a, a);
        assertNotEquals(a, new Cultivo(null, "Soja"));
    }

    @Test
    void loteRechazaHectareasEnCeroYAceptaSinDato() throws ValidacionException {
        Establecimiento campo = Datos.establecimiento(1);
        assertThrows(ValidacionException.class, () -> new Lote(null, "L1", campo, Datos.bd("0")));
        assertNull(new Lote(null, "L1", campo, null).getHectareas());
    }

    @Test
    void usuarioExigeLoginYHash() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> new Usuario(null, "Ana", " ", "", true));
        assertEquals(2, e.getErrores().size());
    }

    @Test
    void loteRechazaUnaSuperficieQueSeRedondeaACero() throws ValidacionException {
        Establecimiento campo = Datos.establecimiento(1);
        assertThrows(ValidacionException.class, () -> new Lote(null, "L1", campo, Datos.bd("0.004")));
        assertEquals(Datos.bd("0.01"), new Lote(null, "L1", campo, Datos.bd("0.005")).getHectareas());
    }
}
