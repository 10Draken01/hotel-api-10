package com.equipouno.reservas.controller;

import com.equipouno.commons.docs.ProblemaDoc;
import com.equipouno.commons.dto.reservas.ReservaActualizarRequest;
import com.equipouno.commons.dto.reservas.ReservaCrearRequest;
import com.equipouno.commons.dto.reservas.ReservaResponse;
import com.equipouno.commons.dto.reservas.ReservaActivaResponse;
import com.equipouno.reservas.services.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@Validated
@RestController
@Tag(name = "Reservas", description = "Operaciones relacionadas con las reservas")
@ApiResponse(
        responseCode = "400",
        description = "Solicitud incorrecta",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemaDoc.class)
        )
)
@ApiResponse(
        responseCode = "500",
        description = "Error interno del servidor",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemaDoc.class)
        )
)
public class ReservaController {

    protected final ReservaService service;

    @GetMapping("/reportes/historico")
    @ApiResponse(
            responseCode = "200",
            description = "Histórico de reservas obtenido correctamente"
    )
    public ResponseEntity<List<ReservaResponse>> listarHistoricoReservas() {
        return ResponseEntity.ok(service.listarHistoricoReservas());
    }
    @GetMapping("/reportes/por-estado")
    @ApiResponse(
            responseCode = "200",
            description = "Reservas por estado obtenidas correctamente"
    )
    public Page<ReservaResponse> listarReportesPorEstado(
            @QueryParam("codigoEstadoReserva") Long codigoEstadoReserva,
            @PageableDefault Pageable pageable
            ){
        return service.listarReservasAgrupadasEstadoReservaSinEstado(codigoEstadoReserva, pageable);
    }

    @GetMapping("/interno/huesped-no-tiene-reserva/{idHuesped}")
    @ApiResponse(
            responseCode = "200",
            description = "Verifica si el huésped no tiene reservas activas"
    )
    public ResponseEntity<Void> huespedNoTieneReservaActiva(@PathVariable @Positive Long idHuesped) {
        service.huespedNoTieneReservaActiva(idHuesped);
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(
            responseCode = "200",
            description = "Verifica si la habitacion tiene reservas activas"
    )
    @GetMapping("/interno/reserva-por-habitacion/{idHabitacion}")
    public ResponseEntity<ReservaActivaResponse> obtenerReservaActivaPorHabitacion(
            @PathVariable @Positive Long idHabitacion
    ) {
        ReservaActivaResponse respuesta = service.obtenerReservaActivaPorHabitacion(idHabitacion);
        if (respuesta == null) {
            return ResponseEntity.noContent().build(); // HTTP 204 si no tiene reserva activa
        }
        return ResponseEntity.ok(respuesta); // HTTP 200 con la reserva
    }

    @PatchMapping("/{idReserva}/estado/{idEstado}")
    @ApiResponse(
            responseCode = "200",
            description = "Estado de la reserva actualizado correctamente"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Reserva o estado no encontrado",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/problem+json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ProblemaDoc.class)
            )
    )
    public void actualizarEstadoReserva(
            @PathVariable @Positive(message = "El idReserva debe ser positivo") Long idReserva,
            @PathVariable @Positive(message = "El idEstado debe ser positivo") Long idEstado
    ) {
        service.actualizarEstadoReserva(idReserva, idEstado);
    }

    //Correccion y cambios
    @GetMapping
    @Operation(
            summary = "Listar registros",
            description = "Obtiene una lista de todos los registros disponibles."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de registros obtenida correctamente."
    )
    public ResponseEntity<List<ReservaResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener registro por ID",
            description = "Busca un registro mediante su identificador único."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Registro encontrado correctamente."
    )
    @ApiResponse(
            responseCode = "404",
            description = "No se encontró el registro solicitado."
    )
    public ResponseEntity<ReservaResponse> obtenerPorId(
            @PathVariable
            @Positive(message = "El id debe ser positivo")
            Long id
    ) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    @Operation(
            summary = "Registrar registro",
            description = "Crea un nuevo registro con los datos proporcionados."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Registro creado correctamente."
    )
    public ResponseEntity<ReservaResponse> registrar(
            @Valid @RequestBody ReservaCrearRequest request
    ) {
        ReservaResponse resultado = service.registrar(request);
        return ResponseEntity.status(201).body(resultado);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar registro",
            description = "Actualiza los datos de un registro existente mediante su identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Registro actualizado correctamente."
    )
    @ApiResponse(
            responseCode = "404",
            description = "No se encontró el registro que se desea actualizar."
    )
    public ResponseEntity<ReservaResponse> actualizar(
            @PathVariable
            @Positive(message = "El id debe ser positivo")
            Long id,
            @Valid @RequestBody ReservaActualizarRequest request
    ) {
        return ResponseEntity.ok(service.actualizar(request, id));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar registro",
            description = "Elimina un registro mediante su identificador único."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Registro eliminado correctamente."
    )
    @ApiResponse(
            responseCode = "404",
            description = "No se encontró el registro que se desea eliminar."
    )
    public ResponseEntity<Void> eliminar(
            @PathVariable
            @Positive(message = "El id debe ser positivo")
            Long id
    ) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
