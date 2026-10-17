package com.equipouno.commons.dto.habitaciones;

import io.swagger.v3.oas.annotations.media.Schema;

public record HabitacionResponse(
    @Schema(description = "Identificador de la habitación", example = "2")
    Long id,

    @Schema(description = "Numero de habitación", example = "505")
    Integer numero,

    @Schema(description = "Tipo de habitacion", example = "1")
    String tipo,

    @Schema(description = "Precio de la habitación", example = "750.2")
    Double precio,

    @Schema(description = "Capacidad de la habitación", example = "2")
    Integer capacidad,

    @Schema(description = "Estado de la habitación", example = "OCUPADA")
    String estado
) {
}
