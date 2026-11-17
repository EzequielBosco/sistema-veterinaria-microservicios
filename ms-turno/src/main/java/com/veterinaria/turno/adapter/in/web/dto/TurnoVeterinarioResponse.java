package com.veterinaria.turno.adapter.in.web.dto;

import com.veterinaria.turno.domain.model.RolVeterinario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Veterinario asignado a un turno con su rol.
 * Solo el id: los datos del veterinario (nombre, especialidad) se consultan en ms-veterinario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnoVeterinarioResponse {

    @Schema(description = "ID del veterinario", example = "2")
    private Long veterinarioId;

    @Schema(description = "Rol del veterinario en el turno", example = "PRINCIPAL")
    private RolVeterinario rol;
}
