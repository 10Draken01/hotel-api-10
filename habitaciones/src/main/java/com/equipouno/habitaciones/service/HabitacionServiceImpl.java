package com.equipouno.habitaciones.service;

import com.equipouno.commons.clients.ReservaClient;
import com.equipouno.commons.dto.reservas.ReservaActivaResponse;
import com.equipouno.commons.enums.EstadoHabitacion;
import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.enums.EventoReserva;
import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import com.equipouno.commons.dto.habitaciones.HabitacionRequest;
import com.equipouno.commons.dto.habitaciones.HabitacionResponse;
import com.equipouno.commons.exceptions.ServicioNoDisponibleException;
import com.equipouno.habitaciones.entity.Habitacion;
import com.equipouno.habitaciones.enums.Tipo;
import com.equipouno.habitaciones.mapper.HabitacionMapper;
import com.equipouno.habitaciones.repository.HabitacionRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HabitacionServiceImpl implements HabitacionService {
    private final HabitacionMapper habitacionMapper;
    private final HabitacionRepository habitacionRepository;

    private final ReservaClient reservaClient;

    //GET /id-habitacion/{id} - obtener habitacionResponse por id sin estado
    @Override
    public HabitacionResponse obtenerHabitacionPorIdSinEstado(Long id) {
        log.info("Buscando habitacion sin estadoRegistro con id {}", id);
        return habitacionMapper.entidadAResponse(habitacionRepository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("Habitacion sin estado no encontrada con id: " + id)
        ));
    }

    @Override
    public HabitacionResponse obtenerHabitacionPorIdDisponible(Long id) {
        log.info("Buscando habitacion disponible con id {}", id);
        Habitacion habitacion = obtenerHabitacionDisponiblePorId(id);

        if (habitacion.getEstadoHabitacion().equals(EstadoHabitacion.OCUPADA))
            throw new ConflictoException("La habitacion con id " + id + " se encuentra ocupada");
        if (habitacion.getEstadoHabitacion().equals(EstadoHabitacion.LIMPIEZA))
            throw new ConflictoException("La habitacion con id " + id + " se encuentra en limpieza");
        if(habitacion.getEstadoHabitacion().equals(EstadoHabitacion.MANTENIMIENTO))
            throw new ConflictoException("La habitacion con id " + id + " se encuentra en mantenimiento");

        return habitacionMapper.entidadAResponse(habitacion);
    }

    //PATCH /{id}/estado/{idEstado} - actualizar manualmente el estado de la habitacion
    @Override
    public void actualizarEstadoHabitacionManualmente(Long idHabitacion, Long idNuevoEstado) {
        Habitacion habitacion = obtenerHabitacionActivaPorId(idHabitacion);
        log.info("Actualizando estado de la habitacion con id {}", idHabitacion);

        // Obtengo el enum por código (si falla, lanza DatoInvalidoException -> 400)
        EstadoHabitacion nuevoEstadoHabitacion = EstadoHabitacion.obtenerPorCodigo(idNuevoEstado);
        EstadoHabitacion viejoEstadoHabitacion = habitacion.getEstadoHabitacion();

        // La entidad ejecuta y valida la regla con sus propios métodos (si falla, lanza ConflictoException -> 409)
        habitacion.actualizarEstadoManualmente(nuevoEstadoHabitacion);

        habitacionRepository.save(habitacion);

        log.info("Estado de habitacion con id {} cambio de {} a {}", idHabitacion, viejoEstadoHabitacion, nuevoEstadoHabitacion);
    }

    @Override
    @Transactional
    public void aplicarEventoReserva(Long idHabitacion, EventoReserva evento) {
        Habitacion habitacion = obtenerHabitacionActivaPorId(idHabitacion);
        EstadoHabitacion anterior = habitacion.getEstadoHabitacion();

        switch (evento) {
            case RESERVA_CREADA -> habitacion.ocuparPorReserva();
            case CHECK_OUT      -> habitacion.liberarPorCheckOut();
            case CANCELACION    -> habitacion.liberarPorCancelacion();
        }

        habitacionRepository.saveAndFlush(habitacion);
        log.info("Habitacion {} cambio de {} a {} por evento {}",
                idHabitacion, anterior, habitacion.getEstadoHabitacion(), evento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabitacionResponse> listar() {
        log.info("Listando todas las habitaciones");
        return habitacionRepository.findByEstadoRegistro(EstadoRegistro.ACTIVO).stream()
                .map(habitacionMapper::entidadAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HabitacionResponse obtenerPorId(Long id) {
        Habitacion habitacion = obtenerHabitacionActivaPorId(id);
        log.info("Habitacion con id {} obtenida exitosamente", id);
        return habitacionMapper.entidadAResponse(habitacion);
    }

    @Override
    public HabitacionResponse registrar(HabitacionRequest request) {
        log.info("Registrando nueva habitacion {}", request.numero());
        validarDatosUnicos(request);

        Habitacion habitacion = habitacionMapper.requestAEntidad(
                request, Tipo.obtenerEstadoHabitacionPorCodigo(request.idTipo()));
        habitacionRepository.saveAndFlush(habitacion);
        log.info("Habitacion numero {} guardada exitosamente", request.numero());
        return habitacionMapper.entidadAResponse(habitacion);
    }

    @Override
    public HabitacionResponse actualizar(HabitacionRequest request, Long id) {
        Habitacion habitacion = obtenerHabitacionActivaPorId(id);
        validarCambiosUnicos(request, id);
        if (habitacion.getEstadoHabitacion() == EstadoHabitacion.OCUPADA) {
            log.info("Consultando reserva activa para la habitación con id: {}", id);

            try {
                ResponseEntity<ReservaActivaResponse> response = reservaClient.obtenerReservaActivaPorHabitacion(id);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    ReservaActivaResponse reservaActiva = response.getBody();

                    // RN: La capacidad no puede ser menor que el número de personas de la reserva vigente (409 Conflict)
                    if (request.capacidad() < reservaActiva.numeroPersonas()) {
                        throw new ConflictoException(String.format(
                                "No se puede reducir la capacidad a %d personas porque la reserva activa exige un mínimo de %d personas",
                                request.capacidad(),
                                reservaActiva.numeroPersonas()
                        )); // -> 409 Conflict
                    }
                }
            } catch (FeignException e) {
                log.error("Error de comunicación con el servicio de reservas: {}", e.getMessage());
                throw new ServicioNoDisponibleException("El microservicio de reservas no está disponible"); // -> 503
            }
        }

        log.info("Actualizando habitacion con id: {}", id);

        habitacion.actualizar(
                request.numero(),
                request.precio(),
                request.capacidad(),
                Tipo.obtenerEstadoHabitacionPorCodigo(request.idTipo())
        );
        habitacionRepository.saveAndFlush(habitacion);
        log.info("Habitacion con id {} actualizada", id);
        return habitacionMapper.entidadAResponse(habitacion);
    }

    @Override
    public void eliminar(Long id) {
        Habitacion habitacion = obtenerHabitacionActivaPorId(id);
         //Validar que no este ocupada, ocupo que se termine el modulo reservas

        log.info("Eliminando habitacion con id: {}", id);

        habitacion.eliminar();
        habitacionRepository.saveAndFlush(habitacion);

        log.info("Habitacion eliminada exitosamente");
    }

    private Habitacion obtenerHabitacionActivaPorId(Long id){
        log.info("Obteniendo habitacion activa por id {}", id);
        return habitacionRepository.findByIdAndEstadoRegistro(id, EstadoRegistro.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Habitacion activa no encontrada con id: " + id));
    }

    private void validarDatosUnicos(HabitacionRequest request){
        log.info("Validando numero de habitacion unico");
        if(habitacionRepository.existsByNumeroAndEstadoRegistro(request.numero(), EstadoRegistro.ACTIVO))
            throw new ConflictoException("Ya existe una habitacion con ese numero");
    }

    private void validarCambiosUnicos(HabitacionRequest request, Long id){
        log.info("Validando cambio en el numero de habitacion único");
        if(habitacionRepository.existsByNumeroAndEstadoRegistroAndIdNot(request.numero(), EstadoRegistro.ACTIVO, id))
            throw new ConflictoException("Ya existe una habitacion con ese numero");
    }

    private Habitacion obtenerHabitacionDisponiblePorId(Long id){
        log.info("Obteniendo habitacion disponible por id {}", id);

        return habitacionRepository.findByIdAndEstadoRegistro(id, EstadoRegistro.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Habitacion no encontrada con id: " + id));
    }
}
