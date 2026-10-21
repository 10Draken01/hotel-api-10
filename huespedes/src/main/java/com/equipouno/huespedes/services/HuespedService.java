package com.equipouno.huespedes.services;

import com.equipouno.commons.dto.huespedes.HuespedRequest;
import com.equipouno.commons.dto.huespedes.HuespedResponse;
import com.equipouno.commons.services.CrudService;

public interface HuespedService extends CrudService<HuespedRequest, HuespedResponse> {
    HuespedResponse obtenerHuespedSinEstado(Long id);
}
