package com.veterinaria.duenio.domain.exception;

public class DuplicatedCedulaException extends RuntimeException {

    public DuplicatedCedulaException(String cedula) {
        super("La cedula/DNI " + cedula + " ya esta registrada");
    }
}
