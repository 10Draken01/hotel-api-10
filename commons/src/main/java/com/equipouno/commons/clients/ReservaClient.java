package com.equipouno.commons.clients;

import com.equipouno.commons.dto.reservas.ReservaActivaResponse;
import com.equipouno.commons.dto.reservas.ReservaResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(name = "reservas")
public interface ReservaClient {

    @GetMapping("/interno/huesped-no-tiene-reserva/{idHuesped}")
    ResponseEntity<Void> huespedNoTieneReservaActiva(@PathVariable @Positive Long idHuesped);

    @GetMapping("/interno/reserva-por-habitacion/{idHabitacion}")
    ResponseEntity<ReservaActivaResponse> obtenerReservaActivaPorHabitacion(@PathVariable @Positive Long idHabitacion);
}
