package com.equipouno.commons.enums;

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
    },
    OCUPADA(2L, "Asignada a una reserva vigente") {
        @Override
        public Set<EstadoHabitacion> transicionesManualesPermitidas() {
            // Desde OCUPADA nadie puede cambiarla manualmente (retorna vacío)
            return EnumSet.noneOf(EstadoHabitacion.class);
        }
    },
    LIMPIEZA(3L, "En proceso de limpieza tras check-out") {
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

    public boolean puedeCambiarManualmenteA(EstadoHabitacion nuevoEstadoHabitacion) {
        return transicionesManualesPermitidas().contains(nuevoEstadoHabitacion);
    }

    public static EstadoHabitacion obtenerPorCodigo(Long codigo) {
        for (EstadoHabitacion e : values()) {
            if (Objects.equals(e.codigo, codigo)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Código de estado de habitación no válido: " + codigo);
    }
}