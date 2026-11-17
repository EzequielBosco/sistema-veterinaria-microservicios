package com.veterinaria.duenio.domain.exception;

public class DuenioNotFoundException extends RuntimeException {

    public DuenioNotFoundException(String mensaje) {
        super(mensaje);
    }
}
