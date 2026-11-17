package com.veterinaria.veterinario.domain.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("El email " + email + " ya esta registrado");
    }
}
