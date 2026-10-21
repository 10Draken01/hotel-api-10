package com.equipouno.commons.dto.datos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DatosHuesped", description = "Información detallada del huésped")
public record DatosHuesped(
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