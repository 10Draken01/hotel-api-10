package com.equipouno.commons.enums;

import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.utils.StringCustomUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum EstadoRegistro {
    ACTIVO("Activo"),
    ELIMINADO("Eliminado");

    private final String descripcion;

    public static EstadoRegistro obtenerPorDescripcion(String descripcion){
        String descipcionNormalizada = StringCustomUtils.normalizarTexto(descripcion);
        for(EstadoRegistro estadoRegistro : values()){
            if(StringCustomUtils.normalizarTexto(
                    estadoRegistro.descripcion
            ).equals(descipcionNormalizada))
                return estadoRegistro;
        }

        throw new DatoInvalidoException("No existe un estado de registro con descripcion: " + descripcion);
    }

}
