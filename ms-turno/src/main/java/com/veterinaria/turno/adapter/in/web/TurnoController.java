package com.veterinaria.turno.adapter.in.web;

import com.veterinaria.turno.adapter.in.web.dto.TurnoMapper;
import com.veterinaria.turno.adapter.in.web.dto.TurnoRequest;
import com.veterinaria.turno.adapter.in.web.dto.TurnoResponse;
import com.veterinaria.turno.adapter.in.web.dto.TurnoVeterinarioResponse;
import com.veterinaria.turno.adapter.in.web.dto.VeterinarioTurnoResponse;
import com.veterinaria.turno.domain.port.in.TurnoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Adaptador de ENTRADA: traduce HTTP -> llamadas al dominio.
 * Depende de la interface TurnoService (puerto de entrada), NUNCA de TurnoServiceImpl.
 */
@Tag(name = "Turnos", description = "Gestión de turnos y agenda de la veterinaria")
@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @Operation(
        summary = "Listar turnos / consultar agenda",
        description = "Retorna todos los turnos. Si se desea filtrar la agenda, deben proveerse ambos parámetros 'veterinarioId' y 'fecha' juntos; pasar solo uno devuelve 400"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente"),
        @ApiResponse(responseCode = "400", description = "Se indicó solo uno de los parámetros de agenda",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<TurnoResponse>> listarTodos(
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        var turnos = (veterinarioId != null || fecha != null)
                ? turnoService.buscarAgenda(veterinarioId, fecha)
                : turnoService.listarTodos();
        return ResponseEntity.ok(turnos.stream().map(TurnoMapper::toResponse).toList());
    }

    @Operation(summary = "Obtener turno por ID", description = "Retorna los datos de un turno a partir de su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Turno encontrado"),
        @ApiResponse(responseCode = "404", description = "Turno no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<TurnoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(TurnoMapper.toResponse(turnoService.buscarPorId(id)));
    }

    @Operation(summary = "Listar veterinarios de un turno", description = "Retorna los veterinarios asignados a un turno junto con su rol")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente"),
        @ApiResponse(responseCode = "404", description = "Turno no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/veterinarios")
    public ResponseEntity<List<TurnoVeterinarioResponse>> buscarVeterinarios(@PathVariable Long id) {
        List<TurnoVeterinarioResponse> respuesta = turnoService.buscarParticipaciones(id).stream()
                .map(TurnoMapper::toVeterinarioResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Listar turnos de un veterinario", description = "Retorna todos los turnos asignados a un veterinario junto con el rol que desempeña en cada uno")
    @GetMapping("/veterinario/{veterinarioId}")
    public ResponseEntity<List<VeterinarioTurnoResponse>> buscarPorVeterinarioId(@PathVariable Long veterinarioId) {
        List<VeterinarioTurnoResponse> respuesta = turnoService.buscarPorVeterinarioId(veterinarioId).stream()
                .map(turno -> TurnoMapper.toVeterinarioTurnoResponse(turno, veterinarioId))
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Listar turnos de una mascota", description = "Retorna el historial de turnos de una mascota ordenado por fecha y hora")
    @GetMapping("/mascota/{mascotaId}")
    public ResponseEntity<List<TurnoResponse>> buscarPorMascotaId(@PathVariable Long mascotaId) {
        List<TurnoResponse> respuesta = turnoService.buscarPorMascotaId(mascotaId).stream()
                .map(TurnoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @Operation(
        summary = "Registrar un nuevo turno",
        description = "Crea un nuevo turno. La fecha y hora deben ser posteriores al momento actual. " +
            "La duración mínima es de 10 minutos. Debe indicarse exactamente un veterinario con rol PRINCIPAL. " +
            "No se puede repetir el mismo veterinario en el turno. No se permiten turnos superpuestos para un mismo veterinario"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Turno creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o reglas de veterinarios incumplidas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Algún veterinario ya tiene un turno superpuesto",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TurnoResponse> registrar(@Valid @RequestBody TurnoRequest request) {
        TurnoResponse creado = TurnoMapper.toResponse(turnoService.registrar(TurnoMapper.toEntity(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(
        summary = "Actualizar turno",
        description = "Modifica los datos de un turno existente. La fecha y hora deben ser posteriores al momento actual. " +
            "La duración mínima es de 10 minutos. Debe indicarse exactamente un veterinario con rol PRINCIPAL. " +
            "No se puede repetir el mismo veterinario en el turno. No se permiten turnos superpuestos para un mismo veterinario"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Turno actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o reglas de veterinarios incumplidas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Turno no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Algún veterinario ya tiene un turno superpuesto",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<TurnoResponse> actualizar(@PathVariable Long id,
                                                    @Valid @RequestBody TurnoRequest request) {
        return ResponseEntity.ok(TurnoMapper.toResponse(turnoService.actualizar(id, TurnoMapper.toEntity(request))));
    }

    @Operation(
        summary = "Cancelar turno",
        description = "Pasa el turno a estado CANCELADO y libera el horario de sus veterinarios. " +
            "Solo se pueden cancelar turnos en estado PENDIENTE o CONFIRMADO"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Turno cancelado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Turno no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "El turno ya está cancelado, atendido o ausente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<TurnoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(TurnoMapper.toResponse(turnoService.cancelar(id)));
    }

    @Operation(summary = "Eliminar turno", description = "Elimina un turno del sistema a partir de su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Turno eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Turno no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Long id) {
        turnoService.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }
}
