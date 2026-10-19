package com.equipouno.reservas.services;

import com.equipouno.commons.clients.HabitacionClient;
import com.equipouno.commons.clients.HuespedClient;
import com.equipouno.commons.dto.habitaciones.HabitacionResponse;
import com.equipouno.commons.dto.huespedes.HuespedResponse;
import com.equipouno.commons.dto.reservas.ReservaRequest;
import com.equipouno.commons.dto.reservas.ReservaResponse;
import com.equipouno.commons.enums.EstadoHabitacion;
import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import com.equipouno.commons.utils.DateUtils;
import com.equipouno.reservas.entities.Reserva;
import com.equipouno.reservas.enums.EstadoReserva;
import com.equipouno.reservas.mapper.ReservaMapper;
import com.equipouno.reservas.repositories.ReservaRepository;
import com.equipouno.reservas.utils.ServiceUtils;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservaServiceImpl implements ReservaService{
    private final ReservaRepository reservaRepository;
    private final ReservaMapper reservaMapper;

    private final HuespedClient huespedClient;
    private final HabitacionClient habitacionClient;

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listar() {
        log.info("Listando todas las reservas");
        return reservaRepository.findByEstadoRegistro(EstadoRegistro.ACTIVO).stream()
                .map(r ->
                        reservaMapper.entidadAResponse(
                                r,
                                obtenerHuesped(r.getIdHuesped()),
                                obtenerHabitacion(r.getIdHabitacion())
                        )
                ).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listarHistoricoReservas() {
        return reservaRepository.findAll().stream()
                .map(r ->
                        reservaMapper.entidadAResponse(
                                r,
                                obtenerHuespedSinEstado(r.getIdHuesped()),
                                obtenerHabitacionSinEstado(r.getIdHabitacion())
                        )
                ).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void huespedNoTieneReservaActiva(Long id) {
        if(reservaRepository.existsByIdHuespedAndEstadoReservaAndEstadoRegistro(
                id,
                EstadoReserva.CONFIRMADA,
                EstadoRegistro.ACTIVO
        ))
            throw new ConflictoException(
                    String.format("El huesped con id %d tiene reservas confirmadas", id)
            );
        if(reservaRepository.existsByIdHuespedAndEstadoReservaAndEstadoRegistro(
                id,
                EstadoReserva.EN_CURSO,
                EstadoRegistro.ACTIVO
        ))
            throw new ConflictoException(
                    String.format("El huesped con id %d tiene reservas en curso", id)
            );
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ReservaResponse> listarReservasAgrupadasEstadoReservaSinEstado(Long codigoEstadoReserva, Pageable pageable) {
        log.info("Listando reservas agrupadas por estado de reserva sin estado: {}", codigoEstadoReserva);

        EstadoReserva estadoReserva = EstadoReserva.obtenerPorCodigo(codigoEstadoReserva);

        return reservaRepository.findByEstadoReservaWithPage(estadoReserva, pageable)
                .map(r ->
                        reservaMapper.entidadAResponse(
                                r,
                                obtenerHuespedSinEstado(r.getIdHuesped()),
                                obtenerHabitacionSinEstado(r.getIdHabitacion())
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse obtenerPorId(Long id) {
        Reserva reserva = obtenerReservaActiva(id);
        HuespedResponse huesped = obtenerHuesped(reserva.getIdHuesped());
        HabitacionResponse habitacion = obtenerHabitacion(reserva.getIdHabitacion());
        return reservaMapper.entidadAResponse(
                reserva,
                huesped,
                habitacion
        );
    }

    @Override
    public ReservaResponse registrar(ReservaRequest request) {
        log.info("Registrando nueva reserva: {}", request);

        HuespedResponse huesped = obtenerHuesped(request.idHuesped());
        HabitacionResponse habitacion = obtenerHabitacionDisponible(request.idHabitacion());

        BigDecimal precioNoche = habitacion.precio();

        BigDecimal cantidadNoches = BigDecimal.valueOf(DateUtils.obtenerDiasEnRangoFechas(
                request.fechaEntrada(),
                request.fechaSalida()
        ));

        Reserva reserva = reservaMapper.requestAEntidad(
                request,
                precioNoche,
                cantidadNoches.multiply(precioNoche)
        );

        cambiarDisponibilidadHabitacionSegunEstadoReserva(
                reserva.getIdHabitacion(),
                reserva.getEstadoReserva()
        );

        reservaRepository.saveAndFlush(reserva);

        log.info("Reserva registrada con id: {}", reserva.getId());

        return reservaMapper.entidadAResponse(
                reserva,
                huesped,
                habitacion
        );
    }

    @Override
    public ReservaResponse actualizar(ReservaRequest request, Long id) {
        return null;
    }

    @Override
    public void actualizarEstadoReserva(Long idReserva, Long codigoEstadoReserva) {
        log.info("Actualizando estado de la reserva con id {} a estado {}", idReserva, codigoEstadoReserva);
        Reserva reserva = obtenerReservaActiva(idReserva);
        EstadoReserva nuevoEstadoReserva = EstadoReserva.obtenerPorCodigo(codigoEstadoReserva);
        EstadoReserva estadoReservaAnterior = reserva.getEstadoReserva();

        reserva.cambiarEstadoReserva(nuevoEstadoReserva);

        cambiarDisponibilidadHabitacionSegunEstadoReserva(
                reserva.getIdHabitacion(),
                nuevoEstadoReserva
        );

        reservaRepository.saveAndFlush(reserva);
        log.info("Estado de la reserva con id {} actualizado de {} a {}", idReserva, estadoReservaAnterior, nuevoEstadoReserva);
    }

    @Override
    public void eliminar(Long id) {
        log.info("Eliminando reserva con id {}", id);
        Reserva reserva = obtenerReservaActiva(id);

        // esto valida que el estado de la reserva sea o FINALIZADA o CANCELADA, si no lo es lanza una excepcion
        reserva.eliminar();

        cambiarDisponibilidadHabitacionSegunEstadoReserva(
                reserva.getIdHabitacion(),
                reserva.getEstadoReserva()
        );

        reservaRepository.saveAndFlush(reserva);
        log.info("Reserva con id {} eliminada", id);
    }

    private Reserva obtenerReservaActiva(Long id){
        log.info("Obteniendo reserva activa con id {}", id);
        return reservaRepository.findByIdAndEstadoRegistro(
                id, EstadoRegistro.ACTIVO
        ).orElseThrow(() ->
                new RecursoNoEncontradoException(
                        String.format("No se encontró la reserva con id %d", id)
                ));
    }

    private Reserva obtenerReserva(Long id){
        return ServiceUtils.obtenerEntidadOExcepcion(
                reservaRepository,
                id,
                Reserva.class
        );
    }

    private HuespedResponse obtenerHuesped(Long idHuesped){
        log.info("Obteniendo huesped con id {}", idHuesped);

        return huespedClient.obtenerHuespedPorId(idHuesped).getBody();
    }

    private HuespedResponse obtenerHuespedSinEstado(Long idHuesped){
        log.info("Obteniendo huesped sin estado con id {}", idHuesped);

        return huespedClient.obtenerHuespedPorIdSinEstado(idHuesped).getBody();
    }

    private HabitacionResponse obtenerHabitacion(Long idHabitacion){
        log.info("Obteniendo habitacion con id {}", idHabitacion);

        return habitacionClient.obtenerHabitacionPorId(idHabitacion).getBody();
    }

    private HabitacionResponse obtenerHabitacionSinEstado(Long idHabitacion){
        log.info("Obteniendo habitacion sin estado con id {}", idHabitacion);

        return habitacionClient.obtenerHabitacionPorIdSinEstado(idHabitacion).getBody();
    }

    private HabitacionResponse obtenerHabitacionDisponible(Long idHabitacion){
        log.info("Obteniendo habitacion disponible con id {}", idHabitacion);

        return habitacionClient.obtenerHabitacionPorIdDisponible(idHabitacion).getBody();
    }

    // Validar que no exista traslape de reservas para las fechas dadas, excluyendo una reserva específica si se proporciona su ID
//    private void validarFechas(Long idHabitacion, LocalDate fechaInicio, LocalDate fechaFin, Long idExcluir){
//        log.info("Validando que no exista traslape de reservas para las fechas {} - {}", fechaInicio, fechaFin);
//        if(reservaRepository.existeTraslape(
//                idHabitacion,
//                fechaInicio,
//                fechaFin,
//                idExcluir
//        )){
//            throw new RecursoNoEncontradoException(
//                    String.format("Ya existe una reserva para las fechas %s - %s", fechaInicio, fechaFin)
//            );
//        }
//    }

    private void actualizarDisponibilidadHabitacion(Long idHabitacion, EstadoHabitacion estadoHabitacion){
        log.info("Actualizando disponibilidad de la habitacion con id {} a estado {}", idHabitacion, estadoHabitacion);
        habitacionClient.actualizarEstadoHabitacionManualmenteInterno(
                idHabitacion,
                estadoHabitacion.getCodigo()
        );
        log.info("Disponibilidad de la habitacion con id {} actualizada a estado {}", idHabitacion, estadoHabitacion);
    }

    private void cambiarDisponibilidadHabitacionSegunEstadoReserva(Long idHabitacion, EstadoReserva estadoReserva){
        switch (estadoReserva){
            case CONFIRMADA, EN_CURSO ->
                actualizarDisponibilidadHabitacion(idHabitacion, EstadoHabitacion.OCUPADA);
            case FINALIZADA ->
                actualizarDisponibilidadHabitacion(idHabitacion, EstadoHabitacion.LIMPIEZA);
            case CANCELADA ->
                actualizarDisponibilidadHabitacion(idHabitacion, EstadoHabitacion.DISPONIBLE);
        }
    }
}
