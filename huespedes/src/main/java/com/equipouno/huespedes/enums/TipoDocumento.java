package com.equipouno.huespedes.enums;

import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.utils.StringCustomUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TipoDocumento {
    INE( "INE"),
    LICENCIA("Licencia"),
    PASAPORTE("Pasaporte"),;

    final String description;

    public static TipoDocumento obtenerPorDescripcion(String description){
        String descripcionNormalizada = StringCustomUtils.normalizarTexto(description);
        for(TipoDocumento tipoDocumento: values()){
            if(tipoDocumento.getDescription().equalsIgnoreCase(descripcionNormalizada))
                return tipoDocumento;
        }

        throw new DatoInvalidoException("No existe un tipo de documento con descripcion: " + description);
    }
}
