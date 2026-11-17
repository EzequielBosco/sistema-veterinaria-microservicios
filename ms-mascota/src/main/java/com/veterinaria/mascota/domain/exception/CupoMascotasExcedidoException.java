package com.veterinaria.mascota.domain.exception;

public class CupoMascotasExcedidoException extends RuntimeException {
    public CupoMascotasExcedidoException(Long duenioId, long cantidadMascotas, int limite) {
        super("El duenio con id " + duenioId + " ya tiene " + cantidadMascotas
                + " mascotas activas. El limite es de " + limite + " mascotas por duenio");
    }
}
