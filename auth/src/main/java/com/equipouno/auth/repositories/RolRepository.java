package com.equipouno.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.equipouno.auth.entities.Rol;
import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    boolean existsByNombre(String nombre);

    Optional<Rol> findByNombre(String nombre);
}
