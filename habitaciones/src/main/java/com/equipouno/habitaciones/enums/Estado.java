package com.equipouno.habitaciones.enums;

import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum Estado {
    DISPONIBLE(1L, "Lista para asignarse") {
        @Override
        public Set<Estado> transicionesManualesPermitidas() {
            return EnumSet.of(LIMPIEZA, MANTENIMIENTO);
        }
    },
    OCUPADA(2L, "Asignada a una reserva vigente") {
        @Override
        public Set<Estado> transicionesManualesPermitidas() {
            // Desde OCUPADA nadie puede cambiarla manualmente (retorna vacío)
            return EnumSet.noneOf(Estado.class);
        }
    },
    LIMPIEZA(3L, "En proceso de limpieza tras check-out") {
        @Override
        public Set<Estado> transicionesManualesPermitidas() {
            return EnumSet.of(DISPONIBLE, MANTENIMIENTO);
        }
    },
    MANTENIMIENTO(4L, "En reparación") {
        @Override
        public Set<Estado> transicionesManualesPermitidas() {
            return EnumSet.of(DISPONIBLE, LIMPIEZA);
        }
    };

    private final Long codigo;
    private final String descripcion;
    public abstract Set<Estado> transicionesManualesPermitidas();

    public boolean puedeCambiarManualmenteA(Estado nuevoEstado) {
        return transicionesManualesPermitidas().contains(nuevoEstado);
    }

    public static Estado obtenerPorCodigo(Long codigo) {
        for (Estado e : values()) {
            if (Objects.equals(e.codigo, codigo)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Código de estado de habitación no válido: " + codigo);
    }
}