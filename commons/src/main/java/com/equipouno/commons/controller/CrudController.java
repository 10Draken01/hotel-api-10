package com.equipouno.commons.controller;

import com.equipouno.commons.docs.ProblemaDoc;
import com.equipouno.commons.services.CrudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@Validated
@ApiResponse(
        responseCode = "400",
        description = "Solicitud incorrecta",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(
                        implementation = ProblemaDoc.class
                )
        )
)
@ApiResponse(
        responseCode = "500",
        description = "Error interno del servidor",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(
                        implementation = ProblemaDoc.class
                )
        )
)
public class CrudController<RQ,RS,S extends CrudService<RQ, RS>> {
    protected final S service;

    @GetMapping
    @Operation
    @ApiResponse(
            responseCode = "200",
            description = "Lista de registros"
    )
    public ResponseEntity<List<RS>> listar(){return ResponseEntity.ok(service.listar());}

    @GetMapping("/{id}")
    @Operation
    @ApiResponse(
            responseCode = "200",
            description = "Buscar registro"
    )
    public ResponseEntity<RS> obtenerPorId(
            @PathVariable @Positive(message = "El id debe ser positivo")
            Long id
    ){
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    @Operation
    @ApiResponse(
            responseCode = "201",
            description = "Crear registro"
    )
    public ResponseEntity<RS> registrar(
            @Valid @RequestBody
            RQ request
    ){
        return ResponseEntity.ok(service.registrar(request));
    }

    @PutMapping("{id}")
    @Operation
    @ApiResponse(
            responseCode = "200",
            description = "Actualizar registro"
    )
    public ResponseEntity<RS> actualizar(
            @Valid @RequestBody
            RQ request,
            @PathVariable @Positive
            Long id
    ){
        return ResponseEntity.ok(service.actualizar(request, id));
    }

    @DeleteMapping("{id}")
    @Operation
    @ApiResponse(
            responseCode = "204",
            description = "Eliminar registro"
    )
    public ResponseEntity<RS> eliminar(
            @PathVariable @Positive
            Long id
    ){
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }


}
