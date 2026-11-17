package com.veterinaria.turno.application;

import com.veterinaria.turno.domain.exception.BadRequestException;
import com.veterinaria.turno.domain.exception.EstadoTurnoInvalidoException;
import com.veterinaria.turno.domain.exception.MascotaNotFoundException;
import com.veterinaria.turno.domain.exception.TurnoNotFoundException;
import com.veterinaria.turno.domain.exception.TurnoSuperpuestoException;
import com.veterinaria.turno.domain.exception.VeterinarioNotFoundException;
import com.veterinaria.turno.domain.model.EstadoTurno;
import com.veterinaria.turno.domain.model.Participacion;
import com.veterinaria.turno.domain.model.RolVeterinario;
import com.veterinaria.turno.domain.model.Turno;
import com.veterinaria.turno.domain.port.out.MascotaServicePort;
import com.veterinaria.turno.domain.port.out.TurnoRepositoryPort;
import com.veterinaria.turno.domain.port.out.VeterinarioServicePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnoServiceImplTest {

    private static final LocalDate FECHA = LocalDate.now().plusDays(7);

    @Mock
    private TurnoRepositoryPort turnoRepository;

    @Mock
    private MascotaServicePort mascotaService;

    @Mock
    private VeterinarioServicePort veterinarioService;

    @InjectMocks
    private TurnoServiceImpl turnoService;

    @Test
    void registrar_cuandoEsValido_guardaTurnoPendiente() {
        Turno turno = crearTurno(LocalTime.of(10, 0), 30);
        turno.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        referenciasExistentes();
        when(turnoRepository.buscarPorVeterinarioYFecha(2L, FECHA)).thenReturn(List.of());
        when(turnoRepository.guardar(turno)).thenReturn(turno);

        Turno resultado = turnoService.registrar(turno);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTurno.PENDIENTE);
        verify(mascotaService).existsById(1L);
        verify(veterinarioService).existsById(2L);
        verify(turnoRepository).guardar(turno);
    }

    @Test
    void registrar_conMascotaInexistente_lanzaMascotaNotFoundExceptionYNoGuarda() {
        Turno turno = crearTurno(LocalTime.of(10, 0), 30);
        turno.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        when(mascotaService.existsById(1L)).thenReturn(false);

        assertThrows(MascotaNotFoundException.class, () -> turnoService.registrar(turno));
        verify(veterinarioService, never()).existsById(anyLong());
        verify(turnoRepository, never()).guardar(any(Turno.class));
    }

    @Test
    void registrar_conVeterinarioInexistente_lanzaVeterinarioNotFoundExceptionYNoGuarda() {
        Turno turno = crearTurno(LocalTime.of(10, 0), 30);
        turno.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        turno.agregarParticipacion(3L, RolVeterinario.ASISTENTE);
        when(mascotaService.existsById(1L)).thenReturn(true);
        // Las participaciones son un Set: el orden de consulta no esta garantizado. Solo el 3 no existe.
        when(veterinarioService.existsById(anyLong())).thenAnswer(inv -> !Long.valueOf(3L).equals(inv.getArgument(0)));

        VeterinarioNotFoundException ex = assertThrows(VeterinarioNotFoundException.class,
                () -> turnoService.registrar(turno));
        assertThat(ex.getMessage()).contains("id 3");
        verify(turnoRepository, never()).guardar(any(Turno.class));
    }

    @Test
    void registrar_conRequestInvalido_noConsultaOtrosMicroservicios() {
        Turno turno = crearTurno(LocalTime.of(10, 0), 5);
        turno.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);

        assertThrows(BadRequestException.class, () -> turnoService.registrar(turno));
        verifyNoInteractions(mascotaService, veterinarioService);
    }

    @Test
    void registrar_sinVeterinarios_lanzaBadRequestException() {
        Turno turno = crearTurno(LocalTime.of(10, 0), 30);

        assertThrows(BadRequestException.class, () -> turnoService.registrar(turno));
        verify(turnoRepository, never()).guardar(any(Turno.class));
    }

    @Test
    void registrar_conDuracionMenorA10_lanzaBadRequestException() {
        Turno turno = crearTurno(LocalTime.of(10, 0), 5);
        turno.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);

        assertThrows(BadRequestException.class, () -> turnoService.registrar(turno));
    }

    @Test
    void registrar_sinPrincipal_lanzaBadRequestException() {
        Turno turno = crearTurno(LocalTime.of(10, 0), 30);
        turno.agregarParticipacion(2L, RolVeterinario.ASISTENTE);

        assertThrows(BadRequestException.class, () -> turnoService.registrar(turno));
    }

    @Test
    void registrar_conDosPrincipales_lanzaBadRequestException() {
        Turno turno = crearTurno(LocalTime.of(10, 0), 30);
        turno.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        turno.agregarParticipacion(3L, RolVeterinario.PRINCIPAL);

        assertThrows(BadRequestException.class, () -> turnoService.registrar(turno));
    }

    @Test
    void registrar_conVeterinarioRepetido_lanzaBadRequestException() {
        Turno turno = crearTurno(LocalTime.of(10, 0), 30);
        turno.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        turno.agregarParticipacion(2L, RolVeterinario.ASISTENTE);

        assertThrows(BadRequestException.class, () -> turnoService.registrar(turno));
    }

    @Test
    void registrar_conTurnoSuperpuesto_lanzaTurnoSuperpuestoExceptionYNoGuarda() {
        Turno existente = crearTurno(LocalTime.of(10, 0), 30);
        existente.setId(1L);
        Turno nuevo = crearTurno(LocalTime.of(10, 15), 30);
        nuevo.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        referenciasExistentes();
        when(turnoRepository.buscarPorVeterinarioYFecha(2L, FECHA)).thenReturn(List.of(existente));

        assertThrows(TurnoSuperpuestoException.class, () -> turnoService.registrar(nuevo));
        verify(turnoRepository, never()).guardar(any(Turno.class));
    }

    @Test
    void registrar_conTurnoContiguo_noEsSuperposicion() {
        Turno existente = crearTurno(LocalTime.of(10, 0), 30);
        existente.setId(1L);
        Turno nuevo = crearTurno(LocalTime.of(10, 30), 30);
        nuevo.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        referenciasExistentes();
        when(turnoRepository.buscarPorVeterinarioYFecha(2L, FECHA)).thenReturn(List.of(existente));
        when(turnoRepository.guardar(nuevo)).thenReturn(nuevo);

        assertThat(turnoService.registrar(nuevo)).isEqualTo(nuevo);
    }

    @Test
    void actualizar_ignoraElMismoTurnoAlValidarSuperposicion() {
        Turno existente = crearTurno(LocalTime.of(10, 0), 30);
        existente.setId(1L);
        existente.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        Turno cambios = crearTurno(LocalTime.of(10, 10), 30);
        cambios.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        cambios.agregarParticipacion(3L, RolVeterinario.ASISTENTE);
        referenciasExistentes();
        when(turnoRepository.buscarPorId(1L)).thenReturn(existente);
        when(turnoRepository.buscarPorVeterinarioYFecha(2L, FECHA)).thenReturn(List.of(existente));
        when(turnoRepository.buscarPorVeterinarioYFecha(3L, FECHA)).thenReturn(List.of());
        when(turnoRepository.guardar(existente)).thenReturn(existente);

        Turno resultado = turnoService.actualizar(1L, cambios);

        assertThat(resultado.getHora()).isEqualTo(LocalTime.of(10, 10));
        assertThat(resultado.getParticipaciones()).hasSize(2);
        assertThat(resultado.getParticipaciones()).allMatch(participacion -> participacion.getTurno() == existente);
    }

    @Test
    void actualizar_cuandoNoExiste_lanzaTurnoNotFoundException() {
        Turno cambios = crearTurno(LocalTime.of(10, 0), 30);
        cambios.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        when(turnoRepository.buscarPorId(99L)).thenReturn(null);

        assertThrows(TurnoNotFoundException.class, () -> turnoService.actualizar(99L, cambios));
        verifyNoInteractions(mascotaService, veterinarioService);
    }

    @Test
    void actualizar_conMascotaInexistente_lanzaMascotaNotFoundExceptionYNoGuarda() {
        Turno existente = crearTurno(LocalTime.of(10, 0), 30);
        existente.setId(1L);
        Turno cambios = crearTurno(LocalTime.of(11, 0), 30);
        cambios.setMascotaId(50L);
        cambios.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        when(turnoRepository.buscarPorId(1L)).thenReturn(existente);
        when(mascotaService.existsById(50L)).thenReturn(false);

        assertThrows(MascotaNotFoundException.class, () -> turnoService.actualizar(1L, cambios));
        assertThat(existente.getMascotaId()).isEqualTo(1L);
        verify(turnoRepository, never()).guardar(any(Turno.class));
    }

    @Test
    void buscarAgenda_sinFecha_lanzaBadRequestException() {
        assertThrows(BadRequestException.class, () -> turnoService.buscarAgenda(2L, null));
    }

    @Test
    void buscarAgenda_conParametros_retornaTurnosDelVeterinario() {
        Turno turno = crearTurno(LocalTime.of(10, 0), 30);
        when(turnoRepository.buscarPorVeterinarioYFecha(2L, FECHA)).thenReturn(List.of(turno));

        assertThat(turnoService.buscarAgenda(2L, FECHA)).containsExactly(turno);
    }

    @Test
    void buscarParticipaciones_ordenaPorRolYVeterinario() {
        Turno turno = crearTurno(LocalTime.of(10, 0), 30);
        turno.agregarParticipacion(5L, RolVeterinario.ASISTENTE);
        turno.agregarParticipacion(4L, RolVeterinario.CONSULTOR);
        turno.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        turno.agregarParticipacion(3L, RolVeterinario.ASISTENTE);
        when(turnoRepository.buscarPorId(1L)).thenReturn(turno);

        List<Long> veterinarios = turnoService.buscarParticipaciones(1L).stream()
                .map(Participacion::getVeterinarioId)
                .toList();

        assertThat(veterinarios).containsExactly(2L, 3L, 5L, 4L);
    }

    @Test
    void registrar_conTurnoCanceladoEnElMismoHorario_noEsSuperposicion() {
        Turno cancelado = crearTurno(LocalTime.of(10, 0), 30);
        cancelado.setId(1L);
        cancelado.setEstado(EstadoTurno.CANCELADO);
        Turno nuevo = crearTurno(LocalTime.of(10, 0), 30);
        nuevo.agregarParticipacion(2L, RolVeterinario.PRINCIPAL);
        referenciasExistentes();
        when(turnoRepository.buscarPorVeterinarioYFecha(2L, FECHA)).thenReturn(List.of(cancelado));
        when(turnoRepository.guardar(nuevo)).thenReturn(nuevo);

        assertThat(turnoService.registrar(nuevo)).isEqualTo(nuevo);
    }

    @Test
    void buscarPorMascotaId_retornaTurnosDeLaMascota() {
        Turno turno = crearTurno(LocalTime.of(10, 0), 30);
        when(turnoRepository.buscarPorMascotaId(1L)).thenReturn(List.of(turno));

        assertThat(turnoService.buscarPorMascotaId(1L)).containsExactly(turno);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"PENDIENTE", "CONFIRMADO"})
    void cancelar_cuandoEstaPendienteOConfirmado_loPasaACancelado(EstadoTurno estado) {
        Turno turno = crearTurno(LocalTime.of(10, 0), 30);
        turno.setId(1L);
        turno.setEstado(estado);
        when(turnoRepository.buscarPorId(1L)).thenReturn(turno);
        when(turnoRepository.guardar(turno)).thenReturn(turno);

        Turno resultado = turnoService.cancelar(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTurno.CANCELADO);
        verify(turnoRepository).guardar(turno);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"CANCELADO", "ATENDIDO", "AUSENTE"})
    void cancelar_cuandoNoEsCancelable_lanzaEstadoTurnoInvalidoExceptionYNoGuarda(EstadoTurno estado) {
        Turno turno = crearTurno(LocalTime.of(10, 0), 30);
        turno.setId(1L);
        turno.setEstado(estado);
        when(turnoRepository.buscarPorId(1L)).thenReturn(turno);

        assertThrows(EstadoTurnoInvalidoException.class, () -> turnoService.cancelar(1L));
        assertThat(turno.getEstado()).isEqualTo(estado);
        verify(turnoRepository, never()).guardar(any(Turno.class));
    }

    @Test
    void cancelar_cuandoNoExiste_lanzaTurnoNotFoundException() {
        when(turnoRepository.buscarPorId(99L)).thenReturn(null);

        assertThrows(TurnoNotFoundException.class, () -> turnoService.cancelar(99L));
        verify(turnoRepository, never()).guardar(any(Turno.class));
    }

    @Test
    void eliminarPorId_cuandoNoExiste_lanzaTurnoNotFoundException() {
        when(turnoRepository.buscarPorId(99L)).thenReturn(null);

        assertThrows(TurnoNotFoundException.class, () -> turnoService.eliminarPorId(99L));
        verify(turnoRepository, never()).eliminarPorId(anyLong());
    }

    /** La mascota y los veterinarios del turno existen en sus microservicios. */
    private void referenciasExistentes() {
        when(mascotaService.existsById(anyLong())).thenReturn(true);
        when(veterinarioService.existsById(anyLong())).thenReturn(true);
    }

    private Turno crearTurno(LocalTime hora, int duracionMinutos) {
        Turno turno = new Turno();
        turno.setFecha(FECHA);
        turno.setHora(hora);
        turno.setMotivo("Control anual");
        turno.setDuracionMinutos(duracionMinutos);
        turno.setMascotaId(1L);
        return turno;
    }
}
