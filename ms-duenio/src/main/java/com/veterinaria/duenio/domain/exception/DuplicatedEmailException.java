package com.veterinaria.duenio.domain.exception;

public class DuplicatedEmailException extends RuntimeException {

    public DuplicatedEmailException(String email) {
        super("El email " + email + " ya esta registrado");
    }
}
