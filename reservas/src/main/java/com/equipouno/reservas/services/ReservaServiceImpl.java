package com.equipouno.reservas.services;

import com.equipouno.commons.clients.HabitacionClient;
import com.equipouno.commons.clients.HuespedClient;
import com.equipouno.commons.dto.habitaciones.HabitacionResponse;
import com.equipouno.commons.dto.huespedes.HuespedResponse;
import com.equipouno.commons.dto.reservas.ReservaActualizarRequest;
import com.equipouno.commons.dto.reservas.ReservaCrearRequest;
import com.equipouno.commons.dto.reservas.ReservaResponse;
import com.equipouno.commons.enums.EstadoHabitacion;
import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.enums.EventoReserva;
import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import com.equipouno.commons.dto.reservas.ReservaActivaResponse;
import com.equipouno.reservas.entities.Reserva;
import com.equipouno.reservas.enums.EstadoReserva;
import com.equipouno.reservas.mapper.ReservaMapper;
import com.equipouno.reservas.repositories.ReservaRepository;
import com.equipouno.reservas.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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
    public ReservaActivaResponse obtenerReservaActivaPorHabitacion(Long idHabitacion) {
        return reservaRepository.findByIdHabitacionAndEstadoRegistroAndEstadoReservaIn(
                        idHabitacion,
                        EstadoRegistro.ACTIVO,
                        List.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO)
                )
                .map(reserva -> new ReservaActivaResponse(
                        reserva.getId(),
                        reserva.getIdHabitacion(),
                        reserva.getNumeroPersonas()
                ))
                .orElse(null); // Retorna null si no hay reserva activa (o se maneja con ResponseEntity en el controller)
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ReservaResponse> listarReservasAgrupadasEstadoReservaSinEstado(Long codigoEstadoReserva, Pageable pageable) {
        log.info("Listando reservas agrupadas por estado de reserva sin estado: {}", codigoEstadoReserva);

        EstadoReserva estadoReserva = EstadoReserva.obtenerPorCodigo(codigoEstadoReserva);

        return reservaRepository.findByEstadoReserva(estadoReserva, pageable)
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
    public ReservaResponse registrar(ReservaCrearRequest request) {
        log.info("Registrando nueva reserva: {}", request);

        HuespedResponse huesped = obtenerHuesped(request.idHuesped());
        HabitacionResponse habitacion = obtenerHabitacionDisponible(request.idHabitacion());

        if(request.numeroPersonas() > habitacion.capacidad()) {
            throw new ConflictoException(
                    String.format("El número de personas (%d) excede la capacidad máxima de la habitación (%d)",
                            request.numeroPersonas(), habitacion.capacidad())
            );
        }

        BigDecimal precioNoche = habitacion.precio();


        Reserva reserva = reservaMapper.reservaCrearRequestAEntidad(
                request,
                precioNoche
        );

        // 1) Primero se valida contra la BD local (constraints, etc.). Aún no hay commit.
        reservaRepository.saveAndFlush(reserva);

        // 2) Último paso con efecto externo. Si habitaciones responde 409 (no estaba DISPONIBLE),
        //    la excepción propaga y la transacción local hace rollback de la reserva.
        notificarHabitacion(reserva.getIdHabitacion(), EventoReserva.RESERVA_CREADA);

        log.info("Reserva registrada con id: {}", reserva.getId());

        return reservaMapper.entidadAResponse(
                reserva,
                huesped,
                habitacion
        );
    }

    @Override
    public ReservaResponse actualizar(ReservaActualizarRequest request, Long id) {
        log.info("Actualizando reserva con ID: {}", id);
        Reserva reserva = obtenerReservaActiva(id);

        // Ejecutar la actualización pasando los parámetros del DTO y los datos inmutables
        // para que la entidad ejecute 'validarNoCambioRestoDeDatos' y responda 409 Conflict si difieren.
        reserva.actualizar(
                request.fechaEntrada(),
                request.fechaSalida()
        );

        HuespedResponse huesped = obtenerHuesped(reserva.getIdHuesped());
        HabitacionResponse habitacion = obtenerHabitacion(reserva.getIdHabitacion());

        Reserva reservaGuardada = reservaRepository.save(reserva);
        log.info("Reserva ID: {} actualizada exitosamente. Nuevo total: {}", id, reservaGuardada.getPrecioTotal());

        return reservaMapper.entidadAResponse(
                reserva,
                huesped,
                habitacion
        );
    }

    @Override
    public void actualizarEstadoReserva(Long idReserva, Long codigoEstadoReserva) {
        log.info("Actualizando estado de la reserva con id {} a estado {}", idReserva, codigoEstadoReserva);
        Reserva reserva = obtenerReservaActiva(idReserva);
        EstadoReserva nuevoEstadoReserva = EstadoReserva.obtenerPorCodigo(codigoEstadoReserva);
        EstadoReserva estadoReservaAnterior = reserva.getEstadoReserva();

        // Valida la transición (409 si no es válida) y la aplica en memoria
        reserva.cambiarEstadoReserva(nuevoEstadoReserva);

        reservaRepository.saveAndFlush(reserva);

        // Último paso con efecto externo (si habitaciones rechaza, se hace rollback local)
        sincronizarHabitacionTrasCambioDeEstado(reserva.getIdHabitacion(), nuevoEstadoReserva);

        log.info("Estado de la reserva con id {} actualizado de {} a {}", idReserva, estadoReservaAnterior, nuevoEstadoReserva);
    }

    @Override
    public void eliminar(Long id) {
        log.info("Eliminando reserva con id {}", id);
        Reserva reserva = obtenerReservaActiva(id);

        // esto valida que el estado de la reserva sea o FINALIZADA o CANCELADA, si no lo es lanza una excepcion
        reserva.eliminar();

        // No se notifica a habitaciones: el efecto sobre la habitación ya ocurrió en el check-out
        // o la cancelación, y la habitación pudo haber sido reservada por otra reserva desde entonces.

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

    /**
     * Reservas informa QUÉ pasó (evento); habitaciones decide QUÉ estado le corresponde.
     */
    private void notificarHabitacion(Long idHabitacion, EventoReserva evento) {
        log.info("Notificando a habitaciones el evento {} para la habitacion con id {}", evento, idHabitacion);
        habitacionClient.aplicarEventoReserva(idHabitacion, evento);
        log.info("Habitacion con id {} proceso el evento {}", idHabitacion, evento);
    }

    /**
     * Traduce el NUEVO estado de la reserva al evento que debe conocer habitaciones.
     * - CONFIRMADA: solo existe al crear (lo cubre registrar); ninguna transición llega aquí.
     * - EN_CURSO (check-in): la habitación sigue OCUPADA, no hay nada que notificar.
     * - FINALIZADA (check-out): OCUPADA -> LIMPIEZA.
     * - CANCELADA: OCUPADA -> DISPONIBLE.
     */
    private void sincronizarHabitacionTrasCambioDeEstado(Long idHabitacion, EstadoReserva nuevoEstado) {
        switch (nuevoEstado) {
            case EN_CURSO -> log.info("Check-in: la habitacion {} permanece OCUPADA, sin notificar", idHabitacion);
            case FINALIZADA -> notificarHabitacion(idHabitacion, EventoReserva.CHECK_OUT);
            case CANCELADA -> notificarHabitacion(idHabitacion, EventoReserva.CANCELACION);
            case CONFIRMADA -> throw new IllegalStateException(
                    "Una reserva no puede volver a CONFIRMADA; la transicion debio ser rechazada por la entidad");
        }
    }
}
