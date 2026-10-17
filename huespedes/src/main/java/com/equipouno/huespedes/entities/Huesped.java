package com.equipouno.huespedes.entities;

import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.utils.StringCustomUtils;
import com.equipouno.huespedes.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "HUESPED")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder @Getter
public class Huesped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_HUESPED", nullable = false)
    Long id;

    @Column(name = "NOMBRE", length = 50, nullable = false)
    String nombre;

    @Column(name = "APELLIDO", length = 50, nullable = false)
    String apellido;

    @Column(name = "EMAIL", length = 150, nullable = false)
    String email;

    @Column(name = "TELEFONO", length = 10, nullable = false)
    String telefono;

    @Column(name = "TIPO_DOCUMENTO", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    TipoDocumento tipoDocumento;

    @Column(name = "NUMERO_DOCUMENTO", length = 20, nullable = false)
    String numeroDocumento;

    @Column(name = "NACIONALIDAD", length = 50, nullable = false)
    String nacionalidad;

    @Column(name = "ESTADO_REGISTRO", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    EstadoRegistro estadoRegistro;

    public String obtenerNombreCompleto(){
        return nombre + " " + apellido;
    }

    private static void validarDatos(
            String nombre,
            String apellido,
            String email,
            String telefono,
            String numeroDocumento,
            String nacionalidad
    ){
        // validarTamano -> Valida tambien que no sea null ni vacio
        // Validar Nombre
        StringCustomUtils.validarTamano(
                nombre, 2, 50,
                "El nombre debe tener entre 2 y 50 caracteres"
        );

        // Validar Apellido
        StringCustomUtils.validarTamano(
                apellido, 2, 50,
                "El apellido debe tener entre 2 y 50 caracteres"
        );

        // Validar Email
        StringCustomUtils.validarTamano(
                email, 5, 150,
                "El email debe tener entre 5 y 150 caracteres"
        );

        // Validar Telefono
        StringCustomUtils.validarTamano(
                telefono, 10, 10,
                "El telefono debe tener 10 caracteres"
        );

        // Validar Numero Documento
        StringCustomUtils.validarTamano(
                numeroDocumento, 8, 20,
                "El numero de documento debe tener entre 8 y 20 caracteres"
        );

        // Validar Nacionalidad
        StringCustomUtils.validarTamano(
                nacionalidad, 2, 50,
                "La nacionalidad debe tener entre 2 y 50 caracteres"
        );
    }

    private void validarNoEliminado(){
        if(estadoRegistro.equals(EstadoRegistro.ELIMINADO))
            throw new ConflictoException("El huesped con id: " + id + " ha sido eliminado");
    }

    public void eliminar(){
        validarNoEliminado();
        this.estadoRegistro = EstadoRegistro.ELIMINADO;
    }

    public void asignarTipoDocumento(TipoDocumento tipoDocumento){
        validarNoEliminado();
        if(tipoDocumento == null)
            throw new DatoInvalidoException("El tipo de documento no puede ser nulo");
        this.tipoDocumento = tipoDocumento;
    }

    public void actualizar(
            String nombre,
            String apellido,
            String email,
            String telefono,
            String numeroDocumento,
            String nacionalidad,
            TipoDocumento tipoDocumento
    ){
        asignarTipoDocumento(tipoDocumento);
        validarDatos(
                nombre.trim(),
                apellido.trim(),
                email.trim().toLowerCase(),
                telefono.trim(),
                numeroDocumento.trim(),
                nacionalidad.trim()
        );

        this.nombre = nombre.trim();
        this.apellido = apellido.trim();
        this.email = email.trim().toLowerCase();
        this.telefono = telefono.trim();
        this.numeroDocumento = numeroDocumento.trim();
        this.nacionalidad = nacionalidad.trim();
    }

    public static Huesped crear(
            String nombre,
            String apellido,
            String email,
            String telefono,
            String numeroDocumento,
            String nacionalidad
    ){
        validarDatos(
                nombre.trim(),
                apellido.trim(),
                email.trim().toLowerCase(),
                telefono.trim(),
                numeroDocumento.trim(),
                nacionalidad.trim()
        );
        return Huesped.builder()
                .nombre(nombre.trim())
                .apellido(apellido.trim())
                .email(email.trim().toLowerCase())
                .telefono(telefono.trim())
                .numeroDocumento(numeroDocumento.trim())
                .nacionalidad(nacionalidad.trim())
                .estadoRegistro(EstadoRegistro.ACTIVO)
                .build();
    }
}
