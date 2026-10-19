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

    private static void validarFechasNoNulas(LocalDate fechaEntrada, LocalDate fechaSalida){
        // Validar que la fecha de entrada y fin no sean nulas
        DateUtils.validarFechaRequerida(
                fechaEntrada,
                "La fecha de entrada es requerida"
        );
        DateUtils.validarFechaRequerida(
                fechaSalida,
                "La fecha de salida es requerida"
        );
        // Validar que la fecha de entrada sea mayor o igual a la fecha actual
        DateUtils.validarFechaMayorActual(
                fechaEntrada,
                "La fecha de entrada debe ser mayor o igual a la fecha actual"
        );
        // Validar que la fecha de entrada sea menor a la fecha de salida
        DateUtils.validarFechaInicioMenorFin(
                fechaEntrada,
                fechaSalida,
                "La fecha de entrada debe ser menor a la fecha de salida"
        );
    }
    private static void validarDatos(
            Long idHuesped,
            Long idHabitacion,
            LocalDate fechaEntrada,
            LocalDate fechaSalida,
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
        // Validar que la fecha de entrada y fin no sean nulas
        DateUtils.validarFechaRequerida(
                fechaEntrada,
                "La fecha de entrada es requerida"
        );
        DateUtils.validarFechaRequerida(
                fechaSalida,
                "La fecha de salida es requerida"
        );
        // Validar que la fecha de entrada sea mayor o igual a la fecha actual
        DateUtils.validarFechaMayorActual(
                fechaEntrada,
                "La fecha de entrada debe ser mayor o igual a la fecha actual"
        );
        // Validar que la fecha de entrada sea menor a la fecha de salida
        DateUtils.validarFechaInicioMenorFin(
                fechaEntrada,
                fechaSalida,
                "La fecha de entrada debe ser menor a la fecha de salida"
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

    public void asignarEstadoReserva(EstadoReserva estadoReserva){
        validarEstadoReservaNoNulo(estadoReserva);
        this.estadoReserva = estadoReserva;
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
        validarNoEliminado();
        validarFechasNoNulas(fechaEntrada, fechaSalida);
        //Comento el metodo, no se elimina por si hay que validarlo en otro moemtno
        //validarNoCambioRestoDeDatos(idHuesped, idHabitacion, numeroPersonas, precioNoche, precioTotal);
        switch (this.estadoReserva) {
            case CONFIRMADA -> {
                actualizarPorEstadoConfirmada(fechaEntrada, fechaSalida);
                recalcularPrecioTotal();
            }
            case EN_CURSO -> {
                actualizarPorEstadoEnCurso(fechaEntrada, fechaSalida);
                recalcularPrecioTotal();
            }
            default ->
                throw new ConflictoException("No se puede modificar una reserva en estado " + this.estadoReserva); // -> 409 Conflict
        }
    }

    public void actualizarPorEstadoConfirmada(
            LocalDate nuevaFechaEntrada,
            LocalDate nuevaFechaSalida
    ){
        if (!nuevaFechaEntrada.isBefore(nuevaFechaSalida)) {
            throw new ConflictoException("La fecha de entrada debe ser estrictamente anterior a la fecha de salida"); // -> 409 Conflict
        }
        this.fechaEntrada = nuevaFechaEntrada;
        this.fechaSalida = nuevaFechaSalida;
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

    private void validarNoCambioRestoDeDatos(
            Long idHuespedRecibido,
            Long idHabitacionRecibido,
            Integer numeroPersonasRecibido,
            BigDecimal precioNocheRecibido,
            BigDecimal precioTotalRecibido
    ){
        if (!this.idHuesped.equals(idHuespedRecibido))
            throw new ConflictoException("No está permitido cambiar al huésped de una reserva existente"); // -> 409 Conflict

        if (!this.idHabitacion.equals(idHabitacionRecibido))
            throw new ConflictoException("No está permitido cambiar la habitacion de una reserva existente"); // -> 409 Conflict

        if (!this.numeroPersonas.equals(numeroPersonasRecibido))
            throw new ConflictoException("No está permitido cambiar el numero de personas de una reserva existente"); // -> 409 Conflict

        if (!this.precioNoche.equals(precioNocheRecibido))
            throw new ConflictoException("No está permitido cambiar el precio de la noche de una reserva existente"); // -> 409 Conflict

        if (!this.precioTotal.equals(precioTotalRecibido))
            throw new ConflictoException("No está permitido cambiar el precio total de una reserva existente"); // -> 409 Conflict
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
                fechaEntrada,
                fechaSalida,
                numeroPersonas,
                precioNoche
        );
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