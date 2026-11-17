package com.veterinaria.mascota.domain.exception;

/**
 * ms-duenio no responde (caido, timeout o circuito abierto).
 * La lanza el fallback del DuenioFeignAdapter; el GlobalExceptionHandler la traduce a 503.
 */
public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
