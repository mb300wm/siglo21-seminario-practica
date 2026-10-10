package com.comp.spiapp.service;

import com.comp.spiapp.model.Usuario;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SesionUsuarioTest {
    @Test
    void conservaLaIdentidadParaAuditoriaYLaEliminaAlSalir() throws Exception {
        var sesion = new SesionUsuario();
        var usuario = new Usuario(3, "Mateo", "mateo", "hash", true);
        assertFalse(sesion.estaIniciada());
        assertThrows(IllegalStateException.class, sesion::requerirUsuario);
        sesion.iniciar(usuario);
        assertSame(usuario, sesion.requerirUsuario());
        assertEquals(3, sesion.requerirUsuario().getId());
        sesion.cerrar();
        sesion.cerrar();
        assertFalse(sesion.estaIniciada());
        assertThrows(IllegalStateException.class, sesion::requerirUsuario);
    }

    @Test
    void rechazaUsuariosInactivosOTransitoriosYNoComparteSesiones() throws Exception {
        var sesion = new SesionUsuario();
        var inactivo = new Usuario(2, "Histórico", "demo", "hash", false);
        var transitorio = new Usuario(null, "Nuevo", "nuevo", "hash", true);
        assertThrows(IllegalArgumentException.class, () -> sesion.iniciar(inactivo));
        assertThrows(IllegalArgumentException.class, () -> sesion.iniciar(transitorio));
        assertThrows(IllegalArgumentException.class, () -> sesion.iniciar(null));
        sesion.iniciar(new Usuario(3, "Mateo", "mateo", "hash", true));
        assertFalse(new SesionUsuario().estaIniciada());
    }
}
