package com.veterinaria.veterinario.adapter.in.web;

import com.veterinaria.veterinario.adapter.in.web.dto.VeterinarioMapper;
import com.veterinaria.veterinario.adapter.in.web.dto.VeterinarioRequest;
import com.veterinaria.veterinario.adapter.in.web.dto.VeterinarioResponse;
import com.veterinaria.veterinario.domain.port.in.VeterinarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Adaptador de ENTRADA: traduce HTTP -> llamadas al dominio.
 * El endpoint GET /api/veterinarios/{id} es el que ms-turno consume via Feign para validar que exista.
 */
@Tag(name = "Veterinarios", description = "Gestión de veterinarios")
@RestController
@RequestMapping("/api/veterinarios")
@RequiredArgsConstructor
public class VeterinarioController {

    private final VeterinarioService veterinarioService;

    @Operation(summary = "Listar todos los veterinarios", description = "Retorna la lista completa de veterinarios registrados en el sistema")
    @GetMapping
    public ResponseEntity<List<VeterinarioResponse>> listarTodos() {
        List<VeterinarioResponse> respuesta = veterinarioService.listarTodos().stream()
                .map(VeterinarioMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Obtener veterinario por ID", description = "Retorna los datos de un veterinario a partir de su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Veterinario encontrado"),
        @ApiResponse(responseCode = "404", description = "Veterinario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<VeterinarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(VeterinarioMapper.toResponse(veterinarioService.buscarPorId(id)));
    }

    @Operation(
        summary = "Listar veterinarios por especialidad",
        description = "Retorna los veterinarios de una especialidad, sin distinguir mayúsculas de minúsculas. Lista vacía si no hay ninguno"
    )
    @GetMapping("/especialidad/{especialidad}")
    public ResponseEntity<List<VeterinarioResponse>> buscarPorEspecialidad(@PathVariable String especialidad) {
        List<VeterinarioResponse> respuesta = veterinarioService.buscarPorEspecialidad(especialidad).stream()
                .map(VeterinarioMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @Operation(
        summary = "Registrar un nuevo veterinario",
        description = "Crea un nuevo veterinario. El email y la matrícula deben ser únicos en el sistema"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Veterinario creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos (falla de validación)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "La matrícula o el email ya están registrados",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<VeterinarioResponse> registrar(@Valid @RequestBody VeterinarioRequest request) {
        VeterinarioResponse creado = VeterinarioMapper.toResponse(veterinarioService.registrar(VeterinarioMapper.toEntity(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(
        summary = "Actualizar veterinario",
        description = "Modifica los datos de un veterinario existente. El email y la matrícula deben ser únicos en el sistema"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Veterinario actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos (falla de validación)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Veterinario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "La matrícula o el email ya pertenecen a otro veterinario",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<VeterinarioResponse> actualizar(@PathVariable Long id,
                                                          @Valid @RequestBody VeterinarioRequest request) {
        return ResponseEntity.ok(VeterinarioMapper.toResponse(veterinarioService.actualizar(id, VeterinarioMapper.toEntity(request))));
    }

    @Operation(summary = "Eliminar veterinario", description = "Elimina un veterinario del sistema a partir de su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Veterinario eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Veterinario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Long id) {
        veterinarioService.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }
}
