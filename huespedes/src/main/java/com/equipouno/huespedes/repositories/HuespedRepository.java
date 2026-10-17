package com.equipouno.huespedes.repositories;

import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.huespedes.entities.Huesped;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HuespedRepository extends JpaRepository<Huesped, Long> {

    Optional<Huesped> findByIdAndEstadoRegistro(Long id, EstadoRegistro estadoRegistro);

    List<Huesped> findByEstadoRegistro(EstadoRegistro estadoRegistro);
}
