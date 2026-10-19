package com.equipouno.habitaciones.mapper;


import com.equipouno.commons.mapper.CommonMapper;
import com.equipouno.commons.dto.habitaciones.HabitacionRequest;
import com.equipouno.commons.dto.habitaciones.HabitacionResponse;
import com.equipouno.habitaciones.entity.Habitacion;
import com.equipouno.habitaciones.enums.Tipo;
import org.springframework.stereotype.Component;

@Component
public class HabitacionMapper implements CommonMapper<HabitacionRequest, HabitacionResponse, Habitacion> {
    @Override
    public Habitacion requestAEntidad(HabitacionRequest request) {
        return request == null
            ? null
            : Habitacion.crear(
                request.numero(),
                request.precio(),
                request.capacidad());
    }

    public Habitacion requestAEntidad(HabitacionRequest request, Tipo tipo){
        Habitacion habitacion = requestAEntidad(request);
        habitacion.actualizarTipo(tipo);
        return habitacion;
    }

    @Override
    public HabitacionResponse entidadAResponse(Habitacion entidad) {
        return entidad == null
            ? null
            : new HabitacionResponse(
                entidad.getId(),
                entidad.getNumero(),
                entidad.getTipo().getDescripcion(),
                entidad.getPrecio(),
                entidad.getCapacidad(),
                entidad.getEstadoHabitacion().getDescripcion());
    }
}
