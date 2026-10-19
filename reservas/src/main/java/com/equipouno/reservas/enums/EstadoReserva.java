package com.equipouno.reservas.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum EstadoReserva {
    CONFIRMADA(1L, "Reserva creada", true, true) {
        @Override
        public Set<EstadoReserva> puedeCambiar() {
            return Set.of(EN_CURSO, CANCELADA);
        }
    },
    EN_CURSO(2L, "Check-in realizado", true, false) {
        @Override
        public Set<EstadoReserva> puedeCambiar() {
            return Set.of(CANCELADA, FINALIZADA);
        }
    },
    FINALIZADA(3L, "Check-out realizado", false, true) {
        @Override
        public Set<EstadoReserva> puedeCambiar() {
            return Set.of();
        }
    },
    CANCELADA(4L, "Reserva cancelada", false, true) {
        @Override
        public Set<EstadoReserva> puedeCambiar() {
            return Set.of();
        }
    };

    private final Long codigo;
    private final String descripcion;

    private final boolean actualizable;
    private final boolean eliminable;

    public abstract Set<EstadoReserva> puedeCambiar();

    public boolean puedeCambiarA(EstadoReserva estadoReserva){
        return puedeCambiar().contains(estadoReserva);
    }

    public static EstadoReserva obtenerPorCodigo(Long codigo){
        for (EstadoReserva estado : EstadoReserva.values()) {
            if (estado.getCodigo().equals(codigo)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Código de estado de reserva inválido: " + codigo);
    }

}
