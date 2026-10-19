package com.equipouno.reservas.services;

import com.equipouno.commons.dto.reservas.ReservaRequest;
import com.equipouno.commons.dto.reservas.ReservaResponse;
import com.equipouno.commons.services.CrudService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReservaService extends CrudService<ReservaRequest, ReservaResponse> {

    List<ReservaResponse> listarHistoricoReservas();

    Page<ReservaResponse> listarReservasAgrupadasEstadoReservaSinEstado(
            Long codigoEstadoReserva, Pageable pageable
    );

    void actualizarEstadoReserva(Long idReserva, Long codigoEstadoReserva);

    void huespedNoTieneReservaActiva(Long id);
}
