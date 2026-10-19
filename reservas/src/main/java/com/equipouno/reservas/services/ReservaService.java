package com.equipouno.reservas.services;

import com.equipouno.commons.dto.reservas.ReservaActualizarRequest;
import com.equipouno.commons.dto.reservas.ReservaCrearRequest;
import com.equipouno.commons.dto.reservas.ReservaResponse;
import com.equipouno.commons.services.CrudService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReservaService{

    List<ReservaResponse> listarHistoricoReservas();

    Page<ReservaResponse> listarReservasAgrupadasEstadoReservaSinEstado(
            Long codigoEstadoReserva, Pageable pageable
    );
    
    ReservaResponse actualizar(ReservaActualizarRequest request, Long id);

    void actualizarEstadoReserva(Long idReserva, Long codigoEstadoReserva);

    void huespedNoTieneReservaActiva(Long id);

    List<ReservaResponse> listar();

    ReservaResponse obtenerPorId(Long id);

    ReservaResponse registrar(ReservaCrearRequest request);

    void eliminar(Long id);
}