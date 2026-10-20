package com.equipouno.commons.clients;

import com.equipouno.commons.docs.ProblemaDoc;
import com.equipouno.commons.dto.habitaciones.HabitacionResponse;
import com.equipouno.commons.enums.EventoReserva;
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
import org.springframework.web.bind.annotation.RequestParam;

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

    @PutMapping("/interno/{id}/eventos-reserva")
    ResponseEntity<Void> aplicarEventoReserva(
            @PathVariable Long id,
            @RequestParam EventoReserva evento
    );
}
