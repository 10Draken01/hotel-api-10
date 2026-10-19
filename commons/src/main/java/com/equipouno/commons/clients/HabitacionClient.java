package com.equipouno.commons.clients;

import com.equipouno.commons.docs.ProblemaDoc;
import com.equipouno.commons.dto.habitaciones.HabitacionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "habitaciones")
public interface HabitacionClient {
    @GetMapping("/{id}")
    ResponseEntity<HabitacionResponse> obtenerHabitacionPorId(@PathVariable Long id);

    @GetMapping("/id-habitacion/{id}")
    ResponseEntity<HabitacionResponse> obtenerHabitacionPorIdSinEstado(@PathVariable Long id);

    @GetMapping("/interno/id-habitacion-disponible/{id}")
    ResponseEntity<HabitacionResponse> obtenerHabitacionPorIdDisponible(
            @PathVariable Long id
    );

    @PutMapping("/interno/{id}/estado/{idEstado}")
    ResponseEntity<Void> actualizarEstadoHabitacionManualmenteInterno(
            @PathVariable Long id,
            @PathVariable Long idEstado
    );
}
