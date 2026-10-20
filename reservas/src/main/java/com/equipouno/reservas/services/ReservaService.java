package com.equipouno.reservas.services;

import com.equipouno.commons.dto.reservas.ReservaActualizarRequest;
import com.equipouno.commons.dto.reservas.ReservaCrearRequest;
import com.equipouno.commons.dto.reservas.ReservaResponse;
import com.equipouno.commons.dto.reservas.ReservaActivaResponse;
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

    ReservaActivaResponse obtenerReservaActivaPorHabitacion(Long idHabitacion);

    List<ReservaResponse> listar();

    ReservaResponse obtenerPorId(Long id);

    ReservaResponse registrar(ReservaCrearRequest request);

    void eliminar(Long id);
}