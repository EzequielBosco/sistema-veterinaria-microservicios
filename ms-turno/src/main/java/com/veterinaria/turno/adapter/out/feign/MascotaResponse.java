package com.veterinaria.turno.adapter.out.feign;

import lombok.Data;

/**
 * DTO de CONSUMO: copia reducida del JSON que devuelve ms-mascota, solo con los campos que
 * ms-turno necesita. Es una clase PROPIA de ms-turno: no se importa nada de ms-mascota
 * (los microservicios no comparten codigo). Los campos extra del JSON se ignoran.
 */
@Data
public class MascotaResponse {
    private Long id;
    private String nombre;
    private Long duenioId;
}
