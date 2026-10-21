package com.equipouno.huespedes.services;

import com.equipouno.commons.clients.ReservaClient;
import com.equipouno.commons.dto.huespedes.HuespedRequest;
import com.equipouno.commons.dto.huespedes.HuespedResponse;
import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import com.equipouno.commons.exceptions.ServicioNoDisponibleException;
import com.equipouno.huespedes.entities.Huesped;
import com.equipouno.huespedes.enums.TipoDocumento;
import com.equipouno.huespedes.mapper.HuespedMapper;
import com.equipouno.huespedes.repositories.HuespedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link HuespedServiceImpl}.
 *
 * Se usan el mapper y la entidad reales; se simulan el repositorio y el cliente Feign de Reservas.
 * Reglas cubiertas: README secciones 4.3 (Huésped), 5.1 (borrado lógico), 5.2 (unicidad entre ACTIVOS),
 * 6.2 (Huéspedes) y 9 (casos obligatorios).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HuespedServiceImpl")
class HuespedServiceImplTest {

    private static final Long ID = 1L;

    @Mock
    private HuespedRepository huespedRepository;

    @Mock
    private ReservaClient reservaClient;

    private HuespedServiceImpl service;

    @BeforeEach
    void setUp() {
        // Orden de @RequiredArgsConstructor: repository, mapper, reservaClient
        service = new HuespedServiceImpl(huespedRepository, new HuespedMapper(), reservaClient);
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static Huesped huesped(Long id, EstadoRegistro registro) {
        return Huesped.builder()
                .id(id)
                .nombre("Juan")
                .apellido("Pérez")
                .email("juan.perez@mail.com")
                .telefono("5512345678")
                .tipoDocumento(TipoDocumento.INE)
                .numeroDocumento("ABCD1234")
                .nacionalidad("Mexicana")
                .estadoRegistro(registro)
                .build();
    }

    private static Huesped huespedActivo(Long id) {
        return huesped(id, EstadoRegistro.ACTIVO);
    }

    private static HuespedRequest req(String nombre, String apellido, String email, String telefono,
                                      String tipoDocumento, String numeroDocumento, String nacionalidad) {
        return new HuespedRequest(nombre, apellido, email, telefono, tipoDocumento, numeroDocumento, nacionalidad);
    }

    private static HuespedRequest requestValido() {
        return req("Ana", "López", "ana.lopez@mail.com", "5598765432", "Pasaporte", "XYZ98765", "Colombiana");
    }

    /** Request con el email indicado y el resto de datos válidos. */
    private static HuespedRequest requestConEmail(String email) {
        return req("Ana", "López", email, "5598765432", "INE", "ABCD1234", "Mexicana");
    }

    /** Simula el repositorio de forma coherente para un registro almacenado (ver HabitacionServiceImplTest). */
    private void dadoQueExiste(Huesped h) {
        Long id = h.getId();
        lenient().when(huespedRepository.findById(id)).thenReturn(Optional.of(h));
        for (EstadoRegistro er : EstadoRegistro.values()) {
            lenient().when(huespedRepository.findByIdAndEstadoRegistro(id, er))
                    .thenReturn(h.getEstadoRegistro() == er ? Optional.of(h) : Optional.empty());
        }
    }

    private void verificarSinPersistencia() {
        verify(huespedRepository, never()).save(any(Huesped.class));
        verify(huespedRepository, never()).saveAndFlush(any(Huesped.class));
    }

    private void verificarSinBorradoFisico() {
        verify(huespedRepository, never()).delete(any(Huesped.class));
        verify(huespedRepository, never()).deleteById(any());
        verify(huespedRepository, never()).deleteAll();
    }

    // =====================================================================================
    // Listar y consultar (5.1)
    // =====================================================================================

    @Test
    @DisplayName("listar: devuelve solo huéspedes ACTIVOS")
    void listar_soloActivos() {
        when(huespedRepository.findByEstadoRegistro(EstadoRegistro.ACTIVO))
                .thenReturn(List.of(huespedActivo(1L), huespedActivo(2L)));

        List<HuespedResponse> resultado = service.listar();

        assertThat(resultado).extracting(HuespedResponse::id).containsExactly(1L, 2L);
        verify(huespedRepository).findByEstadoRegistro(EstadoRegistro.ACTIVO);
        verify(huespedRepository, never()).findAll();
    }

    @Test
    @DisplayName("listar: lista vacía si no hay huéspedes activos")
    void listar_vacio() {
        when(huespedRepository.findByEstadoRegistro(EstadoRegistro.ACTIVO)).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }

    @Test
    @DisplayName("obtenerPorId: devuelve los datos del huésped activo")
    void obtenerPorId_ok() {
        dadoQueExiste(huespedActivo(ID));

        HuespedResponse r = service.obtenerPorId(ID);

        assertThat(r.id()).isEqualTo(ID);
        assertThat(r.nombre()).isEqualTo("Juan Pérez");
        assertThat(r.email()).isEqualTo("juan.perez@mail.com");
        assertThat(r.telefono()).isEqualTo("5512345678");
        assertThat(r.tipoDocumento()).isEqualTo("INE");
        assertThat(r.numeroDocumento()).isEqualTo("ABCD1234");
        assertThat(r.nacionalidad()).isEqualTo("Mexicana");
    }

    @Test
    @DisplayName("obtenerPorId: inexistente -> 404")
    void obtenerPorId_inexistente() {
        assertThatThrownBy(() -> service.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("obtenerPorId: huésped ELIMINADO -> 404")
    void obtenerPorId_eliminado() {
        dadoQueExiste(huesped(ID, EstadoRegistro.ELIMINADO));

        assertThatThrownBy(() -> service.obtenerPorId(ID))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("obtenerHuespedSinEstado (/id-huesped/{id}): devuelve el registro aunque esté ELIMINADO")
    void sinEstado_devuelveEliminado() {
        dadoQueExiste(huesped(ID, EstadoRegistro.ELIMINADO));

        HuespedResponse r = service.obtenerHuespedSinEstado(ID);

        assertThat(r.id()).isEqualTo(ID);
        assertThat(r.nombre()).isEqualTo("Juan Pérez");
    }

    @Test
    @DisplayName("obtenerHuespedSinEstado: inexistente -> 404")
    void sinEstado_inexistente() {
        assertThatThrownBy(() -> service.obtenerHuespedSinEstado(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =====================================================================================
    // Registrar (4.3, 5.2, caso obligatorio "email con mayúsculas")
    // =====================================================================================

    @Test
    @DisplayName("registrar: guarda el huésped ACTIVO con los datos recibidos")
    void registrar_ok() {
        ArgumentCaptor<Huesped> captor = ArgumentCaptor.forClass(Huesped.class);

        HuespedResponse r = service.registrar(requestValido());

        verify(huespedRepository).saveAndFlush(captor.capture());
        Huesped guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Ana");
        assertThat(guardado.getApellido()).isEqualTo("López");
        assertThat(guardado.getEmail()).isEqualTo("ana.lopez@mail.com");
        assertThat(guardado.getTelefono()).isEqualTo("5598765432");
        assertThat(guardado.getTipoDocumento()).isEqualTo(TipoDocumento.PASAPORTE);
        assertThat(guardado.getNumeroDocumento()).isEqualTo("XYZ98765");
        assertThat(guardado.getNacionalidad()).isEqualTo("Colombiana");
        assertThat(guardado.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);

        assertThat(r.nombre()).isEqualTo("Ana López");
        assertThat(r.tipoDocumento()).isEqualTo("Pasaporte");
    }

    @Test
    @DisplayName("registrar: el email con mayúsculas y espacios se guarda en minúsculas y sin espacios")
    void registrar_emailEnMinusculas() {
        ArgumentCaptor<Huesped> captor = ArgumentCaptor.forClass(Huesped.class);

        HuespedResponse r = service.registrar(requestConEmail("  Juan.PEREZ@Mail.COM  "));

        verify(huespedRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("juan.perez@mail.com");
        assertThat(r.email()).isEqualTo("juan.perez@mail.com");
    }

    @Test
    @DisplayName("registrar: los datos de texto se guardan sin espacios sobrantes (trim)")
    void registrar_aplicaTrim() {
        ArgumentCaptor<Huesped> captor = ArgumentCaptor.forClass(Huesped.class);

        service.registrar(req("  Ana  ", "  López ", "ana@mail.com", "5598765432", "INE", "  ABCD1234  ", " Mexicana "));

        verify(huespedRepository).saveAndFlush(captor.capture());
        Huesped g = captor.getValue();
        assertThat(g.getNombre()).isEqualTo("Ana");
        assertThat(g.getApellido()).isEqualTo("López");
        assertThat(g.getNumeroDocumento()).isEqualTo("ABCD1234");
        assertThat(g.getNacionalidad()).isEqualTo("Mexicana");
    }

    @Test
    @DisplayName("registrar: email duplicado de un huésped ACTIVO -> 409 (se compara en minúsculas)")
    void registrar_emailDuplicadoActivo() {
        when(huespedRepository.existsByEmailAndEstadoRegistro("juan@mail.com", EstadoRegistro.ACTIVO)).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(requestConEmail("JUAN@Mail.com")))
                .isInstanceOf(ConflictoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("registrar: teléfono duplicado de un huésped ACTIVO -> 409")
    void registrar_telefonoDuplicadoActivo() {
        when(huespedRepository.existsByTelefonoAndEstadoRegistro("5598765432", EstadoRegistro.ACTIVO)).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(requestValido()))
                .isInstanceOf(ConflictoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("registrar: documento (tipo + número) duplicado de un ACTIVO -> 409, comparando el número sin espacios")
    void registrar_documentoDuplicadoActivo() {
        when(huespedRepository.existsByTipoDocumentoAndNumeroDocumentoAndEstadoRegistro(
                TipoDocumento.INE, "ABCD1234", EstadoRegistro.ACTIVO)).thenReturn(true);

        HuespedRequest r = req("Ana", "López", "ana@mail.com", "5598765432", "INE", "  ABCD1234 ", "Mexicana");

        assertThatThrownBy(() -> service.registrar(r))
                .isInstanceOf(ConflictoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("registrar: el mismo número de documento con OTRO tipo de documento no es duplicado")
    void registrar_mismoNumeroOtroTipo() {
        // Existe un INE con ese número, pero el nuevo huésped usa PASAPORTE.
        lenient().when(huespedRepository.existsByTipoDocumentoAndNumeroDocumentoAndEstadoRegistro(
                TipoDocumento.INE, "ABCD1234", EstadoRegistro.ACTIVO)).thenReturn(true);

        HuespedRequest r = req("Ana", "López", "ana@mail.com", "5598765432", "Pasaporte", "ABCD1234", "Mexicana");

        assertThatCode(() -> service.registrar(r)).doesNotThrowAnyException();
        verify(huespedRepository).saveAndFlush(any(Huesped.class));
    }

    @Test
    @DisplayName("registrar: un email/teléfono/documento de un huésped ELIMINADO no bloquea (solo se compara con ACTIVOS)")
    void registrar_valoresDeEliminadoNoBloquean() {
        // Los repositorios responden false para ACTIVO (el único registro con esos valores está ELIMINADO).
        assertThatCode(() -> service.registrar(requestValido())).doesNotThrowAnyException();

        verify(huespedRepository, never()).existsByEmailAndEstadoRegistro(anyString(), eq(EstadoRegistro.ELIMINADO));
        verify(huespedRepository, never()).existsByTelefonoAndEstadoRegistro(anyString(), eq(EstadoRegistro.ELIMINADO));
        verify(huespedRepository, never())
                .existsByTipoDocumentoAndNumeroDocumentoAndEstadoRegistro(any(), anyString(), eq(EstadoRegistro.ELIMINADO));
        verify(huespedRepository).saveAndFlush(any(Huesped.class));
    }

    @Test
    @DisplayName("registrar: tipo de documento fuera de catálogo -> 400")
    void registrar_tipoDocumentoInvalido() {
        HuespedRequest r = req("Ana", "López", "ana@mail.com", "5598765432", "CURP", "ABCD1234", "Mexicana");

        assertThatThrownBy(() -> service.registrar(r))
                .isInstanceOf(DatoInvalidoException.class);

        verificarSinPersistencia();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"INE", "ine", "Licencia", "LICENCIA", "Pasaporte", "PASAPORTE"})
    @DisplayName("registrar: acepta los tres tipos de documento sin importar mayúsculas")
    void registrar_tiposDocumentoValidos(String tipo) {
        HuespedRequest r = req("Ana", "López", "ana@mail.com", "5598765432", tipo, "ABCD1234", "Mexicana");

        assertThatCode(() -> service.registrar(r)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestsConCamposInvalidos")
    @DisplayName("registrar: campos fuera de rango (4.3) -> 400 y no guarda")
    void registrar_camposInvalidos(String descripcion, HuespedRequest request) {
        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(DatoInvalidoException.class);

        verificarSinPersistencia();
    }

    static Stream<Arguments> requestsConCamposInvalidos() {
        String largo51 = "a".repeat(51);
        return Stream.of(
                Arguments.of("nombre de 1 carácter", req("J", "Pérez", "a@mail.com", "5512345678", "INE", "ABCD1234", "Mexicana")),
                Arguments.of("nombre de 51 caracteres", req(largo51, "Pérez", "a@mail.com", "5512345678", "INE", "ABCD1234", "Mexicana")),
                Arguments.of("apellido de 1 carácter", req("Juan", "P", "a@mail.com", "5512345678", "INE", "ABCD1234", "Mexicana")),
                Arguments.of("apellido de 51 caracteres", req("Juan", largo51, "a@mail.com", "5512345678", "INE", "ABCD1234", "Mexicana")),
                Arguments.of("teléfono de 9 dígitos", req("Juan", "Pérez", "a@mail.com", "551234567", "INE", "ABCD1234", "Mexicana")),
                Arguments.of("teléfono de 11 dígitos", req("Juan", "Pérez", "a@mail.com", "55123456789", "INE", "ABCD1234", "Mexicana")),
                Arguments.of("documento de 7 caracteres", req("Juan", "Pérez", "a@mail.com", "5512345678", "INE", "ABC1234", "Mexicana")),
                Arguments.of("documento de 21 caracteres", req("Juan", "Pérez", "a@mail.com", "5512345678", "INE", "A".repeat(21), "Mexicana")),
                Arguments.of("nacionalidad de 1 carácter", req("Juan", "Pérez", "a@mail.com", "5512345678", "INE", "ABCD1234", "M")),
                Arguments.of("nacionalidad de 51 caracteres", req("Juan", "Pérez", "a@mail.com", "5512345678", "INE", "ABCD1234", largo51))
        );
    }

    @Test
    @DisplayName("registrar: valores en los límites permitidos (2/50 caracteres, documento 8/20) se aceptan")
    void registrar_limitesValidos() {
        HuespedRequest minimos = req("Al", "Li", "a@b.c", "5512345678", "INE", "12345678", "Pe");
        HuespedRequest maximos = req("a".repeat(50), "b".repeat(50), "ok@mail.com", "5512345679", "INE", "9".repeat(20), "c".repeat(50));

        assertThatCode(() -> service.registrar(minimos)).doesNotThrowAnyException();
        assertThatCode(() -> service.registrar(maximos)).doesNotThrowAnyException();
    }

    // =====================================================================================
    // Actualizar - PUT (6.2: lo pueden hacer USER y ADMIN; 5.2: en PUT se excluye el propio registro)
    // =====================================================================================

    @Test
    @DisplayName("actualizar: modifica todos los datos y guarda el email en minúsculas")
    void actualizar_ok() {
        Huesped h = huespedActivo(ID);
        dadoQueExiste(h);

        HuespedResponse r = service.actualizar(
                req("  Ana ", "López", "ANA.LOPEZ@Mail.com", "5598765432", "Licencia", "XYZ98765", "Colombiana"), ID);

        assertThat(h.getNombre()).isEqualTo("Ana");
        assertThat(h.getApellido()).isEqualTo("López");
        assertThat(h.getEmail()).isEqualTo("ana.lopez@mail.com");
        assertThat(h.getTelefono()).isEqualTo("5598765432");
        assertThat(h.getTipoDocumento()).isEqualTo(TipoDocumento.LICENCIA);
        assertThat(h.getNumeroDocumento()).isEqualTo("XYZ98765");
        assertThat(h.getNacionalidad()).isEqualTo("Colombiana");
        assertThat(h.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(r.email()).isEqualTo("ana.lopez@mail.com");
        verify(huespedRepository).saveAndFlush(h);
    }

    @Test
    @DisplayName("actualizar: email de OTRO huésped ACTIVO -> 409")
    void actualizar_emailDuplicadoDeOtro() {
        dadoQueExiste(huespedActivo(ID));
        when(huespedRepository.existsByEmailAndEstadoRegistroAndIdNot("otro@mail.com", EstadoRegistro.ACTIVO, ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(requestConEmail("OTRO@mail.com"), ID))
                .isInstanceOf(ConflictoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("actualizar: teléfono de OTRO huésped ACTIVO -> 409")
    void actualizar_telefonoDuplicadoDeOtro() {
        dadoQueExiste(huespedActivo(ID));
        when(huespedRepository.existsByTelefonoAndEstadoRegistroAndIdNot("5598765432", EstadoRegistro.ACTIVO, ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(requestValido(), ID))
                .isInstanceOf(ConflictoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("actualizar: documento (tipo + número) de OTRO huésped ACTIVO -> 409")
    void actualizar_documentoDuplicadoDeOtro() {
        dadoQueExiste(huespedActivo(ID));
        when(huespedRepository.existsByTipoDocumentoAndNumeroDocumentoAndEstadoRegistroAndIdNot(
                TipoDocumento.PASAPORTE, "XYZ98765", EstadoRegistro.ACTIVO, ID)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(requestValido(), ID))
                .isInstanceOf(ConflictoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("actualizar: conservar los propios email, teléfono y documento es válido (se excluye el propio registro)")
    void actualizar_conservarPropiosValores() {
        Huesped h = huespedActivo(ID);
        dadoQueExiste(h);
        HuespedRequest sinCambiosUnicos =
                req("Juanito", "Pérez", "juan.perez@mail.com", "5512345678", "INE", "ABCD1234", "Mexicana");

        assertThatCode(() -> service.actualizar(sinCambiosUnicos, ID)).doesNotThrowAnyException();

        verify(huespedRepository).existsByEmailAndEstadoRegistroAndIdNot("juan.perez@mail.com", EstadoRegistro.ACTIVO, ID);
        verify(huespedRepository).existsByTelefonoAndEstadoRegistroAndIdNot("5512345678", EstadoRegistro.ACTIVO, ID);
        verify(huespedRepository).existsByTipoDocumentoAndNumeroDocumentoAndEstadoRegistroAndIdNot(
                TipoDocumento.INE, "ABCD1234", EstadoRegistro.ACTIVO, ID);
        // Nunca se usan las variantes SIN exclusión del propio id
        verify(huespedRepository, never()).existsByEmailAndEstadoRegistro(any(), any());
        verify(huespedRepository, never()).existsByTelefonoAndEstadoRegistro(any(), any());
        verify(huespedRepository, never())
                .existsByTipoDocumentoAndNumeroDocumentoAndEstadoRegistro(any(), any(), any());
        assertThat(h.getNombre()).isEqualTo("Juanito");
    }

    @Test
    @DisplayName("actualizar: huésped inexistente -> 404")
    void actualizar_inexistente() {
        assertThatThrownBy(() -> service.actualizar(requestValido(), 99L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("actualizar: huésped ELIMINADO -> 404")
    void actualizar_eliminado() {
        dadoQueExiste(huesped(ID, EstadoRegistro.ELIMINADO));

        assertThatThrownBy(() -> service.actualizar(requestValido(), ID))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("actualizar: tipo de documento fuera de catálogo -> 400")
    void actualizar_tipoDocumentoInvalido() {
        dadoQueExiste(huespedActivo(ID));
        HuespedRequest r = req("Ana", "López", "ana@mail.com", "5598765432", "CURP", "ABCD1234", "Mexicana");

        assertThatThrownBy(() -> service.actualizar(r, ID))
                .isInstanceOf(DatoInvalidoException.class);

        verificarSinPersistencia();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestsConCamposInvalidos")
    @DisplayName("actualizar: campos fuera de rango -> 400, no guarda y no modifica el huésped")
    void actualizar_camposInvalidos(String descripcion, HuespedRequest request) {
        Huesped h = huespedActivo(ID);
        dadoQueExiste(h);

        assertThatThrownBy(() -> service.actualizar(request, ID))
                .isInstanceOf(DatoInvalidoException.class);

        verificarSinPersistencia();
        assertThat(h.getNombre()).isEqualTo("Juan");
        assertThat(h.getEmail()).isEqualTo("juan.perez@mail.com");
        assertThat(h.getTelefono()).isEqualTo("5512345678");
    }

    // =====================================================================================
    // Eliminar (5.1, 6.2, caso obligatorio "eliminar con reserva CONFIRMADA o EN_CURSO -> 409")
    // =====================================================================================

    @Test
    @DisplayName("eliminar: sin reservas vigentes se elimina lógicamente, después de consultar a Reservas")
    void eliminar_ok() {
        Huesped h = huespedActivo(ID);
        dadoQueExiste(h);

        service.eliminar(ID);

        assertThat(h.getEstadoRegistro()).isEqualTo(EstadoRegistro.ELIMINADO);
        InOrder orden = inOrder(reservaClient, huespedRepository);
        orden.verify(reservaClient).huespedNoTieneReservaActiva(ID);
        orden.verify(huespedRepository).saveAndFlush(h);
        verificarSinBorradoFisico();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "El huesped con id 1 tiene reservas confirmadas",
            "El huesped con id 1 tiene reservas en curso"
    })
    @DisplayName("eliminar: con reserva CONFIRMADA o EN_CURSO -> 409, sigue ACTIVO")
    void eliminar_conReservaVigente(String mensaje) {
        Huesped h = huespedActivo(ID);
        dadoQueExiste(h);
        when(reservaClient.huespedNoTieneReservaActiva(ID)).thenThrow(new ConflictoException(mensaje));

        assertThatThrownBy(() -> service.eliminar(ID))
                .isInstanceOf(ConflictoException.class)
                .hasMessage(mensaje);

        assertThat(h.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
        verificarSinPersistencia();
        verificarSinBorradoFisico();
    }

    @Test
    @DisplayName("eliminar: si Reservas no está disponible -> 503 y NO se elimina (no se puede verificar la regla)")
    void eliminar_reservasNoDisponible() {
        Huesped h = huespedActivo(ID);
        dadoQueExiste(h);
        when(reservaClient.huespedNoTieneReservaActiva(ID))
                .thenThrow(new ServicioNoDisponibleException("reservas no disponible"));

        assertThatThrownBy(() -> service.eliminar(ID))
                .isInstanceOf(ServicioNoDisponibleException.class);

        assertThat(h.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
        verificarSinPersistencia();
    }

    @Test
    @DisplayName("eliminar: huésped inexistente -> 404")
    void eliminar_inexistente() {
        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("eliminar: huésped ya ELIMINADO -> 404")
    void eliminar_yaEliminado() {
        dadoQueExiste(huesped(ID, EstadoRegistro.ELIMINADO));

        assertThatThrownBy(() -> service.eliminar(ID))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }
}
