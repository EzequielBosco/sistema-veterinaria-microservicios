package com.veterinaria.duenio.domain.exception;

public class DuenioNotFoundException extends RuntimeException {
    public DuenioNotFoundException(Long id) {
        super("No existe un duenio con id " + id);
    }

    public DuenioNotFoundException(String mensaje) {
        super(mensaje);
    }
}
