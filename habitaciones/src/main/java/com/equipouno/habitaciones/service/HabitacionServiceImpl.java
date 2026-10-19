package com.equipouno.habitaciones.service;

import com.equipouno.commons.enums.EstadoHabitacion;
import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import com.equipouno.commons.dto.habitaciones.HabitacionRequest;
import com.equipouno.commons.dto.habitaciones.HabitacionResponse;
import com.equipouno.habitaciones.entity.Habitacion;
import com.equipouno.habitaciones.enums.Tipo;
import com.equipouno.habitaciones.mapper.HabitacionMapper;
import com.equipouno.habitaciones.repository.HabitacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HabitacionServiceImpl implements HabitacionService {
    private final HabitacionMapper habitacionMapper;
    private final HabitacionRepository habitacionRepository;

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
        return habitacionMapper.entidadAResponse(obtenerHabitacionDisponiblePorId(id));
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
        //Segun las reglas de negocio si se puede actualizar los datos de habitacion aunque este ocupada
        //Nomas que ocupo la reserva vigente para ver con cuantos se declaro JAJA
        validarCambiosUnicos(request, id);

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
            throw new DatoInvalidoException("Ya existe una habitacion con ese numero");
    }

    private void validarCambiosUnicos(HabitacionRequest request, Long id){
        log.info("Validando cambio en el numero de habitacion único");
        if(habitacionRepository.existsByNumeroAndEstadoRegistroAndIdNot(request.numero(), EstadoRegistro.ACTIVO, id))
            throw new DatoInvalidoException("Ya existe una habitacion con ese numero");
    }

    private Habitacion obtenerHabitacionDisponiblePorId(Long id){
        log.info("Obteniendo habitacion disponible por id {}", id);
        return habitacionRepository.findByIdAndEstadoRegistroAndEstado(id, EstadoRegistro.ACTIVO, EstadoHabitacion.DISPONIBLE)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Habitacion disponible no encontrada con id: " + id));
    }
}
