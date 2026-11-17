package com.veterinaria.mascota.adapter.out.feign;

import lombok.Data;

/**
 * DTO de CONSUMO: copia reducida del JSON que devuelve ms-duenio, solo con los campos que
 * ms-mascota necesita. Es una clase PROPIA de ms-mascota: no se importa nada de ms-duenio.
 * Los campos extra del JSON (cedula, email, telefono) se ignoran.
 */
@Data
public class DuenioResponse {
    private Long id;
    private String nombre;
    private String apellido;
}
