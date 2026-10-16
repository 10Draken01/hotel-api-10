package com.equipouno.commons.clients;

import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import com.equipouno.commons.exceptions.ServicioNoDisponibleException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;

@Slf4j
public class FeingErrorDecoder implements ErrorDecoder {

    private static final String DETALLE_POR_DEFECTO = "Error en la comunicación con el servicio remoto";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String detalle = extraerDetalle(response);
        log.warn("Error en la comunicación con el servicio remoto. Método: {}, Código de estado: {}, Detalle: {}",
                methodKey, response.status(), detalle);

        return switch (response.status()){
            case 400 -> new DatoInvalidoException(detalle);
            case 404 -> new RecursoNoEncontradoException(detalle) ;
            case 409 -> new ConflictoException(detalle);
            case 502, 503, 504 -> new ServicioNoDisponibleException("El servicio remoto no está disponible. Por favor, inténtelo más tarde.");
            default -> defaultDecoder.decode(methodKey, response);
        };
    }

    private String extraerDetalle(Response response) {
        if(response.body() == null)
            return DETALLE_POR_DEFECTO;

        try(InputStream body = response.body().asInputStream()) {
            JsonNode json = MAPPER.readTree(body);
            return json.path("detail").asString(DETALLE_POR_DEFECTO);
        } catch (Exception e) {
            return DETALLE_POR_DEFECTO;
        }
    }
}