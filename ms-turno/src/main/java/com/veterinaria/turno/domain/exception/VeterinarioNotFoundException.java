package com.veterinaria.turno.domain.exception;

/**
 * El veterinario referenciado por el turno no existe en ms-veterinario.
 * Es una excepcion PROPIA de ms-turno: los microservicios no comparten codigo de dominio.
 */
public class VeterinarioNotFoundException extends RuntimeException {
    public VeterinarioNotFoundException(Long id) {
        super("No existe un veterinario con id " + id);
    }
}
