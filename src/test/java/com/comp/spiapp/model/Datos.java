package com.comp.spiapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

final class Datos {
    static final LocalDate HOY = LocalDate.of(2026, 3, 10);

    private Datos() {
    }

    static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    static Usuario usuario() throws ValidacionException {
        return new Usuario(1, "Auditoría", "auditoria", "hash", false);
    }

    static Campania campania() throws ValidacionException {
        return new Campania(1, "25/26");
    }

    static Establecimiento establecimiento(int id) throws ValidacionException {
        return new Establecimiento(id, "Establecimiento " + id);
    }

    static Lote lote(int id, Establecimiento establecimiento) throws ValidacionException {
        return new Lote(id, "Lote " + id, establecimiento, bd("20"));
    }

    static GrupoInsumo grupoSemilla() throws ValidacionException {
        return new GrupoInsumo(1, "Semillas de soja", new Cultivo(1, "Soja"));
    }

    static GrupoInsumo grupoHerbicida() throws ValidacionException {
        return new GrupoInsumo(2, "Herbicidas", null);
    }

    static Insumo semilla() throws ValidacionException {
        return new Insumo(1, "Soja DM 40", grupoSemilla(), null);
    }

    static Insumo herbicida(int id) throws ValidacionException {
        return new Insumo(id, "Herbicida " + id, grupoHerbicida(), null);
    }

    static Siembra siembra() throws ValidacionException {
        return new Siembra(1, campania(), lote(1, establecimiento(1)), semilla(), HOY, HOY.plusDays(2),
                bd("1200"), bd("20"), "Tractor", "Sembradora", usuario());
    }

    static Cosecha cosecha(String hectareas) throws ValidacionException {
        return new Cosecha(1, siembra(), HOY.plusMonths(6), HOY.plusMonths(6).plusDays(1),
                hectareas == null ? null : bd(hectareas), bd("13.5"), null, usuario());
    }

    static OrdenTrabajo orden(Establecimiento establecimiento) throws ValidacionException {
        return new OrdenTrabajo(1, null, HOY, new Labor(1, "Pulverización"), establecimiento,
                new Contratista(1, "Contratista"), null, null, campania(), null, usuario());
    }
}
