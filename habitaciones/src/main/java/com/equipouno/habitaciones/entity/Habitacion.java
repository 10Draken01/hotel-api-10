package com.equipouno.habitaciones.entity;

import com.equipouno.commons.enums.EstadoHabitacion;
import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.utils.ValoresNumericosUtils;
import com.equipouno.habitaciones.enums.Tipo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "HABITACION")
@Getter @Builder
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_HABITACION")
    private Long id;

    @Column(name = "NUMERO", nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false)
    private Tipo tipo;

    @Column(name = "PRECIO", nullable = false)
    private BigDecimal precio;

    @Column(name = "CAPACIDAD", nullable = false)
    private Integer capacidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false)
    private EstadoHabitacion estadoHabitacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_REGISTRO", nullable = false)
    private EstadoRegistro estadoRegistro;

    private static void validarDatos(Integer numero, BigDecimal precio, Integer capacidad){
        ValoresNumericosUtils.validarPositivo(numero,
                "El numero de habitación es requerido y debe ser positivo");
        ValoresNumericosUtils.validarPositivo(precio,
                "El precio de la habitación es requerido y debe ser positivo");
        ValoresNumericosUtils.validarPositivo(capacidad,
                "La capacidad es requerida y debe ser mayor o igual a 1");
    }

    private void validarNoEliminado(){
        if(this.estadoRegistro == EstadoRegistro.ELIMINADO)
            throw new ConflictoException("La habitacion ya esta eliminada");
    }

    public void eliminar(){
        validarNoEliminado();
        if (this.estadoHabitacion == EstadoHabitacion.OCUPADA)
            throw new ConflictoException("No se puede eliminar una habitación OCUPADA");
        this.estadoRegistro = EstadoRegistro.ELIMINADO;
    }

    public void actualizarEstadoManualmente(EstadoHabitacion nuevo) {
        if (nuevo == null) {
            throw new DatoInvalidoException("El nuevo estado es requerido");
        }
        if (nuevo == this.estadoHabitacion) {
            throw new ConflictoException("La habitación ya se encuentra en el estado " + nuevo);
        }
        if (!this.estadoHabitacion.puedeCambiarManualmenteA(nuevo)) {
            throw new ConflictoException(String.format(
                    "No se permite cambiar manualmente de %s a %s", this.estadoHabitacion, nuevo));
        }
        this.estadoHabitacion = nuevo;
    }

    // Eventos del ciclo de vida de la reserva
    public void ocuparPorReserva()          { cambiarPorReserva(EstadoHabitacion.OCUPADA); }
    public void liberarPorCheckOut()        { cambiarPorReserva(EstadoHabitacion.LIMPIEZA); }
    public void liberarPorCancelacion()     { cambiarPorReserva(EstadoHabitacion.DISPONIBLE); }

    private void cambiarPorReserva(EstadoHabitacion nuevo) {
        if (!this.estadoHabitacion.puedeCambiarPorReservaA(nuevo)) {
            throw new ConflictoException(String.format(
                    "La reserva no puede cambiar la habitación de %s a %s", this.estadoHabitacion, nuevo));
        }
        this.estadoHabitacion = nuevo;
    }

    public void actualizar(Integer numero, BigDecimal precio, Integer capacidad, Tipo tipo){
        validarNoEliminado();
        validarDatos(numero, precio, capacidad);
        actualizarTipo(tipo);

        this.numero = numero;
        this.precio = precio;
        this.capacidad = capacidad;
    }

    public void actualizarTipo(Tipo tipo){
        validarNoEliminado();
        if(tipo == null)
            throw new DatoInvalidoException("El tipo de habitacion es necesario");

        this.tipo = tipo;
    }

    public static Habitacion crear(Integer numero, BigDecimal precio, Integer capacidad){
        validarDatos(numero, precio, capacidad);

        return Habitacion.builder()
                .numero(numero)
                .precio(precio)
                .capacidad(capacidad)
                .estadoHabitacion(EstadoHabitacion.DISPONIBLE)
                .estadoRegistro(EstadoRegistro.ACTIVO)
                .build();
    }
}
