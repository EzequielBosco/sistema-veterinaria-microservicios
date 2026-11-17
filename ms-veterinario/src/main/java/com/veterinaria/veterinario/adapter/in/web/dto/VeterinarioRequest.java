package com.veterinaria.veterinario.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de ENTRADA de Veterinario: lo que el cliente HTTP envia al crear/actualizar.
 * No es la entidad: no tiene id (lo genera la BD) y lleva las validaciones de @Valid.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VeterinarioRequest {

    @Schema(description = "Nombre del veterinario", example = "María")
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Schema(description = "Apellido del veterinario", example = "García")
    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @Schema(description = "Teléfono de contacto del veterinario", example = "+54 11 9876-5432")
    @NotBlank(message = "El telefono es obligatorio")
    @Pattern(regexp = "^[0-9+()\\-\\s]{6,30}$", message = "El telefono debe tener un formato valido")
    private String telefono;

    @Schema(description = "Correo electrónico del veterinario", example = "maria.garcia@clinica.com")
    @Email(message = "El email debe tener un formato valido")
    private String email;

    @Schema(description = "Número de matrícula profesional del veterinario", example = "MV-4521")
    @NotBlank(message = "La matricula es obligatoria")
    private String matricula;

    @Schema(description = "Especialidad del veterinario", example = "Clínica general")
    @NotBlank(message = "La especialidad es obligatoria")
    private String especialidad;
}
