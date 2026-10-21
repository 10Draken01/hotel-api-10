package com.equipouno.commons.dto.reservas;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(name = "ReservaActualizarRequest", description = "Representa la solicitud para actualizar una reserva.")
public record ReservaActualizarRequest(
        @Schema(description = "Fecha de entrada de la reserva", example = "2026-11-11")
        @NotNull(message = "La fecha de entrada es requerida")
        @FutureOrPresent(message = "La fecha de entrada debe ser hoy o en el futuro")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fechaEntrada,

        @Schema(description = "Fecha de salida de la reserva", example = "2026-11-14")
        @NotNull(message = "La fecha de salida es requerida")
        @FutureOrPresent(message = "La fecha de salida debe ser hoy o en el futuro")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fechaSalida
) {
}
