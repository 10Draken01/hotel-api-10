package com.equipouno.huespedes.mapper;

import com.equipouno.commons.dto.huespedes.HuespedRequest;
import com.equipouno.commons.dto.huespedes.HuespedResponse;
import com.equipouno.commons.mapper.CommonMapper;
import com.equipouno.huespedes.entities.Huesped;
import com.equipouno.huespedes.enums.TipoDocumento;
import org.springframework.stereotype.Component;

@Component
public class HuespedMapper implements CommonMapper<HuespedRequest, HuespedResponse, Huesped> {
    @Override
    public Huesped requestAEntidad(HuespedRequest request) {
        return request == null ? null
                : Huesped.crear(
                        request.nombre(),
                        request.apellido(),
                        request.email(),
                        request.telefono(),
                        request.numeroDocumento(),
                        request.nacionalidad()
        );
    }

    public Huesped requestAEntidad(HuespedRequest request, TipoDocumento tipoDocumento) {
        Huesped huesped = requestAEntidad(request);

        huesped.asignarTipoDocumento(tipoDocumento);

        return huesped;
    }

    @Override
    public HuespedResponse entidadAResponse(Huesped huesped) {
        return huesped == null ? null
                : new HuespedResponse(
                        huesped.getId(),
                        huesped.obtenerNombreCompleto(),
                        huesped.getEmail(),
                        huesped.getTelefono(),
                        huesped.getTipoDocumento().getDescription(),
                        huesped.getNumeroDocumento(),
                        huesped.getNacionalidad()
        );
    }
}
