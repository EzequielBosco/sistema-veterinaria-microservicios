package com.veterinaria.turno.adapter.in.web.dto;

import com.veterinaria.turno.domain.model.RolVeterinario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Veterinario asignado a un turno dentro del request, con su rol. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnoVeterinarioRequest {

    @Schema(description = "ID del veterinario a asignar al turno", example = "2")
    @NotNull(message = "El id del veterinario es obligatorio")
    @Positive(message = "El id del veterinario debe ser positivo")
    private Long veterinarioId;

    @Schema(description = "Rol del veterinario en el turno", example = "PRINCIPAL")
    @NotNull(message = "El rol del veterinario es obligatorio")
    private RolVeterinario rol;
}
