package com.comp.spiapp.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class OperacionAuditadaTest {
    @Test
    void registraLaCreacion() throws ValidacionException {
        Siembra siembra = Datos.siembra();
        assertEquals(Datos.usuario(), siembra.getCreadoPor());
        assertNotNull(siembra.getCreadoEn());
    }

    @Test
    void darDeBajaGuardaUsuarioYMomento() throws ValidacionException {
        Siembra siembra = Datos.siembra();
        LocalDateTime momento = LocalDateTime.of(2026, 5, 1, 10, 0);
        siembra.darDeBaja(Datos.usuario(), momento);
        assertTrue(siembra.estaDadaDeBaja());
        assertEquals(momento, siembra.getEliminadoEn());
        assertEquals(Datos.usuario(), siembra.getEliminadoPor());
    }

    @Test
    void noSePuedeDarDeBajaDosVeces() throws ValidacionException {
        Siembra siembra = Datos.siembra();
        siembra.darDeBaja(Datos.usuario());
        assertThrows(ValidacionException.class, () -> siembra.darDeBaja(Datos.usuario()));
    }

    @Test
    void laBajaExigeUsuario() throws ValidacionException {
        Siembra siembra = Datos.siembra();
        assertThrows(ValidacionException.class, () -> siembra.darDeBaja(null));
        assertFalse(siembra.estaDadaDeBaja());
    }

    @Test
    void unaOperacionDadaDeBajaNoSeModifica() throws ValidacionException {
        Siembra siembra = Datos.siembra();
        siembra.darDeBaja(Datos.usuario());
        assertThrows(ValidacionException.class, () -> siembra.registrarModificacion(Datos.usuario()));
    }

    @Test
    void restauraLaAuditoriaGuardadaYRechazaPares_incompletos() throws ValidacionException {
        Siembra siembra = Datos.siembra();
        LocalDateTime creada = LocalDateTime.of(2026, 3, 1, 8, 0);
        siembra.restaurarAuditoria(creada, null, null, null, null);
        assertEquals(creada, siembra.getCreadoEn());
        assertThrows(ValidacionException.class,
                () -> siembra.restaurarAuditoria(creada, Datos.usuario(), null, null, null));
    }

    @Test
    void validarSeResuelvePorPolimorfismo() throws ValidacionException {
        OperacionAuditada operacion = Datos.siembra();
        assertDoesNotThrow(operacion::validar);
        OperacionAuditada cosechaSinPesajes = Datos.cosecha("20");
        assertThrows(ValidacionException.class, cosechaSinPesajes::validar);
    }

    @Test
    void unaBajaSinFechaNoDejaUnaAuditoriaParcial() throws ValidacionException {
        Siembra siembra = Datos.siembra();
        assertThrows(ValidacionException.class, () -> siembra.darDeBaja(Datos.usuario(), null));
        assertFalse(siembra.estaDadaDeBaja());
        assertNull(siembra.getEliminadoPor());
        assertNull(siembra.getEliminadoEn());
        assertDoesNotThrow(() -> siembra.darDeBaja(Datos.usuario()));
    }

    @Test
    void unaModificacionSinFechaConservaLaAuditoriaAnterior() throws ValidacionException {
        Siembra siembra = Datos.siembra();
        LocalDateTime anterior = LocalDateTime.of(2026, 3, 20, 8, 0);
        siembra.registrarModificacion(Datos.usuario(), anterior);
        Usuario otro = new Usuario(2, "Otra persona", "otra", "hash-prueba", true);
        assertThrows(ValidacionException.class, () -> siembra.registrarModificacion(otro, null));
        assertEquals(Datos.usuario(), siembra.getModificadoPor());
        assertEquals(anterior, siembra.getModificadoEn());
    }

}
