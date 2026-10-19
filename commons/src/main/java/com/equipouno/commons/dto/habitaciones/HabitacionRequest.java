package com.equipouno.commons.dto.habitaciones;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Datos para registrar una habitaciónn")
public record HabitacionRequest(
    @Schema(description = "Numero de habitación", example = "505")
    @NotNull(message = "El número de habitación es requerido")
    @Positive(message = "El número de habitación debe ser positivo")
    Integer numero,

    @Schema(description = "Identificador del tipo de habitacion", example = "1")
    @NotNull(message = "El id del tipo de habitación es requerido")
    @Positive(message = "El id del tipo de habitación debe ser positivo")
    Long idTipo,

    @Schema(description = "Precio de la habitación", example = "750.2")
    @NotNull(message = "El precio es requerido")
    @Positive(message = "El precio debe ser mayor a 0")
    BigDecimal precio,

    @Schema(description = "Capacidad de la habitación", example = "2")
    @NotNull(message = "La capacidad es requerida")
    @Min(value = 1, message = "La capacidad mínima debe ser 1")
    Integer capacidad
) {
}
