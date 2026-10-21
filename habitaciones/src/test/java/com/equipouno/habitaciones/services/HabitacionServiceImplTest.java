package com.equipouno.habitaciones.service;

import com.equipouno.commons.clients.ReservaClient;
import com.equipouno.commons.dto.habitaciones.HabitacionRequest;
import com.equipouno.commons.dto.habitaciones.HabitacionResponse;
import com.equipouno.commons.dto.reservas.ReservaActivaResponse;
import com.equipouno.commons.enums.EstadoHabitacion;
import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.enums.EventoReserva;
import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import com.equipouno.commons.exceptions.ServicioNoDisponibleException;
import com.equipouno.habitaciones.entity.Habitacion;
import com.equipouno.habitaciones.enums.Tipo;
import com.equipouno.habitaciones.mapper.HabitacionMapper;
import com.equipouno.habitaciones.repository.HabitacionRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link HabitacionServiceImpl}.
 *
 * Se usa el mapper y la entidad reales (la lógica de estados vive en la entidad / enum),
 * y se simulan únicamente el repositorio y el cliente Feign de Reservas.
 *
 * Las secciones están numeradas según el README del proyecto (sección 4, 5, 6.3 y 9).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HabitacionServiceImpl")
class HabitacionServiceImplTest {

    private static final Long ID = 1L;

    @Mock
    private HabitacionRepository habitacionRepository;

    @Mock
    private ReservaClient reservaClient;

    private HabitacionServiceImpl service;

    @BeforeEach
    void setUp() {
        // Orden de @RequiredArgsConstructor: mapper, repository, reservaClient
        service = new HabitacionServiceImpl(new HabitacionMapper(), habitacionRepository, reservaClient);
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static Habitacion habitacion(Long id, int numero, EstadoHabitacion estado) {
        return habitacion(id, numero, estado, EstadoRegistro.ACTIVO);
    }

    private static Habitacion habitacion(Long id, int numero, EstadoHabitacion estado, EstadoRegistro registro) {
        return Habitacion.builder()
                .id(id)
                .numero(numero)
                .tipo(Tipo.SENCILLA)
                .precio(new BigDecimal("500.00"))
                .capacidad(2)
                .estadoHabitacion(estado)
                .estadoRegistro(registro)
                .build();
    }

    private static HabitacionRequest request(Integer numero, Long idTipo, BigDecimal precio, Integer capacidad) {
        return new HabitacionRequest(numero, idTipo, precio, capacidad);
    }

    private static HabitacionRequest requestValido() {
        return request(101, 2L, new BigDecimal("750.50"), 3);
    }

    /**
     * Simula el repositorio de forma coherente: dado un registro almacenado, TODAS las
     * consultas por id responden como lo haría la base de datos. Así las pruebas no dependen
     * de qué método del repositorio decida usar el servicio.
     * Si el registro no se declara con este helper, el repositorio responde "no existe".
     */
    private void dadoQueExiste(Habitacion h) {
        Long id = h.getId();
        lenient().when(habitacionRepository.findById(id)).thenReturn(Optional.of(h));
        for (EstadoRegistro er : EstadoRegistro.values()) {
            lenient().when(habitacionRepository.findByIdAndEstadoRegistro(id, er))
                    .thenReturn(h.getEstadoRegistro() == er ? Optional.of(h) : Optional.empty());
            for (EstadoHabitacion eh : EstadoHabitacion.values()) {
                lenient().when(habitacionRepository.findByIdAndEstadoRegistroAndEstadoHabitacion(id, er, eh))
                        .thenReturn(h.getEstadoRegistro() == er && h.getEstadoHabitacion() == eh
                                ? Optional.of(h) : Optional.empty());
            }
        }
    }

    private void verificarSinPersistencia() {
        verify(habitacionRepository, never()).save(any(Habitacion.class));
        verify(habitacionRepository, never()).saveAndFlush(any(Habitacion.class));
    }

    private void verificarSinBorradoFisico() {
        verify(habitacionRepository, never()).delete(any(Habitacion.class));
        verify(habitacionRepository, never()).deleteById(any());
        verify(habitacionRepository, never()).deleteAll();
    }

    private static FeignException feignException(int status) {
        Request req = Request.create(Request.HttpMethod.GET, "/interno/reserva-por-habitacion/1",
                Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        Response resp = Response.builder().status(status).reason("error").request(req).headers(Map.of()).build();
        return FeignException.errorStatus("ReservaClient#obtenerReservaActivaPorHabitacion", resp);
    }

    // =====================================================================================
    // Listar y consultar (5.1 Borrado lógico)
    // =====================================================================================

    @Test
    @DisplayName("listar: devuelve solo habitaciones ACTIVAS")
    void listar_devuelveSoloActivas() {
        when(habitacionRepository.findByEstadoRegistro(EstadoRegistro.ACTIVO))
                .thenReturn(List.of(habitacion(1L, 101, EstadoHabitacion.DISPONIBLE),
                        habitacion(2L, 102, EstadoHabitacion.OCUPADA)));

        List<HabitacionResponse> resultado = service.listar();

        assertThat(resultado).extracting(HabitacionResponse::numero).containsExactly(101, 102);
        verify(habitacionRepository).findByEstadoRegistro(EstadoRegistro.ACTIVO);
        verify(habitacionRepository, never()).findAll();
    }

    @Test
    @DisplayName("listar: lista vacía cuando no hay habitaciones activas")
    void listar_sinHabitaciones() {
        when(habitacionRepository.findByEstadoRegistro(EstadoRegistro.ACTIVO)).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }

    @Test
    @DisplayName("obtenerPorId: devuelve la habitación activa con todos sus datos")
    void obtenerPorId_devuelveHabitacion() {
        dadoQueExiste(habitacion(ID, 101, EstadoHabitacion.LIMPIEZA));

        HabitacionResponse r = service.obtenerPorId(ID);

        assertThat(r.id()).isEqualTo(ID);
        assertThat(r.numero()).isEqualTo(101);
        assertThat(r.precio()).isEqualByComparingTo("500.00");
        assertThat(r.capacidad()).isEqualTo(2);
        assertThat(r.estado()).isEqualTo(EstadoHabitacion.LIMPIEZA.getDescripcion());
    }

    @Test
    @DisplayName("obtenerPorId: habitación inexistente -> 404")
    void obtenerPorId_inexistente() {
        assertThatThrownBy(() -> service.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("obtenerPorId: habitación ELIMINADA -> 404")
    void obtenerPorId_eliminada() {
        dadoQueExiste(habitacion(ID, 101, EstadoHabitacion.DISPONIBLE, EstadoRegistro.ELIMINADO));

        assertThatThrownBy(() -> service.obtenerPorId(ID))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =====================================================================================
    // GET /id-habitacion/{id}  (consumo interno, sin importar estadoRegistro)
    // =====================================================================================

    @Test
    @DisplayName("obtenerHabitacionPorIdSinEstado: devuelve la habitación aunque esté ELIMINADA")
    void sinEstado_devuelveEliminada() {
        dadoQueExiste(habitacion(ID, 101, EstadoHabitacion.LIMPIEZA, EstadoRegistro.ELIMINADO));

        HabitacionResponse r = service.obtenerHabitacionPorIdSinEstado(ID);

        assertThat(r.id()).isEqualTo(ID);
        assertThat(r.numero()).isEqualTo(101);
    }

    @Test
    @DisplayName("obtenerHabitacionPorIdSinEstado: inexistente -> 404")
    void sinEstado_inexistente() {
        assertThatThrownBy(() -> service.obtenerHabitacionPorIdSinEstado(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =====================================================================================
    // Habitación disponible para reservar (README 5.4: inexistente 404; eliminada o no DISPONIBLE 409)
    // =====================================================================================

    @Test
    @DisplayName("obtenerHabitacionPorIdDisponible: habitación DISPONIBLE y ACTIVA -> OK")
    void disponible_ok() {
        dadoQueExiste(habitacion(ID, 101, EstadoHabitacion.DISPONIBLE));

        HabitacionResponse r = service.obtenerHabitacionPorIdDisponible(ID);

        assertThat(r.id()).isEqualTo(ID);
        assertThat(r.estado()).isEqualTo(EstadoHabitacion.DISPONIBLE.getDescripcion());
    }

    @Test
    @DisplayName("obtenerHabitacionPorIdDisponible: inexistente -> 404")
    void disponible_inexistente() {
        assertThatThrownBy(() -> service.obtenerHabitacionPorIdDisponible(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @ParameterizedTest(name = "estado {0} -> 409")
    @EnumSource(value = EstadoHabitacion.class, mode = EnumSource.Mode.EXCLUDE, names = "DISPONIBLE")
    @DisplayName("obtenerHabitacionPorIdDisponible: existe pero no está DISPONIBLE (OCUPADA, LIMPIEZA, MANTENIMIENTO) -> 409")
    void disponible_noDisponible_conflicto(EstadoHabitacion estado) {
        dadoQueExiste(habitacion(ID, 101, estado));

        assertThatThrownBy(() -> service.obtenerHabitacionPorIdDisponible(ID))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("obtenerHabitacionPorIdDisponible: habitación ELIMINADA -> 404")
    void disponible_eliminada_conflicto() {
        dadoQueExiste(habitacion(ID, 101, EstadoHabitacion.DISPONIBLE, EstadoRegistro.ELIMINADO));

        assertThatThrownBy(() -> service.obtenerHabitacionPorIdDisponible(ID))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =====================================================================================
    // Registrar (4.3, 5.2)
    // =====================================================================================

    @Test
    @DisplayName("registrar: crea la habitación DISPONIBLE y ACTIVA con los datos recibidos")
    void registrar_ok() {
        ArgumentCaptor<Habitacion> captor = ArgumentCaptor.forClass(Habitacion.class);

        HabitacionResponse r = service.registrar(requestValido());

        verify(habitacionRepository).saveAndFlush(captor.capture());
        Habitacion guardada = captor.getValue();
        assertThat(guardada.getNumero()).isEqualTo(101);
        assertThat(guardada.getTipo()).isEqualTo(Tipo.DOBLE);
        assertThat(guardada.getPrecio()).isEqualByComparingTo("750.50");
        assertThat(guardada.getCapacidad()).isEqualTo(3);
        assertThat(guardada.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.DISPONIBLE);
        assertThat(guardada.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);

        assertThat(r.numero()).isEqualTo(101);
        assertThat(r.estado()).isEqualTo(EstadoHabitacion.DISPONIBLE.getDescripcion());
    }

    @Test
    @DisplayName("registrar: número duplicado de una habitación ACTIVA -> 409 y no guarda")
    void registrar_numeroDuplicadoActivo() {
        when(habitacionRepository.existsByNumeroAndEstadoRegistro(101, EstadoRegistro.ACTIVO)).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(requestValido()))
                .isInstanceOf(ConflictoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("registrar: el número de una habitación ELIMINADA no bloquea (solo se compara contra ACTIVAS)")
    void registrar_numeroDeEliminadaNoBloquea() {
        // El repositorio responde false para ACTIVO: el único registro con ese número está ELIMINADO.
        when(habitacionRepository.existsByNumeroAndEstadoRegistro(101, EstadoRegistro.ACTIVO)).thenReturn(false);

        assertThatCode(() -> service.registrar(requestValido())).doesNotThrowAnyException();

        verify(habitacionRepository, never()).existsByNumeroAndEstadoRegistro(any(), org.mockito.ArgumentMatchers.eq(EstadoRegistro.ELIMINADO));
        verify(habitacionRepository).saveAndFlush(any(Habitacion.class));
    }

    @ParameterizedTest(name = "numero={0}, precio={2}, capacidad={3}")
    @CsvSource({
            "0,   1, 500, 2",    // número no mayor a 0
            "-5,  1, 500, 2",
            "101, 1, 0,   2",    // precio no mayor a 0
            "101, 1, -10, 2",
            "101, 1, 500, 0",    // capacidad menor a 1
            "101, 1, 500, -1",
            ",    1, 500, 2",    // nulos
            "101, 1, ,    2",
            "101, 1, 500, "
    })
    @DisplayName("registrar: número, precio o capacidad inválidos -> 400 y no guarda")
    void registrar_datosInvalidos(Integer numero, Long idTipo, BigDecimal precio, Integer capacidad) {
        assertThatThrownBy(() -> service.registrar(request(numero, idTipo, precio, capacidad)))
                .isInstanceOf(DatoInvalidoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("registrar: tipo de habitación fuera de catálogo -> 404 (validación de campo) y no guarda")
    void registrar_tipoFueraDeCatalogo() {
        assertThatThrownBy(() -> service.registrar(request(101, 99L, new BigDecimal("500"), 2)))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    // =====================================================================================
    // Actualizar - PUT (4.3, 5.2, 6.3)
    // =====================================================================================

    @Test
    @DisplayName("actualizar: modifica número, tipo, precio y capacidad")
    void actualizar_ok() {
        Habitacion h = habitacion(ID, 101, EstadoHabitacion.DISPONIBLE);
        dadoQueExiste(h);

        HabitacionResponse r = service.actualizar(request(202, 3L, new BigDecimal("999.99"), 4), ID);

        assertThat(h.getNumero()).isEqualTo(202);
        assertThat(h.getTipo()).isEqualTo(Tipo.SUITE);
        assertThat(h.getPrecio()).isEqualByComparingTo("999.99");
        assertThat(h.getCapacidad()).isEqualTo(4);
        assertThat(r.numero()).isEqualTo(202);
        verify(habitacionRepository).saveAndFlush(h);
        verifyNoInteractions(reservaClient);
    }

    @ParameterizedTest(name = "estado {0} se conserva")
    @EnumSource(value = EstadoHabitacion.class, mode = EnumSource.Mode.EXCLUDE, names = "OCUPADA")
    @DisplayName("actualizar: el PUT nunca cambia el estado de la habitación")
    void actualizar_noCambiaElEstado(EstadoHabitacion estado) {
        Habitacion h = habitacion(ID, 101, estado);
        dadoQueExiste(h);

        service.actualizar(request(101, 1L, new BigDecimal("600"), 2), ID);

        assertThat(h.getEstadoHabitacion()).isEqualTo(estado);
    }

    @Test
    @DisplayName("actualizar: número ya usado por OTRA habitación ACTIVA -> 409 y no guarda")
    void actualizar_numeroDuplicadoDeOtra() {
        dadoQueExiste(habitacion(ID, 101, EstadoHabitacion.DISPONIBLE));
        when(habitacionRepository.existsByNumeroAndEstadoRegistroAndIdNot(202, EstadoRegistro.ACTIVO, ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(request(202, 1L, new BigDecimal("500"), 2), ID))
                .isInstanceOf(ConflictoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("actualizar: conservar el propio número es válido (se excluye el propio registro)")
    void actualizar_mismoNumeroPropio() {
        Habitacion h = habitacion(ID, 101, EstadoHabitacion.DISPONIBLE);
        dadoQueExiste(h);

        assertThatCode(() -> service.actualizar(request(101, 1L, new BigDecimal("550"), 2), ID))
                .doesNotThrowAnyException();

        verify(habitacionRepository).existsByNumeroAndEstadoRegistroAndIdNot(101, EstadoRegistro.ACTIVO, ID);
        verify(habitacionRepository, never()).existsByNumeroAndEstadoRegistro(any(), any());
    }

    @Test
    @DisplayName("actualizar: habitación inexistente -> 404")
    void actualizar_inexistente() {
        assertThatThrownBy(() -> service.actualizar(requestValido(), 99L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("actualizar: habitación ELIMINADA -> 404")
    void actualizar_eliminada() {
        dadoQueExiste(habitacion(ID, 101, EstadoHabitacion.DISPONIBLE, EstadoRegistro.ELIMINADO));

        assertThatThrownBy(() -> service.actualizar(requestValido(), ID))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    @ParameterizedTest(name = "precio={1}, capacidad={2}")
    @CsvSource({
            "500, 0,   2",
            "500, -3,  2",
            "500, 100, 0",
            "0,   100, 2"
    })
    @DisplayName("actualizar: precio o capacidad inválidos -> 400 y no guarda")
    void actualizar_datosInvalidos(Integer numero, BigDecimal precio, Integer capacidad) {
        Habitacion h = habitacion(ID, 101, EstadoHabitacion.DISPONIBLE);
        dadoQueExiste(h);

        assertThatThrownBy(() -> service.actualizar(request(numero, 1L, precio, capacidad), ID))
                .isInstanceOf(DatoInvalidoException.class);

        verificarSinPersistencia();
        assertThat(h.getNumero()).isEqualTo(101);
        assertThat(h.getPrecio()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("actualizar: tipo fuera de catálogo -> 404")
    void actualizar_tipoFueraDeCatalogo() {
        dadoQueExiste(habitacion(ID, 101, EstadoHabitacion.DISPONIBLE));

        assertThatThrownBy(() -> service.actualizar(request(101, 99L, new BigDecimal("500"), 2), ID))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    // ---- Capacidad vs reserva vigente (4.3 / 6.3: una OCUPADA sí puede editarse con PUT) ----

    @ParameterizedTest(name = "capacidad {0} < 3 personas -> 409")
    @ValueSource(ints = {1, 2})
    @DisplayName("actualizar OCUPADA: capacidad menor que numeroPersonas de la reserva vigente -> 409")
    void actualizar_ocupada_capacidadMenor(int capacidadNueva) {
        Habitacion h = habitacion(ID, 101, EstadoHabitacion.OCUPADA);
        dadoQueExiste(h);
        when(reservaClient.obtenerReservaActivaPorHabitacion(ID))
                .thenReturn(ResponseEntity.ok(new ReservaActivaResponse(10L, ID, 3)));

        assertThatThrownBy(() -> service.actualizar(request(101, 1L, new BigDecimal("500"), capacidadNueva), ID))
                .isInstanceOf(ConflictoException.class);

        verificarSinPersistencia();
        assertThat(h.getCapacidad()).isEqualTo(2);
    }

    @ParameterizedTest(name = "capacidad {0} >= 3 personas -> OK")
    @ValueSource(ints = {3, 4, 10})
    @DisplayName("actualizar OCUPADA: capacidad igual o mayor que numeroPersonas se permite")
    void actualizar_ocupada_capacidadSuficiente(int capacidadNueva) {
        Habitacion h = habitacion(ID, 101, EstadoHabitacion.OCUPADA);
        dadoQueExiste(h);
        when(reservaClient.obtenerReservaActivaPorHabitacion(ID))
                .thenReturn(ResponseEntity.ok(new ReservaActivaResponse(10L, ID, 3)));

        service.actualizar(request(101, 1L, new BigDecimal("500"), capacidadNueva), ID);

        assertThat(h.getCapacidad()).isEqualTo(capacidadNueva);
        assertThat(h.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.OCUPADA);
        verify(habitacionRepository).saveAndFlush(h);
    }

    @Test
    @DisplayName("actualizar OCUPADA: si Reservas no informa reserva vigente (sin cuerpo) se permite el cambio")
    void actualizar_ocupada_sinReservaVigente() {
        Habitacion h = habitacion(ID, 101, EstadoHabitacion.OCUPADA);
        dadoQueExiste(h);
        when(reservaClient.obtenerReservaActivaPorHabitacion(ID))
                .thenReturn(ResponseEntity.<ReservaActivaResponse>noContent().build());

        service.actualizar(request(101, 1L, new BigDecimal("500"), 1), ID);

        assertThat(h.getCapacidad()).isEqualTo(1);
        verify(habitacionRepository).saveAndFlush(h);
    }

    @Test
    @DisplayName("actualizar OCUPADA: Reservas no disponible (Feign) -> 503 y no guarda")
    void actualizar_ocupada_reservasCaido() {
        dadoQueExiste(habitacion(ID, 101, EstadoHabitacion.OCUPADA));
        when(reservaClient.obtenerReservaActivaPorHabitacion(ID)).thenThrow(feignException(503));

        assertThatThrownBy(() -> service.actualizar(request(101, 1L, new BigDecimal("500"), 2), ID))
                .isInstanceOf(ServicioNoDisponibleException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("actualizar OCUPADA: el decoder de Feign ya traduce 503 a ServicioNoDisponibleException y se propaga")
    void actualizar_ocupada_reservasCaido_excepcionTraducida() {
        dadoQueExiste(habitacion(ID, 101, EstadoHabitacion.OCUPADA));
        when(reservaClient.obtenerReservaActivaPorHabitacion(ID))
                .thenThrow(new ServicioNoDisponibleException("no disponible"));

        assertThatThrownBy(() -> service.actualizar(request(101, 1L, new BigDecimal("500"), 2), ID))
                .isInstanceOf(ServicioNoDisponibleException.class);

        verificarSinPersistencia();
    }

    @ParameterizedTest(name = "estado {0}: no consulta Reservas")
    @EnumSource(value = EstadoHabitacion.class, mode = EnumSource.Mode.EXCLUDE, names = "OCUPADA")
    @DisplayName("actualizar: si la habitación no está OCUPADA no se consulta el microservicio de Reservas")
    void actualizar_noOcupada_noConsultaReservas(EstadoHabitacion estado) {
        dadoQueExiste(habitacion(ID, 101, estado));

        service.actualizar(request(101, 1L, new BigDecimal("500"), 1), ID);

        verifyNoInteractions(reservaClient);
    }

    // =====================================================================================
    // PATCH /{id}/estado/{idEstado}  (4.2 matriz de cambios manuales)
    // =====================================================================================

    @ParameterizedTest(name = "{0} -> {2} (código {1})")
    @CsvSource({
            "DISPONIBLE,    3, LIMPIEZA",
            "DISPONIBLE,    4, MANTENIMIENTO",
            "LIMPIEZA,      1, DISPONIBLE",
            "LIMPIEZA,      4, MANTENIMIENTO",
            "MANTENIMIENTO, 1, DISPONIBLE",
            "MANTENIMIENTO, 3, LIMPIEZA"
    })
    @DisplayName("PATCH estado: transiciones manuales permitidas")
    void patchEstado_permitidas(EstadoHabitacion origen, Long idDestino, EstadoHabitacion esperado) {
        Habitacion h = habitacion(ID, 101, origen);
        dadoQueExiste(h);

        service.actualizarEstadoHabitacionManualmente(ID, idDestino);

        assertThat(h.getEstadoHabitacion()).isEqualTo(esperado);
        verify(habitacionRepository).save(h);
    }

    @ParameterizedTest(name = "{0} -> código {1} => 409")
    @CsvSource({
            // OCUPADA: nadie la cambia manualmente
            "OCUPADA,       1",
            "OCUPADA,       2",
            "OCUPADA,       3",
            "OCUPADA,       4",
            // Cualquier cambio manual hacia OCUPADA
            "DISPONIBLE,    2",
            "LIMPIEZA,      2",
            "MANTENIMIENTO, 2",
            // Mismo estado actual
            "DISPONIBLE,    1",
            "LIMPIEZA,      3",
            "MANTENIMIENTO, 4"
    })
    @DisplayName("PATCH estado: transiciones manuales prohibidas -> 409, el estado no cambia")
    void patchEstado_prohibidas(EstadoHabitacion origen, Long idDestino) {
        Habitacion h = habitacion(ID, 101, origen);
        dadoQueExiste(h);

        assertThatThrownBy(() -> service.actualizarEstadoHabitacionManualmente(ID, idDestino))
                .isInstanceOf(ConflictoException.class);

        assertThat(h.getEstadoHabitacion()).isEqualTo(origen);
        verificarSinPersistencia();
    }

    @ParameterizedTest(name = "idEstado {0} => 400")
    @ValueSource(longs = {0, 5, 99, -1})
    @DisplayName("PATCH estado: idEstado fuera de catálogo -> 400")
    void patchEstado_fueraDeCatalogo(long idEstado) {
        Habitacion h = habitacion(ID, 101, EstadoHabitacion.DISPONIBLE);
        dadoQueExiste(h);

        assertThatThrownBy(() -> service.actualizarEstadoHabitacionManualmente(ID, idEstado))
                .isInstanceOf(DatoInvalidoException.class);

        assertThat(h.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.DISPONIBLE);
        verificarSinPersistencia();
    }

    @Test
    @DisplayName("PATCH estado: habitación inexistente -> 404")
    void patchEstado_inexistente() {
        assertThatThrownBy(() -> service.actualizarEstadoHabitacionManualmente(99L, 1L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("PATCH estado: habitación ELIMINADA -> 404")
    void patchEstado_eliminada() {
        dadoQueExiste(habitacion(ID, 101, EstadoHabitacion.LIMPIEZA, EstadoRegistro.ELIMINADO));

        assertThatThrownBy(() -> service.actualizarEstadoHabitacionManualmente(ID, 1L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    // =====================================================================================
    // Eventos de Reservas (integridad Reserva <-> Habitación, 6.4)
    // =====================================================================================

    @Test
    @DisplayName("evento RESERVA_CREADA: DISPONIBLE -> OCUPADA")
    void evento_reservaCreada() {
        Habitacion h = habitacion(ID, 101, EstadoHabitacion.DISPONIBLE);
        dadoQueExiste(h);

        service.aplicarEventoReserva(ID, EventoReserva.RESERVA_CREADA);

        assertThat(h.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.OCUPADA);
        verify(habitacionRepository).saveAndFlush(h);
    }

    @Test
    @DisplayName("evento CHECK_OUT: OCUPADA -> LIMPIEZA")
    void evento_checkOut() {
        Habitacion h = habitacion(ID, 101, EstadoHabitacion.OCUPADA);
        dadoQueExiste(h);

        service.aplicarEventoReserva(ID, EventoReserva.CHECK_OUT);

        assertThat(h.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.LIMPIEZA);
        verify(habitacionRepository).saveAndFlush(h);
    }

    @Test
    @DisplayName("evento CANCELACION: OCUPADA -> DISPONIBLE")
    void evento_cancelacion() {
        Habitacion h = habitacion(ID, 101, EstadoHabitacion.OCUPADA);
        dadoQueExiste(h);

        service.aplicarEventoReserva(ID, EventoReserva.CANCELACION);

        assertThat(h.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.DISPONIBLE);
        verify(habitacionRepository).saveAndFlush(h);
    }

    @ParameterizedTest(name = "{0} sobre habitación {1} => 409")
    @CsvSource({
            // Solo una habitación DISPONIBLE se puede reservar
            "RESERVA_CREADA, OCUPADA",
            "RESERVA_CREADA, LIMPIEZA",
            "RESERVA_CREADA, MANTENIMIENTO",
            // Check-out y cancelación solo aplican a una habitación OCUPADA
            "CHECK_OUT,      DISPONIBLE",
            "CHECK_OUT,      LIMPIEZA",
            "CHECK_OUT,      MANTENIMIENTO",
            "CANCELACION,    DISPONIBLE",
            "CANCELACION,    LIMPIEZA",
            "CANCELACION,    MANTENIMIENTO"
    })
    @DisplayName("eventos de reserva sobre un estado de habitación inválido -> 409, sin cambios")
    void evento_estadoInvalido(EventoReserva evento, EstadoHabitacion estado) {
        Habitacion h = habitacion(ID, 101, estado);
        dadoQueExiste(h);

        assertThatThrownBy(() -> service.aplicarEventoReserva(ID, evento))
                .isInstanceOf(ConflictoException.class);

        assertThat(h.getEstadoHabitacion()).isEqualTo(estado);
        verificarSinPersistencia();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EventoReserva.class)
    @DisplayName("eventos de reserva: habitación inexistente -> 404")
    void evento_inexistente(EventoReserva evento) {
        assertThatThrownBy(() -> service.aplicarEventoReserva(99L, evento))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    // =====================================================================================
    // Eliminar (5.1 borrado lógico, 6.3)
    // =====================================================================================

    @ParameterizedTest(name = "eliminar habitación {0} => OK")
    @EnumSource(value = EstadoHabitacion.class, mode = EnumSource.Mode.EXCLUDE, names = "OCUPADA")
    @DisplayName("eliminar: DISPONIBLE, LIMPIEZA y MANTENIMIENTO se eliminan lógicamente")
    void eliminar_ok(EstadoHabitacion estado) {
        Habitacion h = habitacion(ID, 101, estado);
        dadoQueExiste(h);

        service.eliminar(ID);

        assertThat(h.getEstadoRegistro()).isEqualTo(EstadoRegistro.ELIMINADO);
        verify(habitacionRepository).saveAndFlush(h);
        verificarSinBorradoFisico();
    }

    @Test
    @DisplayName("eliminar: habitación OCUPADA -> 409, sigue ACTIVA")
    void eliminar_ocupada() {
        Habitacion h = habitacion(ID, 101, EstadoHabitacion.OCUPADA);
        dadoQueExiste(h);

        assertThatThrownBy(() -> service.eliminar(ID))
                .isInstanceOf(ConflictoException.class);

        assertThat(h.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
        verificarSinPersistencia();
        verificarSinBorradoFisico();
    }

    @Test
    @DisplayName("eliminar: habitación inexistente -> 404")
    void eliminar_inexistente() {
        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("eliminar: habitación ya ELIMINADA -> 404")
    void eliminar_yaEliminada() {
        dadoQueExiste(habitacion(ID, 101, EstadoHabitacion.DISPONIBLE, EstadoRegistro.ELIMINADO));

        assertThatThrownBy(() -> service.eliminar(ID))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }
}
