package com.veterinaria.turno.domain.exception;

/**
 * La mascota referenciada por el turno no existe en ms-mascota.
 * Es una excepcion PROPIA de ms-turno: los microservicios no comparten codigo de dominio.
 */
public class MascotaNotFoundException extends RuntimeException {
    public MascotaNotFoundException(Long id) {
        super("No existe una mascota con id " + id);
    }
}
