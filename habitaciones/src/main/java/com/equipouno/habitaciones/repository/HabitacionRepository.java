package com.equipouno.habitaciones.repository;

import com.equipouno.commons.enums.EstadoHabitacion;
import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.habitaciones.entity.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {
    List<Habitacion> findByEstadoRegistro(EstadoRegistro estadoRegistro);

    Optional<Habitacion> findByIdAndEstadoRegistro(Long id, EstadoRegistro estadoRegistro);

    Optional<Habitacion> findByIdAndEstadoRegistroAndEstado(Long id, EstadoRegistro estadoRegistro, EstadoHabitacion estadoHabitacion);

    //Validamos que no haya una habitacion ACTIVA que tenga el mismo numero
    boolean existsByNumeroAndEstadoRegistro(Integer numero, EstadoRegistro estadoRegistro);

    //La misma wea que antes nomas que excluimos el propio id pa actualizar ese mismo
    boolean existsByNumeroAndEstadoRegistroAndIdNot(Integer numero, EstadoRegistro estadoRegistro, Long id);
}
