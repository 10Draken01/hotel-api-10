package com.equipouno.commons.dto.datos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "DatosHabitacion", description = "Información de la habitación")
public record   DatosHabitacion(
        @Schema(description = "Numero de habitación", example = "505")
        Integer numero,

        @Schema(description = "Tipo de habitacion", example = "1")
        String tipo,

        @Schema(description = "Precio de la habitación", example = "750.2")
        BigDecimal precio,

        @Schema(description = "Capacidad de la habitación", example = "2")
        Integer capacidad,

        @Schema(description = "Estado de la habitación", example = "OCUPADA")
        String estado
) { }