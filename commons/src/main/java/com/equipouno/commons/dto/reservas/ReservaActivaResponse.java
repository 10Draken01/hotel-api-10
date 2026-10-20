package com.equipouno.commons.dto.reservas;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReservaActivaResponse(
        @Schema(description = "Identificador de la reserva", example = "2")
        Long idReserva,

        @Schema(description = "Identificador de la habitacion reservada", example = "1")
        Long idHabitacion,

        @Schema(description = "Capacidad de personas de la habitacion reservada", example = "1")
        Integer numeroPersonas
) {
}
