package com.equipouno.commons.dto.reservas;

import com.equipouno.commons.dto.datos.DatosHabitacion;
import com.equipouno.commons.dto.datos.DatosHuesped;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "ReservaResponse", description = "Representa la respuesta de una reserva.")
public record ReservaResponse(
        @Schema(description = "Identificador único de la reserva", example = "1")
        Long id,

        @Schema(description = "Identificador del huésped asociado a la reserva", example = "1")
        DatosHuesped huesped,

        @Schema(description = "Identificador de la habitación asociada a la reserva", example = "101")
        DatosHabitacion habitacion,

        @Schema(description = "Fecha de entrada de la reserva", example = "2024-06-01")
        String fechaEntrada,

        @Schema(description = "Fecha de salida de la reserva", example = "2024-06-05")
        String fechaSalida,

        @Schema(description = "Número de personas para la reserva", example = "2")
        Integer numeroPersonas,

        @Schema(description = "Precio por noche de la reserva", example = "150.00")
        BigDecimal precioNoche,

        @Schema(description = "Precio total de la reserva", example = "600.00")
        BigDecimal precioTotal,

        @Schema(description = "Estado de la reserva", example = "CONFIRMADA")
        String estadoReserva
) { }