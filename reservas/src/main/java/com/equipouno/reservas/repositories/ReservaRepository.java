package com.equipouno.reservas.repositories;

import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.reservas.entities.Reserva;
import com.equipouno.reservas.enums.EstadoReserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByEstadoRegistro(EstadoRegistro estadoRegistro);

    Optional<Reserva> findByIdAndEstadoRegistro(Long id, EstadoRegistro estadoRegistro);

    Page<Reserva> findByEstadoReservaWithPage(
            EstadoReserva estadoReserva,
            Pageable pageable
    );

    boolean existsByIdHuespedAndEstadoReservaAndEstadoRegistro(
            Long idHuesped,
            EstadoReserva estadoReserva,
            EstadoRegistro estadoRegistro
    );

    // Para verificar si existe un traslape de fechas para una reserva en una habitacion específica,
    // excluyendo una reserva específica (por ejemplo, al actualizar una reserva existente).
    @Query("""
        select count(r) > 0
        from Reserva r
        where r.idHabitacion = :idHabitacion
          and r.fechaEntrada < :fechaSalida
          and r.fechaSalida > :fechaEntrada
          and r.estadoRegistro = 'ACTIVO'
          and r.id <> :idExcluir
        """)
    boolean existeTraslape(
            @Param("idHabitacion") Long idHabitacion,
            @Param("fechaEntrada") LocalDate fechaEntrada,
            @Param("fechaSalida") LocalDate fechaSalida,
            @Param("idExcluir") Long idExcluir   // -1L al registrar
    );
}