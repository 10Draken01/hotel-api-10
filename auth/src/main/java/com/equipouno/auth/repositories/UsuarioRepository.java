package com.equipouno.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.equipouno.auth.entities.Usuario;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndUsernameNot(
            String username,
            String usernameActual
    );
}

