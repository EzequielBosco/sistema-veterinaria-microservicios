package com.veterinaria.turno.domain.exception;

import com.veterinaria.turno.domain.model.EstadoTurno;

/** La operacion no se puede aplicar al turno por el estado en el que se encuentra. */
public class EstadoTurnoInvalidoException extends RuntimeException {
    public EstadoTurnoInvalidoException(Long turnoId, EstadoTurno estado, String operacion) {
        super("No se puede " + operacion + " el turno con id " + turnoId
                + " porque esta en estado " + estado);
    }
}
