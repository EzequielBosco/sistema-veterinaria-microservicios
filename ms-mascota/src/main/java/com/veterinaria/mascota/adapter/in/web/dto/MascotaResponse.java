package com.veterinaria.mascota.adapter.in.web.dto;

import com.veterinaria.mascota.domain.model.SexoMascota;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de SALIDA de Mascota: lo que la API devuelve al cliente.
 * Este es el JSON que ms-turno consumira via Feign.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MascotaResponse {

    @Schema(description = "ID único de la mascota", example = "1")
    private Long id;

    @Schema(description = "Nombre de la mascota", example = "Firulais")
    private String nombre;

    @Schema(description = "Especie de la mascota", example = "Perro")
    private String especie;

    @Schema(description = "Raza de la mascota", example = "Labrador")
    private String raza;

    @Schema(description = "Color del pelaje de la mascota", example = "Dorado")
    private String color;

    @Schema(description = "Sexo de la mascota", example = "MACHO")
    private SexoMascota sexo;

    @Schema(description = "Fecha de nacimiento de la mascota", example = "2020-03-10")
    private LocalDate fechaNacimiento;

    @Schema(description = "ID del dueño de la mascota", example = "1")
    private Long duenioId;
}
