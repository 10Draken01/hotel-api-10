package com.equipouno.habitaciones.enums;

import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum Tipo {
    SENCILLA(1L, "Habitacion con 1 cama"),
    DOBLE(2L, "Habitacion grande con 2 camas o 1 cama grande"),
    SUITE (3L, "Habitacion grande y lujosa con camas King Size");

    private final Long codigo;
    private final String descripcion;

    public static Tipo obtenerEstadoHabitacionPorCodigo(Long codigo){
        for(Tipo tipo : values()){
            if(Objects.equals(tipo.codigo, codigo))
                return tipo;
        }
        throw new RecursoNoEncontradoException("Código de tipo de habitación no valido: " + codigo);
    }
}
