package com.equipouno.reservas.mapper;

import com.equipouno.commons.dto.datos.DatosHabitacion;
import com.equipouno.commons.dto.datos.DatosHuesped;
import com.equipouno.commons.dto.habitaciones.HabitacionResponse;
import com.equipouno.commons.dto.huespedes.HuespedResponse;
import com.equipouno.commons.dto.reservas.ReservaRequest;
import com.equipouno.commons.dto.reservas.ReservaResponse;
import com.equipouno.commons.mapper.CommonMapper;
import com.equipouno.commons.utils.DateUtils;
import com.equipouno.reservas.entities.Reserva;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ReservaMapper implements CommonMapper<ReservaRequest, ReservaResponse, Reserva> {
    @Override
    public Reserva requestAEntidad(ReservaRequest request) {
        return request == null ? null
                : Reserva.crear(
                request.idHuesped(),
                request.idHabitacion(),
                request.fechaEntrada(),
                request.fechaSalida(),
                request.numeroPersonas(),
                null,
                null
        );
    }
    public Reserva requestAEntidad(ReservaRequest request, BigDecimal precioNoche, BigDecimal precioTotal) {
        return request == null ? null
                : Reserva.crear(
                request.idHuesped(),
                request.idHabitacion(),
                request.fechaEntrada(),
                request.fechaSalida(),
                request.numeroPersonas(),
                precioNoche,
                precioTotal
        );
    }

    @Override
    public ReservaResponse entidadAResponse(Reserva reserva) {
        return reserva == null ? null
                : new ReservaResponse(
                reserva.getId(),
                null,
                null,
                DateUtils.localDateAStringFormato_YYYY_MM_DD(reserva.getFechaEntrada()),
                DateUtils.localDateAStringFormato_YYYY_MM_DD(reserva.getFechaSalida()),
                reserva.getNumeroPersonas(),
                reserva.getPrecioNoche(),
                reserva.getPrecioTotal(),
                reserva.getEstadoReserva().getDescripcion()
        );
    }
    public ReservaResponse entidadAResponse(Reserva reserva, HuespedResponse huespedResponse, HabitacionResponse habitacionResponse) {
        return reserva == null ? null
                : new ReservaResponse(
                reserva.getId(),
                responseADatosHuesped(huespedResponse),
                responseADatosHabitacion(habitacionResponse),
                DateUtils.localDateAStringFormato_YYYY_MM_DD(reserva.getFechaEntrada()),
                DateUtils.localDateAStringFormato_YYYY_MM_DD(reserva.getFechaSalida()),
                reserva.getNumeroPersonas(),
                reserva.getPrecioNoche(),
                reserva.getPrecioTotal(),
                reserva.getEstadoReserva().getDescripcion()
        );
    }

    public DatosHuesped responseADatosHuesped(HuespedResponse response) {
        return response == null ? null
                : new DatosHuesped(
                response.nombre(),
                response.email(),
                response.telefono(),
                response.tipoDocumento(),
                response.numeroDocumento(),
                response.nacionalidad()
        );
    }

    public DatosHabitacion responseADatosHabitacion(HabitacionResponse response) {
        return response == null ? null
                : new DatosHabitacion(
                response.numero(),
                response.tipo(),
                response.precio(),
                response.capacidad(),
                response.estado()
        );
    }
}
