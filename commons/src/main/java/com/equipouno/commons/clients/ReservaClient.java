package com.equipouno.commons.clients;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "reservas")
public interface ReservaClient {

    @GetMapping("/interno/huesped-no-tiene-reserva/{idHuesped}")
    ResponseEntity<Void> huespedNoTieneReservaActiva(@PathVariable @Positive Long idHuesped);
}
