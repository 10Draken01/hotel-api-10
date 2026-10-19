package com.equipouno.huespedes.services;

import com.equipouno.commons.dto.huespedes.HuespedRequest;
import com.equipouno.commons.dto.huespedes.HuespedResponse;
import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import com.equipouno.huespedes.entities.Huesped;
import com.equipouno.huespedes.enums.TipoDocumento;
import com.equipouno.huespedes.mapper.HuespedMapper;
import com.equipouno.huespedes.repositories.HuespedRepository;
import com.equipouno.huespedes.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class HuespedServiceImpl implements HuespedService{
    private final HuespedRepository huespedRepository;
    private final HuespedMapper huespedMapper;

    @Override
    @Transactional(readOnly = true)
    public List<HuespedResponse> listar() {
        log.info("Listando todos los huespedes");
        return huespedRepository.findByEstadoRegistro(EstadoRegistro.ACTIVO).stream()
                .map(huespedMapper::entidadAResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HuespedResponse obtenerPorId(Long id) {
        return huespedMapper.entidadAResponse(obtenerHuespedActivo(id));
    }

    @Override
    public HuespedResponse obtenerHuespedSinEstado(Long id) {
        return huespedMapper.entidadAResponse(obtenerHuesped(id));
    }

    @Override
    public HuespedResponse registrar(HuespedRequest request) {
        log.info("Registrando nuevo huesped: {}", request);

        validarHuespedUnico(request);

        TipoDocumento tipoDocumento = TipoDocumento.obtenerPorDescripcion(
                request.tipoDocumento()
        );
        Huesped huesped = huespedMapper.requestAEntidad(
                request,
                tipoDocumento
        );

        huespedRepository.saveAndFlush(huesped);

        log.info("Huesped registrado con id: {}", huesped.getId());
        return huespedMapper.entidadAResponse(huesped);
    }

    @Override
    public HuespedResponse actualizar(HuespedRequest request, Long id) {
        log.info("Actualizando huesped con id: {}", id);
        validarHuespedCambiosUnicos(request, id);

        Huesped huesped = obtenerHuespedActivo(id);

        TipoDocumento tipoDocumento = TipoDocumento.obtenerPorDescripcion(
                request.tipoDocumento()
        );

        huesped.actualizar(
                request.nombre(),
                request.apellido(),
                request.email(),
                request.telefono(),
                request.numeroDocumento(),
                request.nacionalidad(),
                tipoDocumento
        );

        huespedRepository.saveAndFlush(huesped);

        log.info("Huesped actualizado con id: {}", huesped.getId());
        return huespedMapper.entidadAResponse(huesped);
    }

    @Override
    public void eliminar(Long id) {
        log.info("Eliminando huesped con id: {}", id);
        Huesped huesped = obtenerHuespedActivo(id);
        huesped.eliminar();
        huespedRepository.saveAndFlush(huesped);
        log.info("Huesped eliminado con id: {}", id);
    }

    private Huesped obtenerHuespedActivo(Long id){
        log.info("Buscando huesped activo con id: {}", id);
        return huespedRepository.findByIdAndEstadoRegistro(
                id, EstadoRegistro.ACTIVO
        ).orElseThrow(()->
                new RecursoNoEncontradoException(
                        "Huesped no encontrado con id: " + id + " y estado: " + EstadoRegistro.ACTIVO.getDescripcion()
                )
        );
    }

    private Huesped obtenerHuesped(Long id){
        log.info("Buscando huesped con id: {}", id);
        return ServiceUtils.obtenerEntidadOExcepcion(
                huespedRepository,
                id,
                Huesped.class
        );
    }

    private void validarHuespedUnico(HuespedRequest request){
        if (huespedRepository.existsByEmailAndEstadoRegistro(
                request.email(),
                EstadoRegistro.ACTIVO
        ))
            throw new ConflictoException(
                    "Ya existe un huesped con el email: " + request.email()
            );
        if(huespedRepository.existsByTelefonoAndEstadoRegistro(
                request.telefono(),
                EstadoRegistro.ACTIVO
        ))
            throw new ConflictoException(
                    "Ya existe un huesped con el telefono: " + request.telefono()
            );
        if(huespedRepository.existsByTipoDocumentoAndNumeroDocumentoAndEstadoRegistro(
                    TipoDocumento.obtenerPorDescripcion(request.tipoDocumento()),
                    request.numeroDocumento(),
                    EstadoRegistro.ACTIVO
        ))
            throw new ConflictoException(
                    "Ya existe un huesped con el tipo de documento: " + request.tipoDocumento() +
                            " y numero de documento: " + request.numeroDocumento()
            );
    }

    private void validarHuespedCambiosUnicos(HuespedRequest request, Long idExclude){
        if (huespedRepository.existsByEmailAndEstadoRegistroAndIdNot(
                request.email(),
                EstadoRegistro.ACTIVO,
                idExclude
        ))
            throw new ConflictoException(
                    "Ya existe un huesped con el email: " + request.email()
            );
        if(huespedRepository.existsByTelefonoAndEstadoRegistroAndIdNot(
                request.telefono(),
                EstadoRegistro.ACTIVO,
                idExclude
        ))
            throw new ConflictoException(
                    "Ya existe un huesped con el telefono: " + request.telefono()
            );
        if(huespedRepository.existsByTipoDocumentoAndNumeroDocumentoAndEstadoRegistroAndIdNot(
                TipoDocumento.obtenerPorDescripcion(request.tipoDocumento()),
                request.numeroDocumento(),
                EstadoRegistro.ACTIVO,
                idExclude
        ))
            throw new ConflictoException(
                    "Ya existe un huesped con el tipo de documento: " + request.tipoDocumento() +
                            " y numero de documento: " + request.numeroDocumento()
            );
    }
}
