package com.veterinaria.mascota.domain.exception;

public class MascotaNotFoundException extends RuntimeException {
    public MascotaNotFoundException(Long id) {
        super("No existe una mascota con id " + id);
    }
}
