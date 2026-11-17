package com.veterinaria.mascota.domain.port.out;

/**
 * Puerto de salida: "necesito saber si un duenio existe".
 * El dominio depende de esta interface, NO del cliente Feign (que es un adaptador):
 * no sabe nada de HTTP, solo pide un booleano.
 */
public interface DuenioServicePort {
    boolean existsById(Long id);
}
