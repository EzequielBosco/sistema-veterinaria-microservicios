package com.veterinaria.turno.adapter.in.web.dto;

import com.veterinaria.turno.domain.model.Participacion;
import com.veterinaria.turno.domain.model.RolVeterinario;
import com.veterinaria.turno.domain.model.Turno;

import java.util.Comparator;
import java.util.List;

/**
 * Traductor entre DTOs (mundo HTTP) y la entidad de dominio (mundo interno).
 * El estado NO se mapea desde el request: lo asigna el dominio (todo turno nace PENDIENTE).
 */
public final class TurnoMapper {

    private TurnoMapper() {
        // clase de utilidades: no se instancia
    }

    /** DTO de entrada -> entidad de dominio (sin id ni estado: los asigna el sistema). */
    public static Turno toEntity(TurnoRequest req) {
        Turno turno = new Turno();
        turno.setFecha(req.getFecha());
        turno.setHora(req.getHora());
        turno.setMotivo(req.getMotivo());
        turno.setDuracionMinutos(req.getDuracionMinutos());
        turno.setMascotaId(req.getMascotaId());
        if (req.getVeterinarios() != null) {
            req.getVeterinarios().forEach(veterinario ->
                    turno.agregarParticipacion(veterinario.getVeterinarioId(), veterinario.getRol()));
        }
        return turno;
    }

    /** Entidad de dominio -> DTO de salida, con los veterinarios ordenados por rol. */
    public static TurnoResponse toResponse(Turno turno) {
        List<TurnoVeterinarioResponse> veterinarios = turno.getParticipaciones().stream()
                .sorted(Comparator
                        .comparing(Participacion::getRol)
                        .thenComparing(Participacion::getVeterinarioId))
                .map(TurnoMapper::toVeterinarioResponse)
                .toList();
        return new TurnoResponse(
                turno.getId(),
                turno.getFecha(),
                turno.getHora(),
                turno.getMotivo(),
                turno.getDuracionMinutos(),
                turno.getEstado(),
                turno.getMascotaId(),
                veterinarios
        );
    }

    public static TurnoVeterinarioResponse toVeterinarioResponse(Participacion participacion) {
        return new TurnoVeterinarioResponse(participacion.getVeterinarioId(), participacion.getRol());
    }

    /** Turno visto desde la agenda de un veterinario: incluye el rol que cumple en el. */
    public static VeterinarioTurnoResponse toVeterinarioTurnoResponse(Turno turno, Long veterinarioId) {
        RolVeterinario rol = turno.getParticipaciones().stream()
                .filter(participacion -> veterinarioId.equals(participacion.getVeterinarioId()))
                .map(Participacion::getRol)
                .findFirst()
                .orElse(null);
        return new VeterinarioTurnoResponse(
                turno.getId(),
                turno.getFecha(),
                turno.getHora(),
                turno.getMotivo(),
                turno.getDuracionMinutos(),
                turno.getMascotaId(),
                rol
        );
    }
}
