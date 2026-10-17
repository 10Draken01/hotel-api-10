package com.equipouno.commons.clients;

import com.equipouno.commons.dto.habitaciones.HabitacionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "habitaciones")
public interface HabitacionClient {
    @GetMapping("/{id}")
    ResponseEntity<HabitacionResponse> obtenerHabitacionPorId(@PathVariable Long id);

    @GetMapping("/id-habitacion/{id}")
    ResponseEntity<HabitacionResponse> obtenerHabitacionPorIdSinEstado(@PathVariable Long id);
}
