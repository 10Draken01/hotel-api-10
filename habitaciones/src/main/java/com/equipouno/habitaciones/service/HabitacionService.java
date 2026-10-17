package com.equipouno.habitaciones.service;

import com.equipouno.commons.services.CrudService;
import com.equipouno.commons.dto.habitaciones.HabitacionRequest;
import com.equipouno.commons.dto.habitaciones.HabitacionResponse;

public interface HabitacionService extends CrudService<HabitacionRequest, HabitacionResponse> {
    /* Siguiendo las reglas de negocio tengo que implementar estos endpoints
    * Estos son aparte de los q ya tiene CrudService (listar, obtenerPorId, registrar, actualizar, eliminar)
    * que en teoria seruan un get, get/{id}, post, put, delete
    *
    * GET /id-habitacion/{id} - obtener habitacionResponse por id sin estado
    * PATCH /{id}/estado/{idEstado} - actualizar manualmente el estado de la habitacion
    * */

    HabitacionResponse obtenerHabitacionPorIdSinEstado(Long id);

    //Este tiene que ser un patch de ahuevo
    void actualizarEstadoHabitacionManualmente(Long idHabitacion, Long idNuevoEstado);
}
