package com.veterinaria.veterinario.domain.exception;

public class DuplicateMatriculaException extends RuntimeException {
    public DuplicateMatriculaException(String matricula) {
        super("La matricula " + matricula + " ya esta registrada");
    }
}
