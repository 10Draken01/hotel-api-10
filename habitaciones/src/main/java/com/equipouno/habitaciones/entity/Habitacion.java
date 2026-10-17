package com.equipouno.habitaciones.entity;

import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.utils.ValoresNumericosUtils;
import com.equipouno.habitaciones.enums.Estado;
import com.equipouno.habitaciones.enums.Tipo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "NUMERO")
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO")
    private Tipo tipo;

    @Column(name = "PRECIO")
    private Double precio;

    @Column(name = "CAPACIDAD")
    private Integer capacidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO")
    private Estado estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_REGISTRO")
    private EstadoRegistro estadoRegistro;

    private static void validarDatos(Integer numero, Double precio, Integer capacidad){
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
        this.estadoRegistro = EstadoRegistro.ELIMINADO;
    }

    public void actualizarEstadoManualmente(Estado nuevoEstado){
        if(nuevoEstado == null)
            throw new DatoInvalidoException("El nuevo estado es requerido");
        if(nuevoEstado == estado)
            throw new ConflictoException("El nuevo estado debe ser diferente al actual");
        if (nuevoEstado == Estado.OCUPADA)
            throw new ConflictoException("No puede actualizar manualmente una habitacion ocupada");

        this.estado = nuevoEstado;
    }

    public void actualizar(Integer numero, Double precio, Integer capacidad){
        validarNoEliminado();
        validarDatos(numero, precio, capacidad);

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

    public static Habitacion crear(Integer numero, Double precio, Integer capacidad){
        validarDatos(numero, precio, capacidad);

        return Habitacion.builder()
                .numero(numero)
                .precio(precio)
                .capacidad(capacidad)
                .estado(Estado.DISPONIBLE)
                .estadoRegistro(EstadoRegistro.ACTIVO)
                .build();
    }
}
