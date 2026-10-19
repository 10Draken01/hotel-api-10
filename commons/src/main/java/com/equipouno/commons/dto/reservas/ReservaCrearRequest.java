package com.equipouno.commons.dto.reservas;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(name = "ReservaRequest", description = "Representa la solicitud para crear una reserva.")
public record ReservaCrearRequest(
        @Schema(description = "Identificador del huésped asociado a la reserva", example = "1")
        @NotNull(message = "El id del huésped es requerido")
        @Positive(message = "El id del huésped debe ser un número positivo")
        Long idHuesped,

        @Schema(description = "Identificador de la habitación asociada a la reserva", example = "101")
        @NotNull(message = "El id de la habitación es requerido")
        @Positive(message = "El id de la habitación debe ser un número positivo")
        Long idHabitacion,

        @Schema(description = "Fecha de entrada de la reserva", example = "2024-06-01")
        @NotNull(message = "La fecha de entrada es requerida")
        @FutureOrPresent(message = "La fecha de entrada debe ser hoy o en el futuro")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fechaEntrada,

        @Schema(description = "Fecha de salida de la reserva", example = "2024-06-05")
        @NotNull(message = "La fecha de salida es requerida")
        @FutureOrPresent(message = "La fecha de salida debe ser hoy o en el futuro")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fechaSalida,

        @Schema(description = "Número de personas para la reserva", example = "2")
        @NotNull(message = "El número de personas es requerido")
        @Min(value = 1, message = "El número de personas debe ser al menos 1")
        Integer numeroPersonas,

        @Schema(description = "Código de reserva único", example = "1")
        @NotNull(message = "El código de reserva es requerido")
        @Positive(message = "El código de reserva debe ser un número positivo")
        Long codigoReserva
) { }