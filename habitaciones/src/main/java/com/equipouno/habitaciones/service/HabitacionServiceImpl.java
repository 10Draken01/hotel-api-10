package com.equipouno.habitaciones.service;

import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import com.equipouno.habitaciones.dto.HabitacionRequest;
import com.equipouno.habitaciones.dto.HabitacionResponse;
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
        return null;
    }

    //PATCH /{id}/estado/{idEstado} - actualizar manualmente el estado de la habitacion
    @Override
    public void actualizarEstadoHabitacionManualmente(Long idHabitacion, Long idNuevoEstado) {

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
        //
        validarCambiosUnicos(request, id);

        return null;
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
}
