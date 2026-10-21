package com.equipouno.reservas.services;

import com.equipouno.commons.clients.HabitacionClient;
import com.equipouno.commons.clients.HuespedClient;
import com.equipouno.commons.dto.habitaciones.HabitacionResponse;
import com.equipouno.commons.dto.huespedes.HuespedResponse;
import com.equipouno.commons.dto.reservas.ReservaActivaResponse;
import com.equipouno.commons.dto.reservas.ReservaActualizarRequest;
import com.equipouno.commons.dto.reservas.ReservaCrearRequest;
import com.equipouno.commons.dto.reservas.ReservaResponse;
import com.equipouno.commons.enums.EstadoRegistro;
import com.equipouno.commons.enums.EventoReserva;
import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.DatoInvalidoException;
import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import com.equipouno.commons.exceptions.ServicioNoDisponibleException;
import com.equipouno.reservas.entities.Reserva;
import com.equipouno.reservas.enums.EstadoReserva;
import com.equipouno.reservas.mapper.ReservaMapper;
import com.equipouno.reservas.repositories.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link ReservaServiceImpl}.
 *
 * Se usan el mapper y la entidad Reserva reales (ahí vive la lógica de transiciones, fechas y
 * precios) y se simulan el repositorio y los clientes Feign de Huéspedes y Habitaciones.
 *
 * Reglas cubiertas: README 4.1 (EstadoReserva), 4.3 (Reserva), 5.1 (borrado lógico), 5.4 (códigos HTTP),
 * 6.4 (Reservas), 7 (Reportes) y 9 (casos obligatorios).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaServiceImpl")
class ReservaServiceImplTest {

    private static final Long ID_RESERVA = 100L;
    private static final Long ID_HUESPED = 5L;
    private static final Long ID_HABITACION = 7L;

    private static final LocalDate ENTRADA = LocalDate.of(2026, 11, 11);
    private static final LocalDate SALIDA = LocalDate.of(2026, 11, 14);       // 3 noches
    private static final BigDecimal PRECIO_NOCHE = new BigDecimal("750.00");

    private static final HuespedResponse HUESPED = new HuespedResponse(
            ID_HUESPED, "Juan Pérez", "juan@mail.com", "5512345678", "INE", "ABCD1234", "Mexicana");

    /** Habitación DISPONIBLE, capacidad 2, $750 por noche. */
    private static final HabitacionResponse HABITACION = new HabitacionResponse(
            ID_HABITACION, 101, "Habitacion con 1 cama", PRECIO_NOCHE, 2, "Lista para asignarse");

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private HuespedClient huespedClient;

    @Mock
    private HabitacionClient habitacionClient;

    private ReservaServiceImpl service;

    @BeforeEach
    void setUp() {
        // Orden de @RequiredArgsConstructor: reservaRepository, reservaMapper, huespedClient, habitacionClient
        service = new ReservaServiceImpl(reservaRepository, new ReservaMapper(), huespedClient, habitacionClient);

        // actualizar() usa el valor devuelto por save(); un mock devolvería null, así que simulamos el comportamiento real.
        lenient().when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static Reserva reserva(Long id, EstadoReserva estado) {
        return reserva(id, estado, EstadoRegistro.ACTIVO);
    }

    private static Reserva reserva(Long id, EstadoReserva estado, EstadoRegistro registro) {
        Reserva r = Reserva.builder()
                .id(id)
                .idHuesped(ID_HUESPED)
                .idHabitacion(ID_HABITACION)
                .fechaEntrada(ENTRADA)
                .fechaSalida(SALIDA)
                .numeroPersonas(2)
                .precioNoche(PRECIO_NOCHE)
                .estadoReserva(estado)
                .estadoRegistro(registro)
                .build();
        r.recalcularPrecioTotal();
        return r;
    }

    private static ReservaCrearRequest crearRequest(int personas, LocalDate entrada, LocalDate salida) {
        // El último parámetro (codigoReserva) lo exige el DTO actual; el servicio no lo usa.
        return new ReservaCrearRequest(ID_HUESPED, ID_HABITACION, entrada, salida, personas, 1L);
    }

    private static ReservaCrearRequest crearRequestValido() {
        return crearRequest(2, ENTRADA, SALIDA);
    }

    private static ReservaActualizarRequest actualizarRequest(LocalDate entrada, LocalDate salida) {
        return new ReservaActualizarRequest(entrada, salida);
    }

    /** Repositorio coherente para una reserva almacenada: todas las consultas por id responden igual que la BD. */
    private void dadoQueExiste(Reserva r) {
        for (EstadoRegistro er : EstadoRegistro.values()) {
            lenient().when(reservaRepository.findByIdAndEstadoRegistro(r.getId(), er))
                    .thenReturn(r.getEstadoRegistro() == er ? Optional.of(r) : Optional.empty());
        }
        lenient().when(reservaRepository.findById(r.getId())).thenReturn(Optional.of(r));
    }

    /** Huésped y habitación existentes y consultables por cualquiera de los endpoints de los clientes. */
    private void dadoHuespedYHabitacionExistentes() {
        lenient().when(huespedClient.obtenerHuespedPorId(ID_HUESPED)).thenReturn(ResponseEntity.ok(HUESPED));
        lenient().when(huespedClient.obtenerHuespedPorIdSinEstado(ID_HUESPED)).thenReturn(ResponseEntity.ok(HUESPED));
        lenient().when(habitacionClient.obtenerHabitacionPorId(ID_HABITACION)).thenReturn(ResponseEntity.ok(HABITACION));
        lenient().when(habitacionClient.obtenerHabitacionPorIdSinEstado(ID_HABITACION)).thenReturn(ResponseEntity.ok(HABITACION));
        lenient().when(habitacionClient.obtenerHabitacionPorIdDisponible(ID_HABITACION)).thenReturn(ResponseEntity.ok(HABITACION));
    }

    /** El huésped fue eliminado: el endpoint "activo" responde 404 y el endpoint interno sigue devolviendo los datos. */
    private void dadoHuespedEliminado() {
        lenient().when(huespedClient.obtenerHuespedPorId(ID_HUESPED))
                .thenThrow(new RecursoNoEncontradoException("Huesped no encontrado"));
        lenient().when(huespedClient.obtenerHuespedPorIdSinEstado(ID_HUESPED)).thenReturn(ResponseEntity.ok(HUESPED));
    }

    /** La habitación fue eliminada: el endpoint "activo" responde 404 y el endpoint interno sigue devolviendo los datos. */
    private void dadoHabitacionEliminada() {
        lenient().when(habitacionClient.obtenerHabitacionPorId(ID_HABITACION))
                .thenThrow(new RecursoNoEncontradoException("Habitacion no encontrada"));
        lenient().when(habitacionClient.obtenerHabitacionPorIdSinEstado(ID_HABITACION)).thenReturn(ResponseEntity.ok(HABITACION));
    }

    private void verificarSinPersistencia() {
        verify(reservaRepository, never()).save(any(Reserva.class));
        verify(reservaRepository, never()).saveAndFlush(any(Reserva.class));
    }

    private void verificarSinBorradoFisico() {
        verify(reservaRepository, never()).delete(any(Reserva.class));
        verify(reservaRepository, never()).deleteById(any());
        verify(reservaRepository, never()).deleteAll();
    }

    private void verificarHabitacionNoNotificada() {
        verify(habitacionClient, never()).aplicarEventoReserva(any(), any());
    }

    /** Fallos que pueden venir de un microservicio dependiente (ya traducidos por el ErrorDecoder de Feign). */
    static Stream<RuntimeException> fallosDeDependencia() {
        return Stream.of(
                new RecursoNoEncontradoException("no existe"),               // 404
                new ConflictoException("regla de negocio incumplida"),       // 409
                new ServicioNoDisponibleException("servicio no disponible")  // 503
        );
    }

    // =====================================================================================
    // Crear reserva (6.4 "Orden de operaciones al crear una reserva")
    // =====================================================================================

    @Test
    @DisplayName("registrar: crea la reserva CONFIRMADA y ACTIVA, calcula precioNoche y precioTotal, y ocupa la habitación")
    void registrar_ok() {
        dadoHuespedYHabitacionExistentes();
        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);

        ReservaResponse r = service.registrar(crearRequestValido());

        verify(reservaRepository).saveAndFlush(captor.capture());
        Reserva guardada = captor.getValue();
        assertThat(guardada.getIdHuesped()).isEqualTo(ID_HUESPED);
        assertThat(guardada.getIdHabitacion()).isEqualTo(ID_HABITACION);
        assertThat(guardada.getFechaEntrada()).isEqualTo(ENTRADA);
        assertThat(guardada.getFechaSalida()).isEqualTo(SALIDA);
        assertThat(guardada.getNumeroPersonas()).isEqualTo(2);
        assertThat(guardada.getEstadoReserva()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(guardada.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(guardada.getPrecioNoche()).isEqualByComparingTo("750.00");
        assertThat(guardada.getPrecioTotal()).isEqualByComparingTo("2250.00");   // 3 noches x 750

        // La habitación pasa DISPONIBLE -> OCUPADA (la decide el microservicio de Habitaciones)
        verify(habitacionClient).aplicarEventoReserva(ID_HABITACION, EventoReserva.RESERVA_CREADA);

        // La respuesta incluye datos del huésped y de la habitación
        assertThat(r.estadoReserva()).isEqualTo(EstadoReserva.CONFIRMADA.getDescripcion());
        assertThat(r.fechaEntrada()).isEqualTo("2026-11-11");
        assertThat(r.fechaSalida()).isEqualTo("2026-11-14");
        assertThat(r.precioNoche()).isEqualByComparingTo("750.00");
        assertThat(r.precioTotal()).isEqualByComparingTo("2250.00");
        assertThat(r.huesped().nombre()).isEqualTo("Juan Pérez");
        assertThat(r.huesped().email()).isEqualTo("juan@mail.com");
        assertThat(r.habitacion().numero()).isEqualTo(101);
        assertThat(r.habitacion().capacidad()).isEqualTo(2);
    }

    @Test
    @DisplayName("registrar: el huésped se valida antes de ocupar la habitación")
    void registrar_validaHuespedPrimero() {
        dadoHuespedYHabitacionExistentes();

        service.registrar(crearRequestValido());

        InOrder orden = inOrder(huespedClient, habitacionClient);
        orden.verify(huespedClient).obtenerHuespedPorId(ID_HUESPED);
        orden.verify(habitacionClient).aplicarEventoReserva(ID_HABITACION, EventoReserva.RESERVA_CREADA);
    }

    @Test
    @DisplayName("registrar: precioNoche es copia del precio de la habitación (no lo envía el cliente) y precioTotal = noches x precioNoche")
    void registrar_precioDesdeHabitacion() {
        HabitacionResponse cara = new HabitacionResponse(ID_HABITACION, 101, "Suite", new BigDecimal("899.99"), 4, "Lista para asignarse");
        lenient().when(huespedClient.obtenerHuespedPorId(ID_HUESPED)).thenReturn(ResponseEntity.ok(HUESPED));
        lenient().when(habitacionClient.obtenerHabitacionPorIdDisponible(ID_HABITACION)).thenReturn(ResponseEntity.ok(cara));
        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);

        service.registrar(crearRequest(3, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3)));   // 2 noches

        verify(reservaRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPrecioNoche()).isEqualByComparingTo("899.99");
        assertThat(captor.getValue().getPrecioTotal()).isEqualByComparingTo("1799.98");
    }

    @ParameterizedTest(name = "{0} personas en habitación de capacidad 2")
    @ValueSource(ints = {1, 2})
    @DisplayName("registrar: numeroPersonas entre 1 y la capacidad se acepta")
    void registrar_personasDentroDeCapacidad(int personas) {
        dadoHuespedYHabitacionExistentes();

        assertThatCode(() -> service.registrar(crearRequest(personas, ENTRADA, SALIDA))).doesNotThrowAnyException();

        verify(reservaRepository).saveAndFlush(any(Reserva.class));
    }

    @Test
    @DisplayName("registrar: numeroPersonas mayor que la capacidad de la habitación -> 409, sin guardar ni ocupar la habitación")
    void registrar_personasExcedenCapacidad() {
        dadoHuespedYHabitacionExistentes();

        assertThatThrownBy(() -> service.registrar(crearRequest(3, ENTRADA, SALIDA)))
                .isInstanceOf(ConflictoException.class);

        verificarSinPersistencia();
        verificarHabitacionNoNotificada();
    }

    @ParameterizedTest(name = "{0} personas")
    @ValueSource(ints = {0, -1})
    @DisplayName("registrar: numeroPersonas menor a 1 -> 400, sin guardar ni ocupar la habitación")
    void registrar_personasInvalidas(int personas) {
        dadoHuespedYHabitacionExistentes();

        assertThatThrownBy(() -> service.registrar(crearRequest(personas, ENTRADA, SALIDA)))
                .isInstanceOf(DatoInvalidoException.class);

        verificarSinPersistencia();
        verificarHabitacionNoNotificada();
    }

    @ParameterizedTest(name = "entrada={0}, salida={1}")
    @CsvSource({
            "2026-11-14, 2026-11-14",   // iguales
            "2026-11-15, 2026-11-14",   // entrada posterior a salida
            ",           2026-11-14",   // nulas
            "2026-11-11,"
    })
    @DisplayName("registrar: se exige fechaEntrada < fechaSalida -> 400, sin guardar ni ocupar la habitación")
    void registrar_fechasInvalidas(LocalDate entrada, LocalDate salida) {
        dadoHuespedYHabitacionExistentes();

        assertThatThrownBy(() -> service.registrar(crearRequest(2, entrada, salida)))
                .isInstanceOf(DatoInvalidoException.class);

        verificarSinPersistencia();
        verificarHabitacionNoNotificada();
    }

    @ParameterizedTest(name = "huésped: {0}")
    @MethodSource("fallosDeDependencia")
    @DisplayName("registrar: si falla la validación del huésped (404 / 409 / 503) se propaga, sin guardar ni tocar la habitación")
    void registrar_falloAlValidarHuesped(RuntimeException fallo) {
        when(huespedClient.obtenerHuespedPorId(ID_HUESPED)).thenThrow(fallo);

        assertThatThrownBy(() -> service.registrar(crearRequestValido()))
                .isSameAs(fallo);

        verifyNoInteractions(habitacionClient);
        verificarSinPersistencia();
    }

    @ParameterizedTest(name = "habitación: {0}")
    @MethodSource("fallosDeDependencia")
    @DisplayName("registrar: si falla la validación de la habitación (404 / 409 / 503) se propaga, sin guardar ni ocuparla")
    void registrar_falloAlValidarHabitacion(RuntimeException fallo) {
        lenient().when(huespedClient.obtenerHuespedPorId(ID_HUESPED)).thenReturn(ResponseEntity.ok(HUESPED));
        when(habitacionClient.obtenerHabitacionPorIdDisponible(ID_HABITACION)).thenThrow(fallo);

        assertThatThrownBy(() -> service.registrar(crearRequestValido()))
                .isSameAs(fallo);

        verificarSinPersistencia();
        verificarHabitacionNoNotificada();
    }

    @Test
    @DisplayName("registrar: si Habitaciones rechaza ocuparla (409) el error se propaga (la transacción local hace rollback)")
    void registrar_habitacionRechazaOcupar() {
        dadoHuespedYHabitacionExistentes();
        ConflictoException rechazo = new ConflictoException("La reserva no puede cambiar la habitación de OCUPADA a OCUPADA");
        doThrow(rechazo).when(habitacionClient).aplicarEventoReserva(ID_HABITACION, EventoReserva.RESERVA_CREADA);

        assertThatThrownBy(() -> service.registrar(crearRequestValido()))
                .isSameAs(rechazo);
    }

    @Test
    @DisplayName("registrar: si falla al guardar la reserva, la habitación NO se ocupa (no queda OCUPADA sin reserva)")
    void registrar_falloAlGuardar_noOcupaHabitacion() {
        dadoHuespedYHabitacionExistentes();
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenThrow(new IllegalStateException("fallo de BD"));

        assertThatThrownBy(() -> service.registrar(crearRequestValido()))
                .isInstanceOf(IllegalStateException.class);

        verificarHabitacionNoNotificada();
    }

    // =====================================================================================
    // Modificar reserva - PUT (6.4 "Modificación de reservas")
    // =====================================================================================

    @Test
    @DisplayName("actualizar CONFIRMADA: se pueden cambiar fecha de entrada y de salida y se recalcula precioTotal")
    void actualizar_confirmada_ok() {
        Reserva r = reserva(ID_RESERVA, EstadoReserva.CONFIRMADA);
        dadoQueExiste(r);
        dadoHuespedYHabitacionExistentes();

        ReservaResponse resp = service.actualizar(
                actualizarRequest(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 6)), ID_RESERVA);   // 5 noches

        assertThat(r.getFechaEntrada()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(r.getFechaSalida()).isEqualTo(LocalDate.of(2026, 12, 6));
        assertThat(r.getPrecioTotal()).isEqualByComparingTo("3750.00");          // 5 x 750
        assertThat(r.getEstadoReserva()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(r.getIdHuesped()).isEqualTo(ID_HUESPED);                       // no cambian
        assertThat(r.getIdHabitacion()).isEqualTo(ID_HABITACION);
        verify(reservaRepository).save(r);

        assertThat(resp.fechaEntrada()).isEqualTo("2026-12-01");
        assertThat(resp.fechaSalida()).isEqualTo("2026-12-06");
        assertThat(resp.precioTotal()).isEqualByComparingTo("3750.00");
        assertThat(resp.huesped().nombre()).isEqualTo("Juan Pérez");
        assertThat(resp.habitacion().numero()).isEqualTo(101);
    }

    @Test
    @DisplayName("actualizar: un cambio de precio de la habitación no afecta el precioNoche de la reserva ya creada")
    void actualizar_noAfectadaPorCambioDePrecioDeHabitacion() {
        Reserva r = reserva(ID_RESERVA, EstadoReserva.CONFIRMADA);        // precioNoche = 750
        dadoQueExiste(r);
        HabitacionResponse conPrecioNuevo = new HabitacionResponse(ID_HABITACION, 101, "Suite", new BigDecimal("1200.00"), 2, "Asignada a una reserva vigente");
        lenient().when(huespedClient.obtenerHuespedPorId(ID_HUESPED)).thenReturn(ResponseEntity.ok(HUESPED));
        lenient().when(habitacionClient.obtenerHabitacionPorId(ID_HABITACION)).thenReturn(ResponseEntity.ok(conPrecioNuevo));
        lenient().when(habitacionClient.obtenerHabitacionPorIdSinEstado(ID_HABITACION)).thenReturn(ResponseEntity.ok(conPrecioNuevo));

        service.actualizar(actualizarRequest(ENTRADA, LocalDate.of(2026, 11, 16)), ID_RESERVA);      // 5 noches

        assertThat(r.getPrecioNoche()).isEqualByComparingTo("750.00");
        assertThat(r.getPrecioTotal()).isEqualByComparingTo("3750.00");
    }

    @Test
    @DisplayName("actualizar CONFIRMADA: fechaEntrada >= fechaSalida -> 400 y no guarda")
    void actualizar_confirmada_fechasInvalidas() {
        Reserva r = reserva(ID_RESERVA, EstadoReserva.CONFIRMADA);
        dadoQueExiste(r);

        assertThatThrownBy(() -> service.actualizar(actualizarRequest(SALIDA, ENTRADA), ID_RESERVA))
                .isInstanceOf(DatoInvalidoException.class);

        assertThat(r.getFechaEntrada()).isEqualTo(ENTRADA);
        assertThat(r.getFechaSalida()).isEqualTo(SALIDA);
        verificarSinPersistencia();
    }

    @Test
    @DisplayName("actualizar EN_CURSO: se puede ampliar la fecha de salida y se recalcula precioTotal")
    void actualizar_enCurso_ampliarSalida() {
        Reserva r = reserva(ID_RESERVA, EstadoReserva.EN_CURSO);
        dadoQueExiste(r);
        dadoHuespedYHabitacionExistentes();

        service.actualizar(actualizarRequest(ENTRADA, LocalDate.of(2026, 11, 20)), ID_RESERVA);     // 9 noches

        assertThat(r.getFechaEntrada()).isEqualTo(ENTRADA);
        assertThat(r.getFechaSalida()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(r.getPrecioTotal()).isEqualByComparingTo("6750.00");                               // 9 x 750
        assertThat(r.getEstadoReserva()).isEqualTo(EstadoReserva.EN_CURSO);
        verify(reservaRepository).save(r);
    }

    @Test
    @DisplayName("actualizar EN_CURSO: se puede acortar la fecha de salida y se recalcula precioTotal")
    void actualizar_enCurso_acortarSalida() {
        Reserva r = reserva(ID_RESERVA, EstadoReserva.EN_CURSO);
        dadoQueExiste(r);
        dadoHuespedYHabitacionExistentes();

        service.actualizar(actualizarRequest(ENTRADA, LocalDate.of(2026, 11, 12)), ID_RESERVA);     // 1 noche

        assertThat(r.getFechaSalida()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(r.getPrecioTotal()).isEqualByComparingTo("750.00");
        verify(reservaRepository).save(r);
    }

    @Test
    @DisplayName("actualizar EN_CURSO: modificar la fecha de entrada -> 409, sin cambios")
    void actualizar_enCurso_cambiarEntrada() {
        Reserva r = reserva(ID_RESERVA, EstadoReserva.EN_CURSO);
        dadoQueExiste(r);

        assertThatThrownBy(() -> service.actualizar(actualizarRequest(LocalDate.of(2026, 11, 12), SALIDA), ID_RESERVA))
                .isInstanceOf(ConflictoException.class);

        assertThat(r.getFechaEntrada()).isEqualTo(ENTRADA);
        assertThat(r.getFechaSalida()).isEqualTo(SALIDA);
        assertThat(r.getPrecioTotal()).isEqualByComparingTo("2250.00");
        verificarSinPersistencia();
    }

    @ParameterizedTest(name = "nueva salida {0}")
    @ValueSource(strings = {"2026-11-11", "2026-11-10"})
    @DisplayName("actualizar EN_CURSO: la salida debe ser mayor que la entrada (igual o menor) -> 400, sin cambios")
    void actualizar_enCurso_salidaNoMayorQueEntrada(String nuevaSalida) {
        Reserva r = reserva(ID_RESERVA, EstadoReserva.EN_CURSO);
        dadoQueExiste(r);

        assertThatThrownBy(() -> service.actualizar(actualizarRequest(ENTRADA, LocalDate.parse(nuevaSalida)), ID_RESERVA))
                .isInstanceOf(DatoInvalidoException.class);

        assertThat(r.getFechaSalida()).isEqualTo(SALIDA);
        verificarSinPersistencia();
    }

    @ParameterizedTest(name = "reserva {0}")
    @EnumSource(value = EstadoReserva.class, names = {"FINALIZADA", "CANCELADA"})
    @DisplayName("actualizar: una reserva FINALIZADA o CANCELADA no se puede modificar -> 409")
    void actualizar_finalizadaOCancelada(EstadoReserva estado) {
        Reserva r = reserva(ID_RESERVA, estado);
        dadoQueExiste(r);

        assertThatThrownBy(() -> service.actualizar(actualizarRequest(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5)), ID_RESERVA))
                .isInstanceOf(ConflictoException.class);

        assertThat(r.getFechaEntrada()).isEqualTo(ENTRADA);
        assertThat(r.getFechaSalida()).isEqualTo(SALIDA);
        verificarSinPersistencia();
    }

    @Test
    @DisplayName("actualizar: reserva inexistente -> 404")
    void actualizar_inexistente() {
        assertThatThrownBy(() -> service.actualizar(actualizarRequest(ENTRADA, SALIDA), 999L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("actualizar: reserva ELIMINADA -> 404")
    void actualizar_eliminada() {
        dadoQueExiste(reserva(ID_RESERVA, EstadoReserva.FINALIZADA, EstadoRegistro.ELIMINADO));

        assertThatThrownBy(() -> service.actualizar(actualizarRequest(ENTRADA, SALIDA), ID_RESERVA))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    // =====================================================================================
    // PATCH /{idReserva}/estado/{idEstado}  (check-in, check-out, cancelación)
    // =====================================================================================

    @Test
    @DisplayName("check-in: CONFIRMADA -> EN_CURSO y la habitación permanece OCUPADA (no se notifica)")
    void checkIn() {
        Reserva r = reserva(ID_RESERVA, EstadoReserva.CONFIRMADA);
        dadoQueExiste(r);

        service.actualizarEstadoReserva(ID_RESERVA, EstadoReserva.EN_CURSO.getCodigo());

        assertThat(r.getEstadoReserva()).isEqualTo(EstadoReserva.EN_CURSO);
        verify(reservaRepository).saveAndFlush(r);
        verifyNoInteractions(habitacionClient);
    }

    @Test
    @DisplayName("check-out: EN_CURSO -> FINALIZADA y la habitación pasa a LIMPIEZA (evento CHECK_OUT)")
    void checkOut() {
        Reserva r = reserva(ID_RESERVA, EstadoReserva.EN_CURSO);
        dadoQueExiste(r);

        service.actualizarEstadoReserva(ID_RESERVA, EstadoReserva.FINALIZADA.getCodigo());

        assertThat(r.getEstadoReserva()).isEqualTo(EstadoReserva.FINALIZADA);
        InOrder orden = inOrder(reservaRepository, habitacionClient);
        orden.verify(reservaRepository).saveAndFlush(r);
        orden.verify(habitacionClient).aplicarEventoReserva(ID_HABITACION, EventoReserva.CHECK_OUT);
    }

    @Test
    @DisplayName("cancelación: CONFIRMADA -> CANCELADA y la habitación vuelve a DISPONIBLE (evento CANCELACION)")
    void cancelacion() {
        Reserva r = reserva(ID_RESERVA, EstadoReserva.CONFIRMADA);
        dadoQueExiste(r);

        service.actualizarEstadoReserva(ID_RESERVA, EstadoReserva.CANCELADA.getCodigo());

        assertThat(r.getEstadoReserva()).isEqualTo(EstadoReserva.CANCELADA);
        InOrder orden = inOrder(reservaRepository, habitacionClient);
        orden.verify(reservaRepository).saveAndFlush(r);
        orden.verify(habitacionClient).aplicarEventoReserva(ID_HABITACION, EventoReserva.CANCELACION);
    }

    /** Todas las combinaciones origen -> destino salvo 1->2, 2->3 y 1->4 (incluye repetir el estado y volver a CONFIRMADA). */
    static Stream<Arguments> transicionesInvalidas() {
        Set<String> validas = Set.of("CONFIRMADA>EN_CURSO", "EN_CURSO>FINALIZADA", "CONFIRMADA>CANCELADA");
        return Arrays.stream(EstadoReserva.values())
                .flatMap(origen -> Arrays.stream(EstadoReserva.values())
                        .filter(destino -> !validas.contains(origen + ">" + destino))
                        .map(destino -> Arguments.of(origen, destino)));
    }

    @ParameterizedTest(name = "{0} -> {1} => 409")
    @MethodSource("transicionesInvalidas")
    @DisplayName("PATCH estado: cualquier otra transición -> 409, la reserva y la habitación no cambian")
    void transicionInvalida(EstadoReserva origen, EstadoReserva destino) {
        Reserva r = reserva(ID_RESERVA, origen);
        dadoQueExiste(r);

        assertThatThrownBy(() -> service.actualizarEstadoReserva(ID_RESERVA, destino.getCodigo()))
                .isInstanceOf(ConflictoException.class);

        assertThat(r.getEstadoReserva()).isEqualTo(origen);
        verificarSinPersistencia();
        verifyNoInteractions(habitacionClient);
    }

    @ParameterizedTest(name = "idEstado {0} => 400")
    @ValueSource(longs = {0, 5, 99, -1})
    @DisplayName("PATCH estado: idEstado fuera de 1 a 4 -> 400")
    void patch_estadoFueraDeCatalogo(long idEstado) {
        Reserva r = reserva(ID_RESERVA, EstadoReserva.CONFIRMADA);
        dadoQueExiste(r);

        assertThatThrownBy(() -> service.actualizarEstadoReserva(ID_RESERVA, idEstado))
                .isInstanceOf(DatoInvalidoException.class);

        assertThat(r.getEstadoReserva()).isEqualTo(EstadoReserva.CONFIRMADA);
        verificarSinPersistencia();
        verifyNoInteractions(habitacionClient);
    }

    @Test
    @DisplayName("PATCH estado: reserva inexistente -> 404")
    void patch_reservaInexistente() {
        assertThatThrownBy(() -> service.actualizarEstadoReserva(999L, EstadoReserva.EN_CURSO.getCodigo()))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
        verifyNoInteractions(habitacionClient);
    }

    @Test
    @DisplayName("PATCH estado: reserva ELIMINADA -> 404")
    void patch_reservaEliminada() {
        dadoQueExiste(reserva(ID_RESERVA, EstadoReserva.CANCELADA, EstadoRegistro.ELIMINADO));

        assertThatThrownBy(() -> service.actualizarEstadoReserva(ID_RESERVA, EstadoReserva.EN_CURSO.getCodigo()))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fallosDeDependencia")
    @DisplayName("check-out: si Habitaciones falla (404 / 409 / 503) el error se propaga para que se revierta la reserva")
    void checkOut_falloDeHabitaciones(RuntimeException fallo) {
        dadoQueExiste(reserva(ID_RESERVA, EstadoReserva.EN_CURSO));
        doThrow(fallo).when(habitacionClient).aplicarEventoReserva(ID_HABITACION, EventoReserva.CHECK_OUT);

        assertThatThrownBy(() -> service.actualizarEstadoReserva(ID_RESERVA, EstadoReserva.FINALIZADA.getCodigo()))
                .isSameAs(fallo);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fallosDeDependencia")
    @DisplayName("cancelación: si Habitaciones falla (404 / 409 / 503) el error se propaga para que se revierta la reserva")
    void cancelacion_falloDeHabitaciones(RuntimeException fallo) {
        dadoQueExiste(reserva(ID_RESERVA, EstadoReserva.CONFIRMADA));
        doThrow(fallo).when(habitacionClient).aplicarEventoReserva(ID_HABITACION, EventoReserva.CANCELACION);

        assertThatThrownBy(() -> service.actualizarEstadoReserva(ID_RESERVA, EstadoReserva.CANCELADA.getCodigo()))
                .isSameAs(fallo);
    }

    // =====================================================================================
    // Eliminar (6.4: solo FINALIZADA o CANCELADA, no afecta a la habitación)
    // =====================================================================================

    @ParameterizedTest(name = "reserva {0}")
    @EnumSource(value = EstadoReserva.class, names = {"FINALIZADA", "CANCELADA"})
    @DisplayName("eliminar: FINALIZADA o CANCELADA se elimina lógicamente y NO afecta a la habitación")
    void eliminar_ok(EstadoReserva estado) {
        Reserva r = reserva(ID_RESERVA, estado);
        dadoQueExiste(r);

        service.eliminar(ID_RESERVA);

        assertThat(r.getEstadoRegistro()).isEqualTo(EstadoRegistro.ELIMINADO);
        assertThat(r.getEstadoReserva()).isEqualTo(estado);          // el estado de la reserva se conserva para reportes
        verify(reservaRepository).saveAndFlush(r);
        verifyNoInteractions(habitacionClient);
        verificarSinBorradoFisico();
    }

    @ParameterizedTest(name = "reserva {0} => 409")
    @EnumSource(value = EstadoReserva.class, names = {"CONFIRMADA", "EN_CURSO"})
    @DisplayName("eliminar: CONFIRMADA o EN_CURSO -> 409, sigue ACTIVA")
    void eliminar_vigente(EstadoReserva estado) {
        Reserva r = reserva(ID_RESERVA, estado);
        dadoQueExiste(r);

        assertThatThrownBy(() -> service.eliminar(ID_RESERVA))
                .isInstanceOf(ConflictoException.class);

        assertThat(r.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
        verificarSinPersistencia();
        verificarSinBorradoFisico();
        verifyNoInteractions(habitacionClient);
    }

    @Test
    @DisplayName("eliminar: reserva inexistente -> 404")
    void eliminar_inexistente() {
        assertThatThrownBy(() -> service.eliminar(999L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    @Test
    @DisplayName("eliminar: reserva ya ELIMINADA -> 404")
    void eliminar_yaEliminada() {
        dadoQueExiste(reserva(ID_RESERVA, EstadoReserva.FINALIZADA, EstadoRegistro.ELIMINADO));

        assertThatThrownBy(() -> service.eliminar(ID_RESERVA))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verificarSinPersistencia();
    }

    // =====================================================================================
    // Listar y consultar (las eliminadas dejan de aparecer en los listados)
    // =====================================================================================

    @Test
    @DisplayName("listar: devuelve solo reservas ACTIVAS, con los datos del huésped y la habitación")
    void listar_soloActivas() {
        dadoHuespedYHabitacionExistentes();
        when(reservaRepository.findByEstadoRegistro(EstadoRegistro.ACTIVO))
                .thenReturn(List.of(reserva(1L, EstadoReserva.CONFIRMADA), reserva(2L, EstadoReserva.EN_CURSO)));

        List<ReservaResponse> resultado = service.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(ReservaResponse::id).containsExactly(1L, 2L);
        assertThat(resultado).allSatisfy(r -> {
            assertThat(r.huesped().nombre()).isEqualTo("Juan Pérez");
            assertThat(r.habitacion().numero()).isEqualTo(101);
        });
        verify(reservaRepository).findByEstadoRegistro(EstadoRegistro.ACTIVO);
        verify(reservaRepository, never()).findAll();
    }

    @Test
    @DisplayName("listar: lista vacía si no hay reservas activas")
    void listar_vacio() {
        when(reservaRepository.findByEstadoRegistro(EstadoRegistro.ACTIVO)).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }

    @Test
    @DisplayName("listar: una reserva FINALIZADA cuyo huésped fue eliminado sigue apareciendo (no rompe el listado con 404)")
    void listar_conHuespedEliminado() {
        // Eliminar un huésped solo se bloquea con reservas CONFIRMADA/EN_CURSO, por lo que
        // pueden existir reservas FINALIZADAS/CANCELADAS de huéspedes ya eliminados.
        dadoHuespedEliminado();
        dadoHabitacionExistente();
        when(reservaRepository.findAll())
                .thenReturn(List.of(reserva(1L, EstadoReserva.FINALIZADA)));

        List<ReservaResponse> resultado = service.listarHistoricoReservas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).huesped().nombre()).isEqualTo("Juan Pérez");
    }

    @Test
    @DisplayName("listar: una reserva FINALIZADA cuya habitación fue eliminada sigue apareciendo (no rompe el listado con 404)")
    void listar_conHabitacionEliminada() {
        dadoHabitacionEliminada();
        lenient().when(huespedClient.obtenerHuespedPorIdSinEstado(ID_HUESPED)).thenReturn(ResponseEntity.ok(HUESPED));
        lenient().when(habitacionClient.obtenerHabitacionPorIdSinEstado(ID_HABITACION)).thenReturn(ResponseEntity.ok(HABITACION));
        when(reservaRepository.findAll())
                .thenReturn(List.of(reserva(1L, EstadoReserva.FINALIZADA)));

        List<ReservaResponse> resultado = service.listarHistoricoReservas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).habitacion().numero()).isEqualTo(101);
    }

    private void dadoHabitacionExistente() {
        lenient().when(habitacionClient.obtenerHabitacionPorId(ID_HABITACION)).thenReturn(ResponseEntity.ok(HABITACION));
        lenient().when(habitacionClient.obtenerHabitacionPorIdSinEstado(ID_HABITACION)).thenReturn(ResponseEntity.ok(HABITACION));
    }

    @Test
    @DisplayName("obtenerPorId: devuelve la reserva con los datos del huésped y la habitación")
    void obtenerPorId_ok() {
        dadoQueExiste(reserva(ID_RESERVA, EstadoReserva.CONFIRMADA));
        dadoHuespedYHabitacionExistentes();

        ReservaResponse r = service.obtenerPorId(ID_RESERVA);

        assertThat(r.id()).isEqualTo(ID_RESERVA);
        assertThat(r.estadoReserva()).isEqualTo(EstadoReserva.CONFIRMADA.getDescripcion());
        assertThat(r.precioNoche()).isEqualByComparingTo("750.00");
        assertThat(r.precioTotal()).isEqualByComparingTo("2250.00");
        assertThat(r.numeroPersonas()).isEqualTo(2);
        assertThat(r.huesped().email()).isEqualTo("juan@mail.com");
        assertThat(r.habitacion().tipo()).isEqualTo("Habitacion con 1 cama");
    }

    @Test
    @DisplayName("obtenerPorId: reserva inexistente -> 404")
    void obtenerPorId_inexistente() {
        assertThatThrownBy(() -> service.obtenerPorId(999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("obtenerPorId: reserva ELIMINADA -> 404")
    void obtenerPorId_eliminada() {
        dadoQueExiste(reserva(ID_RESERVA, EstadoReserva.FINALIZADA, EstadoRegistro.ELIMINADO));

        assertThatThrownBy(() -> service.obtenerPorId(ID_RESERVA))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =====================================================================================
    // Reportes (sección 7): sin filtrar por estado ni estadoRegistro, usando endpoints internos
    // =====================================================================================

    @Test
    @DisplayName("histórico: incluye TODAS las reservas sin importar estado ni estadoRegistro (CANCELADAS y ELIMINADAS incluidas)")
    void historico_incluyeTodas() {
        dadoHuespedYHabitacionExistentes();
        when(reservaRepository.findAll()).thenReturn(List.of(
                reserva(1L, EstadoReserva.CONFIRMADA),
                reserva(2L, EstadoReserva.EN_CURSO),
                reserva(3L, EstadoReserva.CANCELADA),
                reserva(4L, EstadoReserva.FINALIZADA, EstadoRegistro.ELIMINADO)));

        List<ReservaResponse> resultado = service.listarHistoricoReservas();

        assertThat(resultado).extracting(ReservaResponse::id).containsExactly(1L, 2L, 3L, 4L);
        verify(reservaRepository, never()).findByEstadoRegistro(any());
    }

    @Test
    @DisplayName("histórico: obtiene huésped y habitación con los endpoints internos (sin estado), no con los de ACTIVOS")
    void historico_usaEndpointsInternos() {
        dadoHuespedYHabitacionExistentes();
        when(reservaRepository.findAll()).thenReturn(List.of(
                reserva(1L, EstadoReserva.CANCELADA),
                reserva(2L, EstadoReserva.FINALIZADA, EstadoRegistro.ELIMINADO)));

        service.listarHistoricoReservas();

        verify(huespedClient, times(2)).obtenerHuespedPorIdSinEstado(ID_HUESPED);
        verify(habitacionClient, times(2)).obtenerHabitacionPorIdSinEstado(ID_HABITACION);
        verify(huespedClient, never()).obtenerHuespedPorId(any());
        verify(habitacionClient, never()).obtenerHabitacionPorId(any());
    }

    @Test
    @DisplayName("histórico: con huésped y habitación ELIMINADOS sigue devolviendo sus datos")
    void historico_conHuespedYHabitacionEliminados() {
        dadoHuespedEliminado();
        dadoHabitacionEliminada();
        when(reservaRepository.findAll()).thenReturn(List.of(reserva(1L, EstadoReserva.FINALIZADA, EstadoRegistro.ELIMINADO)));

        List<ReservaResponse> resultado = service.listarHistoricoReservas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).huesped().nombre()).isEqualTo("Juan Pérez");
        assertThat(resultado.get(0).habitacion().numero()).isEqualTo(101);
    }

    @Test
    @DisplayName("histórico: sin reservas devuelve lista vacía")
    void historico_vacio() {
        when(reservaRepository.findAll()).thenReturn(List.of());

        assertThat(service.listarHistoricoReservas()).isEmpty();
    }

    @Test
    @DisplayName("por-estado: incluye las reservas ELIMINADAS de ese estado y usa endpoints internos")
    void porEstado_incluyeEliminadas() {
        dadoHuespedYHabitacionExistentes();
        Pageable pageable = Pageable.unpaged();
        Page<Reserva> pagina = new PageImpl<>(List.of(
                reserva(1L, EstadoReserva.CANCELADA),
                reserva(2L, EstadoReserva.CANCELADA, EstadoRegistro.ELIMINADO)));
        when(reservaRepository.findByEstadoReserva(EstadoReserva.CANCELADA, pageable)).thenReturn(pagina);

        Page<ReservaResponse> resultado = service.listarReservasAgrupadasEstadoReservaSinEstado(
                EstadoReserva.CANCELADA.getCodigo(), pageable);

        assertThat(resultado.getContent()).extracting(ReservaResponse::id).containsExactly(1L, 2L);
        assertThat(resultado.getContent()).allSatisfy(r ->
                assertThat(r.estadoReserva()).isEqualTo(EstadoReserva.CANCELADA.getDescripcion()));
        verify(huespedClient, never()).obtenerHuespedPorId(any());
        verify(habitacionClient, never()).obtenerHabitacionPorId(any());
    }

    @ParameterizedTest(name = "código {0} => 400")
    @ValueSource(longs = {0, 5, 99, -1})
    @DisplayName("por-estado: código de estado fuera de catálogo -> 400")
    void porEstado_codigoInvalido(long codigo) {
        assertThatThrownBy(() -> service.listarReservasAgrupadasEstadoReservaSinEstado(codigo, Pageable.unpaged()))
                .isInstanceOf(DatoInvalidoException.class);

        verify(reservaRepository, never()).findByEstadoReserva(any(), any());
    }

    // =====================================================================================
    // Consumo interno entre microservicios (Huéspedes -> Reservas, Habitaciones -> Reservas)
    // =====================================================================================

    @Test
    @DisplayName("huespedNoTieneReservaActiva: sin reservas vigentes no lanza excepción")
    void huesped_sinReservasVigentes() {
        assertThatCode(() -> service.huespedNoTieneReservaActiva(ID_HUESPED)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("huespedNoTieneReservaActiva: con reserva CONFIRMADA -> 409")
    void huesped_conReservaConfirmada() {
        when(reservaRepository.existsByIdHuespedAndEstadoReservaAndEstadoRegistro(
                ID_HUESPED, EstadoReserva.CONFIRMADA, EstadoRegistro.ACTIVO)).thenReturn(true);

        assertThatThrownBy(() -> service.huespedNoTieneReservaActiva(ID_HUESPED))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("huespedNoTieneReservaActiva: con reserva EN_CURSO -> 409")
    void huesped_conReservaEnCurso() {

        when(reservaRepository.existsByIdHuespedAndEstadoReservaAndEstadoRegistro(
                ID_HUESPED,
                EstadoReserva.CONFIRMADA,
                EstadoRegistro.ACTIVO
        )).thenReturn(false);

        when(reservaRepository.existsByIdHuespedAndEstadoReservaAndEstadoRegistro(
                ID_HUESPED,
                EstadoReserva.EN_CURSO,
                EstadoRegistro.ACTIVO
        )).thenReturn(true);

        assertThatThrownBy(() -> service.huespedNoTieneReservaActiva(ID_HUESPED))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("huespedNoTieneReservaActiva: las reservas FINALIZADA o CANCELADA no bloquean")
    void huesped_reservasTerminadasNoBloquean() {
        assertThatCode(() -> service.huespedNoTieneReservaActiva(ID_HUESPED)).doesNotThrowAnyException();

        verify(reservaRepository, never()).existsByIdHuespedAndEstadoReservaAndEstadoRegistro(
                any(), eq(EstadoReserva.FINALIZADA), any());
        verify(reservaRepository, never()).existsByIdHuespedAndEstadoReservaAndEstadoRegistro(
                any(), eq(EstadoReserva.CANCELADA), any());
    }

    @Test
    @DisplayName("obtenerReservaActivaPorHabitacion: devuelve id, habitación y numeroPersonas de la reserva vigente (CONFIRMADA o EN_CURSO)")
    void reservaActiva_existe() {
        Reserva r = reserva(ID_RESERVA, EstadoReserva.EN_CURSO);
        when(reservaRepository.findByIdHabitacionAndEstadoRegistroAndEstadoReservaIn(
                eq(ID_HABITACION), eq(EstadoRegistro.ACTIVO),
                argThat(estados -> estados.size() == 2
                        && estados.contains(EstadoReserva.CONFIRMADA)
                        && estados.contains(EstadoReserva.EN_CURSO))))
                .thenReturn(Optional.of(r));

        ReservaActivaResponse resultado = service.obtenerReservaActivaPorHabitacion(ID_HABITACION);

        assertThat(resultado).isNotNull();
        assertThat(resultado.idReserva()).isEqualTo(ID_RESERVA);
        assertThat(resultado.idHabitacion()).isEqualTo(ID_HABITACION);
        assertThat(resultado.numeroPersonas()).isEqualTo(2);
    }

    @Test
    @DisplayName("obtenerReservaActivaPorHabitacion: sin reserva vigente devuelve null")
    void reservaActiva_noExiste() {
        assertThat(service.obtenerReservaActivaPorHabitacion(ID_HABITACION)).isNull();
    }
}