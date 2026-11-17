package com.veterinaria.mascota.adapter.in.web;

import com.veterinaria.mascota.adapter.in.web.dto.MascotaMapper;
import com.veterinaria.mascota.adapter.in.web.dto.MascotaRequest;
import com.veterinaria.mascota.adapter.in.web.dto.MascotaResponse;
import com.veterinaria.mascota.domain.port.in.MascotaService;
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
 * Adaptador de ENTRADA (driving adapter): traduce HTTP -> llamadas al dominio.
 * Depende de la interface MascotaService (puerto de entrada), NUNCA de MascotaServiceImpl.
 * El endpoint GET /api/mascotas/{id} es el que ms-turno consume via Feign para validar que exista.
 */
@Tag(name = "Mascotas", description = "Gestión de mascotas registradas en el sistema")
@RestController
@RequestMapping("/api/mascotas")
@RequiredArgsConstructor
public class MascotaController {

    private final MascotaService mascotaService;

    @Operation(summary = "Listar todas las mascotas", description = "Retorna la lista completa de mascotas registradas en el sistema")
    @GetMapping
    public ResponseEntity<List<MascotaResponse>> listarTodos() {
        List<MascotaResponse> respuesta = mascotaService.listarTodos().stream()
                .map(MascotaMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Obtener mascota por ID", description = "Retorna los datos de una mascota a partir de su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mascota encontrada"),
        @ApiResponse(responseCode = "404", description = "Mascota no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<MascotaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(MascotaMapper.toResponse(mascotaService.buscarPorId(id)));
    }

    @Operation(summary = "Listar mascotas de un dueño", description = "Retorna todas las mascotas registradas a nombre del dueño indicado")
    @GetMapping("/duenio/{duenioId}")
    public ResponseEntity<List<MascotaResponse>> buscarPorDuenioId(@PathVariable Long duenioId) {
        List<MascotaResponse> respuesta = mascotaService.buscarPorDuenioId(duenioId).stream()
                .map(MascotaMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @Operation(
        summary = "Registrar mascota para un dueño",
        description = "Crea una nueva mascota asociada al dueño indicado en duenioId. El sexo debe ser MACHO o HEMBRA y la fecha de nacimiento no puede ser futura. " +
            "Un dueño puede tener como máximo 5 mascotas activas"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Mascota creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o falta el duenioId",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "El dueño ya alcanzó el límite de 5 mascotas activas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<MascotaResponse> registrar(@Valid @RequestBody MascotaRequest request) {
        MascotaResponse creada = MascotaMapper.toResponse(mascotaService.registrar(MascotaMapper.toEntity(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @Operation(
        summary = "Actualizar mascota",
        description = "Modifica los datos de una mascota existente. El sexo debe ser MACHO o HEMBRA y la fecha de nacimiento no puede ser futura. " +
            "Si se cambia el dueño, el nuevo dueño no puede superar el límite de 5 mascotas activas"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mascota actualizada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos (falla de validación)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Mascota no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "El nuevo dueño ya alcanzó el límite de 5 mascotas activas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<MascotaResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody MascotaRequest request) {
        return ResponseEntity.ok(MascotaMapper.toResponse(mascotaService.actualizar(id, MascotaMapper.toEntity(request))));
    }

    @Operation(summary = "Eliminar mascota", description = "Elimina una mascota del sistema a partir de su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Mascota eliminada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Mascota no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Long id) {
        mascotaService.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }
}
