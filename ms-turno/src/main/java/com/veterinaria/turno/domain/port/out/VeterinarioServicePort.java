package com.veterinaria.turno.domain.port.out;

/**
 * Puerto de salida: "necesito saber si un veterinario existe".
 * El dominio depende de esta interface, NO del cliente Feign.
 */
public interface VeterinarioServicePort {
    boolean existsById(Long id);
}
