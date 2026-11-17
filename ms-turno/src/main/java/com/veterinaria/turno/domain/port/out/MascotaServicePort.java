package com.veterinaria.turno.domain.port.out;

/**
 * Puerto de salida: "necesito saber si una mascota existe".
 * El dominio depende de esta interface, NO del cliente Feign (que es un adaptador):
 * no sabe nada de HTTP, solo pide un booleano.
 */
public interface MascotaServicePort {
    boolean existsById(Long id);
}
