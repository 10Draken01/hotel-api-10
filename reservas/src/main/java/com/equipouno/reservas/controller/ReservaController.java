package com.equipouno.reservas.controller;

import com.equipouno.commons.controller.CrudController;
import com.equipouno.commons.docs.ProblemaDoc;
import com.equipouno.commons.dto.reservas.ReservaRequest;
import com.equipouno.commons.dto.reservas.ReservaResponse;
import com.equipouno.reservas.services.ReservaService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.QueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Reservas", description = "Operaciones relacionadas con las reservas")
public class ReservaController extends CrudController<ReservaRequest, ReservaResponse, ReservaService> {

    public ReservaController(ReservaService service) {
        super(service);
    }

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
            @PathVariable @Positive(message = "El codigoEstadoReserva debe ser positivo") Long codigoEstadoReserva
    ) {
        service.actualizarEstadoReserva(idReserva, codigoEstadoReserva);
    }


}
