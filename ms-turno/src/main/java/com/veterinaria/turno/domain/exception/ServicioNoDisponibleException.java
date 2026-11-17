package com.veterinaria.turno.domain.exception;

/**
 * Un microservicio del que depende ms-turno no responde (caido, timeout o circuito abierto).
 * La lanzan los metodos de fallback de los adaptadores Feign; el GlobalExceptionHandler la traduce a 503.
 */
public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
