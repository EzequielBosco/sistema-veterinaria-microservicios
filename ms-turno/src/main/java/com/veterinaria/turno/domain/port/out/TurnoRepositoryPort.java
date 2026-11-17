package com.veterinaria.turno.domain.port.out;

import com.veterinaria.turno.domain.model.Turno;

import java.time.LocalDate;
import java.util.List;

/** Puerto de salida: contrato de persistencia de Turno. */
public interface TurnoRepositoryPort {
    List<Turno> buscarTodos();
    Turno buscarPorId(Long id);
    List<Turno> buscarPorVeterinarioYFecha(Long veterinarioId, LocalDate fecha);
    List<Turno> buscarPorVeterinarioId(Long veterinarioId);
    Turno guardar(Turno turno);
    void eliminarPorId(Long id);
}
