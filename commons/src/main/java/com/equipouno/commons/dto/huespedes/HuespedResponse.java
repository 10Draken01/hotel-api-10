package com.equipouno.commons.dto.huespedes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "HuespedResponse", description = "Respuesta de la API para un huesped")
public record HuespedResponse(
        @Schema(description = "Identificador del huésped", example = "1")
        Long id,

        @Schema(description = "Nombre del huésped", example = "Juan Pérez")
        String nombre,

        @Schema(description = "Email del huésped", example = "jupe@gmail.com")
        String email,

        @Schema(description = "Teléfono del huésped", example = "123456789")
        String telefono,

        @Schema(description = "Tipo de documento del huésped", example = "INE")
        String tipoDocumento,

        @Schema(description = "Número de documento del huésped", example = "12345678")
        String numeroDocumento,

        @Schema(description = "Nacionalidad del huésped", example = "Mexicana")
        String nacionalidad
) { }