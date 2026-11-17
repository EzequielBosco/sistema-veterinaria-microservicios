package com.veterinaria.turno.domain.exception;

import com.veterinaria.turno.domain.model.Turno;

public class TurnoSuperpuestoException extends RuntimeException {
    public TurnoSuperpuestoException(Long veterinarioId, Turno conflictivo) {
        super("El veterinario con id " + veterinarioId
                + " ya tiene asignado el turno con id " + conflictivo.getId()
                + " el " + conflictivo.getFecha()
                + " de " + conflictivo.getHora()
                + " a " + conflictivo.getHora().plusMinutes(conflictivo.getDuracionMinutos()));
    }
}
