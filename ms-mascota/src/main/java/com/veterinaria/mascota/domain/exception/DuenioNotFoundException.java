package com.veterinaria.mascota.domain.exception;

/**
 * El duenio referenciado por la mascota no existe en ms-duenio.
 * Es una excepcion PROPIA de ms-mascota: los microservicios no comparten codigo de dominio.
 */
public class DuenioNotFoundException extends RuntimeException {
    public DuenioNotFoundException(Long id) {
        super("No existe un duenio con id " + id);
    }
}
