package com.equipouno.habitaciones.controller;

import com.equipouno.commons.controller.CrudController;
import com.equipouno.commons.docs.ProblemaDoc;
import com.equipouno.commons.dto.habitaciones.HabitacionRequest;
import com.equipouno.commons.dto.habitaciones.HabitacionResponse;
import com.equipouno.habitaciones.service.HabitacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "API habitaciones", description = "Api para control de operaciones de habitaciones")
public class HabitacionController extends CrudController<HabitacionRequest, HabitacionResponse, HabitacionService> {
    public HabitacionController(HabitacionService service) {
        super(service);
    }

    @GetMapping("/id-habitacion/{id}")
    @Operation(
            summary = "Obtener huesped por id sin importar el estado",
            description = "Obtiene una habitacion por su id, sin importar si está activo o inactivo"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Buscar registro"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Registro no encontrado",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(
                            implementation = ProblemaDoc.class
                    )
            )
    )
    ResponseEntity<HabitacionResponse> obtenerHuespedPorIdSinEstado(
            @PathVariable @Positive(message = "El id debe ser positivo")            Long id
    ){
        return ResponseEntity.ok(service.obtenerHabitacionPorIdSinEstado(id));
    }

    @PatchMapping("/{id}/estado/{idEstado}")
    @Operation(summary = "Actualizar estado de habitacion manualmente")
    @ApiResponse(responseCode = "204", description = "Estado de la habitacion actualizada")
    @ApiResponse(responseCode = "409", description = "El estaod de la habitacion no puede actualizarse por regla de negocio",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemaDoc.class)))
    ResponseEntity<Void> actualizarEstadoHabitacionManualmente(
            @PathVariable @Positive(message = "El id de la habitacion debe ser positivo") Long id,
            @PathVariable @Positive(message = "El idEstado debe ser positivo") Long idEstado
    ){
        service.actualizarEstadoHabitacionManualmente(id, idEstado);
        return ResponseEntity.noContent().build();
    }
}
