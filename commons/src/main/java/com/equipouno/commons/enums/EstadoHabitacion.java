package com.equipouno.commons.enums;

import com.equipouno.commons.exceptions.DatoInvalidoException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum EstadoHabitacion {
    DISPONIBLE(1L, "Lista para asignarse") {
        @Override
        public Set<EstadoHabitacion> transicionesManualesPermitidas() {
            return EnumSet.of(LIMPIEZA, MANTENIMIENTO);
        }

        @Override
        public Set<EstadoHabitacion> transicionesPorReservaPermitidas() {
            return EnumSet.of(OCUPADA);                 // crear reserva
        }
    },
    OCUPADA(2L, "Asignada a una reserva vigente") {
        @Override
        public Set<EstadoHabitacion> transicionesManualesPermitidas() {
            // Desde OCUPADA nadie puede cambiarla manualmente (retorna vacío)
            return EnumSet.noneOf(EstadoHabitacion.class);
        }

        @Override
        public Set<EstadoHabitacion> transicionesPorReservaPermitidas() {
            return EnumSet.of(LIMPIEZA, DISPONIBLE);    // check-out / cancelación
        }
    },
    LIMPIEZA(3L, "En proceso de limpieza") {
        @Override
        public Set<EstadoHabitacion> transicionesManualesPermitidas() {
            return EnumSet.of(DISPONIBLE, MANTENIMIENTO);
        }
    },
    MANTENIMIENTO(4L, "En reparación") {
        @Override
        public Set<EstadoHabitacion> transicionesManualesPermitidas() {
            return EnumSet.of(DISPONIBLE, LIMPIEZA);
        }
    };

    private final Long codigo;
    private final String descripcion;
    public abstract Set<EstadoHabitacion> transicionesManualesPermitidas();
    // Por defecto ninguna; solo DISPONIBLE y OCUPADA la sobrescriben
    public Set<EstadoHabitacion> transicionesPorReservaPermitidas() {
        return EnumSet.noneOf(EstadoHabitacion.class);
    }

    public boolean puedeCambiarManualmenteA(EstadoHabitacion nuevo) {
        return transicionesManualesPermitidas().contains(nuevo);
    }

    public boolean puedeCambiarPorReservaA(EstadoHabitacion nuevo) {
        return transicionesPorReservaPermitidas().contains(nuevo);
    }

    public boolean esReservable() {
        return this == DISPONIBLE;
    }

    public static EstadoHabitacion obtenerPorCodigo(Long codigo) {
        for (EstadoHabitacion e : values()) {
            if (Objects.equals(e.codigo, codigo)) return e;
        }
        throw new DatoInvalidoException("Código de estado de habitación no válido: " + codigo);
    }
}