package com.veterinaria.turno.domain.exception;

public class TurnoNotFoundException extends RuntimeException {
    public TurnoNotFoundException(Long id) {
        super("No existe un turno con id " + id);
    }
}
