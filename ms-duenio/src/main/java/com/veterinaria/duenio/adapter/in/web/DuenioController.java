package com.veterinaria.duenio.adapter.in.web;

import com.veterinaria.duenio.adapter.in.web.dto.DuenioMapper;
import com.veterinaria.duenio.adapter.in.web.dto.DuenioRequest;
import com.veterinaria.duenio.adapter.in.web.dto.DuenioResponse;
import com.veterinaria.duenio.domain.port.in.DuenioService;
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
 * Depende de la interface DuenioService (puerto de entrada), NUNCA de DuenioServiceImpl,
 * y no importa domain.model: solo maneja DTOs y delega la traduccion en DuenioMapper.
 */
@Tag(name = "Dueños", description = "Gestión de dueños")
@RestController
@RequestMapping("/api/duenios")
@RequiredArgsConstructor
public class DuenioController {

    private final DuenioService duenioService;

    @Operation(summary = "Listar todos los dueños", description = "Retorna la lista completa de dueños registrados")
    @GetMapping
    public ResponseEntity<List<DuenioResponse>> listarTodos() {
        List<DuenioResponse> respuesta = duenioService.listarTodos().stream()
                .map(DuenioMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Obtener dueño por ID", description = "Retorna los datos de un dueño a partir de su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dueño encontrado"),
        @ApiResponse(responseCode = "404", description = "Dueño no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<DuenioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(DuenioMapper.toResponse(duenioService.buscarPorId(id)));
    }

    @Operation(summary = "Obtener dueño por cédula", description = "Retorna los datos de un dueño buscando por su número de cédula o DNI")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dueño encontrado"),
        @ApiResponse(responseCode = "404", description = "Dueño no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/cedula/{cedula}")
    public ResponseEntity<DuenioResponse> buscarPorCedula(@PathVariable String cedula) {
        return ResponseEntity.ok(DuenioMapper.toResponse(duenioService.buscarPorCedula(cedula)));
    }

    @Operation(summary = "Obtener dueño por email", description = "Retorna los datos de un dueño buscando por su correo electrónico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dueño encontrado"),
        @ApiResponse(responseCode = "404", description = "Dueño no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/email/{email}")
    public ResponseEntity<DuenioResponse> buscarPorEmail(@PathVariable String email) {
        return ResponseEntity.ok(DuenioMapper.toResponse(duenioService.buscarPorEmail(email)));
    }

    @Operation(summary = "Buscar dueños por nombre y apellido", description = "Retorna los dueños cuyo nombre y apellido coincidan con los parámetros de búsqueda")
    @GetMapping("/search")
    public ResponseEntity<List<DuenioResponse>> buscarPorNombreYApellido(@RequestParam String nombre,
                                                                         @RequestParam String apellido) {
        List<DuenioResponse> respuesta = duenioService.buscarPorNombreYApellido(nombre, apellido).stream()
                .map(DuenioMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @Operation(
        summary = "Registrar un nuevo dueño",
        description = "Crea un nuevo dueño. La cédula debe tener entre 7 y 8 dígitos. El email y la cédula deben ser únicos en el sistema"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Dueño creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos (falla de validación)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "El email o la cédula ya están registrados",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DuenioResponse> registrar(@Valid @RequestBody DuenioRequest request) {
        DuenioResponse creado = DuenioMapper.toResponse(duenioService.registrar(DuenioMapper.toEntity(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(
        summary = "Actualizar dueño",
        description = "Modifica los datos de un dueño existente. La cédula debe tener entre 7 y 8 dígitos. El email y la cédula deben ser únicos en el sistema"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dueño actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos (falla de validación)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Dueño no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "El email o la cédula ya pertenecen a otro dueño",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<DuenioResponse> actualizar(@PathVariable Long id,
                                                     @Valid @RequestBody DuenioRequest request) {
        return ResponseEntity.ok(DuenioMapper.toResponse(duenioService.actualizar(id, DuenioMapper.toEntity(request))));
    }

    @Operation(summary = "Eliminar dueño", description = "Elimina un dueño del sistema a partir de su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Dueño eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Dueño no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Long id) {
        duenioService.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }
}
