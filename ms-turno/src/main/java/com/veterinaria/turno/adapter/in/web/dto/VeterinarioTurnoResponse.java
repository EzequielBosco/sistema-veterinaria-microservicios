package com.veterinaria.turno.adapter.in.web.dto;

import com.veterinaria.turno.domain.model.RolVeterinario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/** Turno visto desde la agenda de un veterinario, con el rol que cumple en el. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VeterinarioTurnoResponse {

    @Schema(description = "ID único del turno", example = "1")
    private Long id;

    @Schema(description = "Fecha del turno", example = "2026-12-15")
    private LocalDate fecha;

    @Schema(description = "Hora del turno", example = "10:30")
    private LocalTime hora;

    @Schema(description = "Motivo de la consulta", example = "Control anual y vacunación")
    private String motivo;

    @Schema(description = "Duración del turno en minutos", example = "30")
    private Integer duracionMinutos;

    @Schema(description = "ID de la mascota que asiste al turno", example = "1")
    private Long mascotaId;

    @Schema(description = "Rol del veterinario en el turno", example = "PRINCIPAL")
    private RolVeterinario rol;
}
