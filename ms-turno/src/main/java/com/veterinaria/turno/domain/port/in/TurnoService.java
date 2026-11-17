package com.veterinaria.turno.domain.port.in;

import com.veterinaria.turno.domain.model.Participacion;
import com.veterinaria.turno.domain.model.Turno;

import java.time.LocalDate;
import java.util.List;

/** Puerto de entrada: casos de uso de Turno. */
public interface TurnoService {
    List<Turno> listarTodos();
    List<Turno> buscarAgenda(Long veterinarioId, LocalDate fecha);
    Turno buscarPorId(Long id);
    List<Turno> buscarPorVeterinarioId(Long veterinarioId);
    List<Turno> buscarPorMascotaId(Long mascotaId);
    List<Participacion> buscarParticipaciones(Long turnoId);
    Turno registrar(Turno turno);
    Turno actualizar(Long id, Turno turno);
    Turno cancelar(Long id);
    void eliminarPorId(Long id);
}
