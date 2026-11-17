package com.veterinaria.duenio.domain.exception;

public class DuplicateCedulaException extends RuntimeException {
    public DuplicateCedulaException(String cedula) {
        super("La cedula/DNI " + cedula + " ya esta registrada");
    }
}
