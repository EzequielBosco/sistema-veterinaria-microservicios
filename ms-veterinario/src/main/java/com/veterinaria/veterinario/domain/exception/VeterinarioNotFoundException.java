package com.veterinaria.veterinario.domain.exception;

public class VeterinarioNotFoundException extends RuntimeException {
    public VeterinarioNotFoundException(Long id) {
        super("No existe un veterinario con id " + id);
    }
}
