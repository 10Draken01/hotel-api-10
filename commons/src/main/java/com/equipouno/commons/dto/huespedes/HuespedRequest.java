package com.equipouno.commons.dto.huespedes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(name = "HuespedRequest", description = "Formato de solicitud para crear o actualizar un huésped")
public record HuespedRequest(
        @Schema(description = "Nombre del huésped", example = "Juan")
        @NotNull(message = "El nombre es requerido")
        @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
        String nombre,

        @Schema(description = "Apellido del huésped", example = "Pérez")
        @NotNull(message = "El apellido es requerido")
        @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
        String apellido,

        @Schema(description = "Email del huésped", example = "jupe@gmail.com")
        @NotBlank(message = "El email es requerido")
        @Size (max = 150, message = "El email debe tener como máximo 150 caracteres")
        @Email(message = "El email debe ser válido")
        String email,

        @Schema(description = "Teléfono del huésped", example = "5512345678")
        @NotNull(message = "El teléfono es requerido")
        @Pattern(regexp = "^\\s*\\d{10}\\s*$", message = "El teléfono debe tener exactamente 10 dígitos")
        String telefono,

        @Schema(description = "Tipo de documento del huésped", example = "INE")
        @NotNull(message = "El tipo de documento es requerido")
        @Size(max = 50, message = "El tipo de documento debe tener como máximo 50 caracteres")
        String tipoDocumento,

        @Schema(description = "Número de documento del huésped", example = "12345678")
        @NotNull(message = "El número de documento es requerido")
        @Size(min = 8, max = 20, message = "El número de documento debe tener entre 8 y 20 caracteres")
        String numeroDocumento,

        @Schema(description = "Nacionalidad del huésped", example = "Mexicana")
        @NotNull(message = "La nacionalidad es requerida")
        @Size(min = 2, max = 50, message = "La nacionalidad debe tener entre 2 y 50 caracteres")
        String nacionalidad
) { }