package com.comp.spiapp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InsumoTest {
    @Test
    void esSemillaSoloSiElGrupoTieneCultivo() throws ValidacionException {
        assertTrue(Datos.semilla().esSemilla());
        assertFalse(Datos.herbicida(2).esSemilla());
    }

    @Test 
    void elPrincipioActivoDebeSerDelMismoGrupo() throws ValidacionException {
        PrincipioActivo glifosato = new PrincipioActivo(1, "Glifosato", Datos.grupoHerbicida());
        assertThrows(ValidacionException.class, 
            () -> new Insumo(null, "Semilla rara", Datos.grupoSemilla(), glifosato));

        assertEquals(glifosato,
            new Insumo(null, "Roundup", Datos.grupoHerbicida(), glifosato).getPrincipioActivo());
    }
}
