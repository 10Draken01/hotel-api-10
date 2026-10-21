package com.equipouno.reservas.entities;

import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.utils.DateUtils;
import com.equipouno.commons.utils.ValoresNumericosUtils;
import com.equipouno.reservas.enums.EstadoReserva;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "RESERVA")
@AllArgsConstructor
@NoArgsConstructor
@Builder @Getter
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_RESERVA", nullable = false)
    private Long id;

    @Column(name = "ID_HUESPED", nullable = false)
    private Long idHuesped;

    @Column(name = "ID_HABITACION", nullable = false)
    private Long idHabitacion;

    @Column(name = "FECHA_ENTRADA", nullable = false)
    private LocalDate fechaEntrada;

    @Column(name = "FECHA_SALIDA", nullable = false)
    private LocalDate fechaSalida;

    @Column(name = "NUMERO_PERSONAS", nullable = false)
    private Integer numeroPersonas;

    @Column(name = "PRECIO_NOCHE", nullable = false)
    private BigDecimal precioNoche;

    @Column(name = "PRECIO_TOTAL", nullable = false)
    private BigDecimal precioTotal;

    @Column(name = "ESTADO", nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoReserva estadoReserva;

    @Column(name = "ESTADO_REGISTRO", nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoRegistro estadoRegistro;

    private static void validarFechas(LocalDate fechaEntrada, LocalDate fechaSalida){
        DateUtils.validarFechaRequerida(fechaEntrada, "La fecha de entrada es requerida");
        DateUtils.validarFechaRequerida(fechaSalida, "La fecha de salida es requerida");
        DateUtils.validarFechaInicioMenorFin(
                fechaEntrada,
                fechaSalida,
                "La fecha de entrada debe ser menor a la fecha de salida"
        );
    }

    private static void validarDatos(
            Long idHuesped,
            Long idHabitacion,
            Integer numeroPersonas,
            BigDecimal precioNoche
    ){
        // Validar que el id del huesped sea positivo
        ValoresNumericosUtils.validarPositivo(
                idHuesped,
                "El id del huesped debe ser un valor positivo"
        );
        // Validar que el id de la habitacion sea positivo
        ValoresNumericosUtils.validarPositivo(
                idHabitacion,
                "El id de la habitacion debe ser un valor positivo"
        );
        // Validar que el numero de personas sea positivo
        ValoresNumericosUtils.validarPositivo(
                numeroPersonas,
                "El numero de personas debe ser un valor positivo"
        );
        // Validar que el precio por noche sea positivo
        ValoresNumericosUtils.validarPositivo(
                precioNoche,
                "El precio por noche debe ser un valor positivo"
        );
    }

    private void validarNoEliminado(){
        if(estadoRegistro.equals(EstadoRegistro.ELIMINADO))
            throw new ConflictoException("La reserva con id: " + id + " no puede ser modificada porque ha sido eliminada");
    }

    public void eliminar(){
        validarNoEliminado();
        if(
                this.estadoReserva.equals(EstadoReserva.CONFIRMADA) ||
                        this.estadoReserva.equals(EstadoReserva.EN_CURSO)
        )
            throw new ConflictoException(
                    "La reserva con id: " + id + " no puede ser eliminada porque se encuentra en estado: " + this.estadoReserva.getDescripcion()
            );
        this.estadoRegistro = EstadoRegistro.ELIMINADO;
    }

    private void validarEstadoReservaNoNulo(EstadoReserva estadoReserva){
        if(estadoReserva == null)
            throw new ConflictoException("El estado de la reserva no puede ser nulo");
    }

    public void cambiarEstadoReserva(EstadoReserva estadoReserva){
        validarEstadoReservaNoNulo(estadoReserva);
        validarNoEliminado();
        if(!this.estadoReserva.puedeCambiarA(estadoReserva))
            throw new ConflictoException(
                    "El estado de la reserva no puede cambiar de " + this.estadoReserva.getDescripcion() +
                            " a " + estadoReserva.getDescripcion()
            );
        this.estadoReserva = estadoReserva;
    }

    public void actualizar(
            LocalDate fechaEntrada,
            LocalDate fechaSalida
    ){
        validarNoEliminado();  // 409
        if (!this.estadoReserva.isActualizable())                                      // FINALIZADA / CANCELADA
            throw new ConflictoException("No se puede modificar una reserva en estado " + this.estadoReserva);
        validarFechas(fechaEntrada, fechaSalida);                                      // 400

        if (this.estadoReserva == EstadoReserva.CONFIRMADA) {
            this.fechaEntrada = fechaEntrada;
            this.fechaSalida = fechaSalida;
        } else {
            actualizarPorEstadoEnCurso(fechaEntrada, fechaSalida);                     // solo salida, si no 409
        }
        recalcularPrecioTotal();
    }

    public void actualizarPorEstadoEnCurso(
            LocalDate nuevaFechaEntrada,
            LocalDate nuevaFechaSalida
    ){
        if (!this.fechaEntrada.equals(nuevaFechaEntrada)) {
            throw new ConflictoException("No se permite modificar la fecha de entrada de una reserva que ya está en curso"); // -> 409 Conflict
        }
        if (!this.fechaEntrada.isBefore(nuevaFechaSalida)) {
            throw new ConflictoException("La nueva fecha de salida debe ser posterior a la fecha de entrada original"); // -> 409 Conflict
        }
        this.fechaSalida = nuevaFechaSalida;
    }

    public void recalcularPrecioTotal() {
        long noches = DateUtils.obtenerDiasEnRangoFechas(fechaEntrada, fechaSalida);
        this.precioTotal = this.precioNoche.multiply(BigDecimal.valueOf(noches));
    }

    public static Reserva crear(
            Long idHuesped,
            Long idHabitacion,
            LocalDate fechaEntrada,
            LocalDate fechaSalida,
            Integer numeroPersonas,
            BigDecimal precioNoche
    ){
        validarDatos(
                idHuesped,
                idHabitacion,
                numeroPersonas,
                precioNoche
        );
        validarFechas(fechaEntrada, fechaSalida);
        Reserva reserva = Reserva.builder()
                .idHuesped(idHuesped)
                .idHabitacion(idHabitacion)
                .fechaEntrada(fechaEntrada)
                .fechaSalida(fechaSalida)
                .numeroPersonas(numeroPersonas)
                .precioNoche(precioNoche)
                .estadoReserva(EstadoReserva.CONFIRMADA)
                .estadoRegistro(EstadoRegistro.ACTIVO)
                .build();
        reserva.recalcularPrecioTotal();
        return reserva;
    }
}