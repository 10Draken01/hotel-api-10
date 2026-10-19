package com.equipouno.commons.dto.reservas;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(name = "ReservaRequest", description = "Representa la solicitud para actualizar una reserva.")
public record ReservaActualizarRequest(
        @Schema(description = "Fecha de entrada de la reserva", example = "2024-06-01")
        @NotNull(message = "La fecha de entrada es requerida")
        @FutureOrPresent(message = "La fecha de entrada debe ser hoy o en el futuro")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fechaEntrada,

        @Schema(description = "Fecha de salida de la reserva", example = "2024-06-05")
        @NotNull(message = "La fecha de salida es requerida")
        @FutureOrPresent(message = "La fecha de salida debe ser hoy o en el futuro")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fechaSalida
) {
}
