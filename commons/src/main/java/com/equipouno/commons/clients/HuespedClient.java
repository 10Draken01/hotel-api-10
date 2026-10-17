package com.equipouno.commons.clients;

import com.equipouno.commons.dto.huespedes.HuespedResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "huespedes")
public interface HuespedClient {

    @GetMapping("/{id}")
    ResponseEntity<HuespedResponse> obtenerHuespedPorId(@PathVariable Long id);

    @GetMapping("/id-huesped/{id}")
    ResponseEntity<HuespedResponse> obtenerHuespedPorIdSinEstado(@PathVariable Long id);
}
