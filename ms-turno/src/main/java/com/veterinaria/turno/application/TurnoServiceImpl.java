package com.veterinaria.turno.application;

import com.veterinaria.turno.domain.exception.BadRequestException;
import com.veterinaria.turno.domain.exception.EstadoTurnoInvalidoException;
import com.veterinaria.turno.domain.exception.TurnoNotFoundException;
import com.veterinaria.turno.domain.exception.TurnoSuperpuestoException;
import com.veterinaria.turno.domain.model.EstadoTurno;
import com.veterinaria.turno.domain.model.Participacion;
import com.veterinaria.turno.domain.model.RolVeterinario;
import com.veterinaria.turno.domain.model.Turno;
import com.veterinaria.turno.domain.port.in.TurnoService;
import com.veterinaria.turno.domain.port.out.TurnoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lógica de negocio de Turno. Depende SOLO de puertos (in/out), nunca de adapter.*
 * Las reglas de veterinarios por turno y de superposicion se validan localmente:
 * turnos y participaciones viven en la misma BD.
 * La existencia de la mascota y de los veterinarios se validara via Feign (Sprint 11).
 */
@Service
@RequiredArgsConstructor
public class TurnoServiceImpl implements TurnoService {

    private static final Set<EstadoTurno> ESTADOS_CANCELABLES = Set.of(EstadoTurno.PENDIENTE, EstadoTurno.CONFIRMADO);

    private final TurnoRepositoryPort turnoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Turno> listarTodos() {
        return turnoRepository.buscarTodos();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Turno> buscarAgenda(Long veterinarioId, LocalDate fecha) {
        if (veterinarioId == null || fecha == null) {
            throw new BadRequestException("Debe indicar veterinarioId y fecha");
        }
        return turnoRepository.buscarPorVeterinarioYFecha(veterinarioId, fecha);
    }

    @Override
    @Transactional(readOnly = true)
    public Turno buscarPorId(Long id) {
        Turno turno = turnoRepository.buscarPorId(id);
        if (turno == null) {
            throw new TurnoNotFoundException(id);
        }
        return turno;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Turno> buscarPorVeterinarioId(Long veterinarioId) {
        return turnoRepository.buscarPorVeterinarioId(veterinarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Turno> buscarPorMascotaId(Long mascotaId) {
        return turnoRepository.buscarPorMascotaId(mascotaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Participacion> buscarParticipaciones(Long turnoId) {
        return buscarPorId(turnoId).getParticipaciones().stream()
                .sorted(Comparator
                        .comparing(Participacion::getRol)
                        .thenComparing(Participacion::getVeterinarioId))
                .toList();
    }

    @Override
    @Transactional
    public Turno registrar(Turno turno) {
        validarTurno(turno);
        validarDisponibilidad(turno, null);
        // Regla de negocio: todo turno nace PENDIENTE
        turno.setEstado(EstadoTurno.PENDIENTE);
        return turnoRepository.guardar(turno);
    }

    @Override
    @Transactional
    public Turno actualizar(Long id, Turno turno) {
        validarTurno(turno);
        Turno existente = buscarPorId(id);
        validarDisponibilidad(turno, id);

        existente.setFecha(turno.getFecha());
        existente.setHora(turno.getHora());
        existente.setMotivo(turno.getMotivo());
        existente.setDuracionMinutos(turno.getDuracionMinutos());
        existente.setMascotaId(turno.getMascotaId());

        existente.getParticipaciones().clear();
        turno.getParticipaciones().forEach(participacion ->
                existente.agregarParticipacion(participacion.getVeterinarioId(), participacion.getRol()));
        return turnoRepository.guardar(existente);
    }

    @Override
    @Transactional
    public Turno cancelar(Long id) {
        Turno turno = buscarPorId(id);
        // Regla de negocio: solo se cancela un turno que todavia no ocurrio ni fue cancelado
        if (!ESTADOS_CANCELABLES.contains(turno.getEstado())) {
            throw new EstadoTurnoInvalidoException(id, turno.getEstado(), "cancelar");
        }
        turno.setEstado(EstadoTurno.CANCELADO);
        return turnoRepository.guardar(turno);
    }

    @Override
    @Transactional
    public void eliminarPorId(Long id) {
        buscarPorId(id); // valida que exista antes de eliminar
        turnoRepository.eliminarPorId(id);
    }

    private void validarTurno(Turno turno) {
        if (turno.getFecha() == null
                || turno.getHora() == null
                || turno.getDuracionMinutos() == null
                || turno.getMascotaId() == null
                || turno.getMotivo() == null
                || turno.getMotivo().isBlank()) {
            throw new BadRequestException("Debe indicar fecha, hora, duracion, motivo y mascotaId");
        }
        if (turno.getDuracionMinutos() < 10) {
            throw new BadRequestException("La duracion debe ser de al menos 10 minutos");
        }

        Set<Participacion> participaciones = turno.getParticipaciones();
        if (participaciones == null || participaciones.isEmpty()) {
            throw new BadRequestException("Debe indicar al menos un veterinario");
        }
        if (participaciones.stream().anyMatch(participacion -> participacion.getVeterinarioId() == null
                || participacion.getRol() == null)) {
            throw new BadRequestException("Debe indicar veterinarioId y rol para cada veterinario");
        }

        long cantidadVeterinariosUnicos = participaciones.stream()
                .map(Participacion::getVeterinarioId)
                .distinct()
                .count();
        if (cantidadVeterinariosUnicos != participaciones.size()) {
            throw new BadRequestException("No se puede repetir el mismo veterinario en un turno");
        }

        long cantidadPrincipales = participaciones.stream()
                .filter(participacion -> RolVeterinario.PRINCIPAL.equals(participacion.getRol()))
                .count();
        if (cantidadPrincipales != 1) {
            throw new BadRequestException("Debe indicar exactamente un veterinario con rol PRINCIPAL");
        }
    }

    private void validarDisponibilidad(Turno turno, Long turnoId) {
        for (Participacion participacion : turno.getParticipaciones()) {
            buscarTurnoSuperpuesto(participacion.getVeterinarioId(), turno, turnoId)
                    .ifPresent(conflictivo -> {
                        throw new TurnoSuperpuestoException(participacion.getVeterinarioId(), conflictivo);
                    });
        }
    }

    private Optional<Turno> buscarTurnoSuperpuesto(Long veterinarioId, Turno turno, Long turnoId) {
        LocalTime inicioNuevo = turno.getHora();
        LocalTime finNuevo = inicioNuevo.plusMinutes(turno.getDuracionMinutos());

        return turnoRepository.buscarPorVeterinarioYFecha(veterinarioId, turno.getFecha())
                .stream()
                .filter(existente -> turnoId == null || !turnoId.equals(existente.getId()))
                // Un turno cancelado libera el horario del veterinario
                .filter(existente -> existente.getEstado() != EstadoTurno.CANCELADO)
                .filter(existente -> {
                    LocalTime inicioExistente = existente.getHora();
                    LocalTime finExistente = inicioExistente.plusMinutes(existente.getDuracionMinutos());
                    return inicioExistente.isBefore(finNuevo) && inicioNuevo.isBefore(finExistente);
                })
                .findFirst();
    }
}
