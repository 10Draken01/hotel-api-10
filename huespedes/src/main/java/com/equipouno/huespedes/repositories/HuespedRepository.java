package com.equipouno.huespedes.repositories;

import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.huespedes.entities.Huesped;
import com.equipouno.huespedes.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HuespedRepository extends JpaRepository<Huesped, Long> {

    Optional<Huesped> findByIdAndEstadoRegistro(Long id, EstadoRegistro estadoRegistro);

    List<Huesped> findByEstadoRegistro(EstadoRegistro estadoRegistro);

    // Para validar huesped unico
    boolean existsByEmailAndEstadoRegistro(String email, EstadoRegistro estadoRegistro);
    boolean existsByTelefonoAndEstadoRegistro(String telefono, EstadoRegistro estadoRegistro);
    boolean existsByTipoDocumentoAndNumeroDocumentoAndEstadoRegistro(TipoDocumento tipoDocumento, String numeroDocumento, EstadoRegistro estadoRegistro);

    // Para validar huesped unico excluyendo un id especifico (para actualizacion)


    boolean existsByEmailAndEstadoRegistroAndIdNot(String email, EstadoRegistro estadoRegistro, Long idExclude);
    boolean existsByTelefonoAndEstadoRegistroAndIdNot(String telefono, EstadoRegistro estadoRegistro, Long idExclude);
    boolean existsByTipoDocumentoAndNumeroDocumentoAndEstadoRegistroAndIdNot(TipoDocumento tipoDocumento, String numeroDocumento, EstadoRegistro estadoRegistro, Long idExclude);

    @Query("""
        SELECT CASE WHEN COUNT(h) = 0 THEN true ELSE false END
        FROM HUESPED h
        WHERE (
            h.email = :email OR
            h.telefono = :telefono OR
            h.tipoDocumento = :tipoDocumento
            )
        AND h.estadoRegistro = 'ACTIVO'
    """)
    boolean huespedIsUnique(
            String email,
            String telefono,
            TipoDocumento tipoDocumento,
            String numeroDocumento
    );
}
