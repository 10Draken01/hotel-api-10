package com.equipouno.huespedes.controller;

import com.equipouno.commons.controller.CrudController;
import com.equipouno.commons.docs.ProblemaDoc;
import com.equipouno.commons.dto.huespedes.HuespedRequest;
import com.equipouno.commons.dto.huespedes.HuespedResponse;
import com.equipouno.huespedes.services.HuespedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Huespedes", description = "Controlador de huespedes")
public class HuespedController extends CrudController<HuespedRequest, HuespedResponse, HuespedService> {
    public HuespedController(HuespedService service) {
        super(service);
    }

    @GetMapping("/id-huesped/{id}")
    @Operation(
            summary = "Obtener huesped por id sin importar el estado",
            description = "Obtiene un huesped por su id, sin importar si está activo o inactivo"
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
    ResponseEntity<HuespedResponse> obtenerHuespedPorIdSinEstado(
            @PathVariable @Positive(message = "El id debe ser positivo")
            Long id
    ){
        return ResponseEntity.ok(service.obtenerHuespedSinEstado(id));
    }
}
