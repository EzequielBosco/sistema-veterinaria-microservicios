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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST. Solo conoce el puerto DuenioService (la interface)
 * y los DTOs: no importa DuenioServiceImpl ni domain.model.
 */
@Tag(name = "Dueños", description = "Gestión de dueños")
@RestController
@RequestMapping("/api/duenios")
@RequiredArgsConstructor
public class DuenioController {

    private final DuenioService duenioService;
    private final DuenioMapper duenioMapper;

    @Operation(summary = "Listar todos los dueños", description = "Retorna la lista completa de dueños registrados")
    @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<DuenioResponse>> getAllDuenios() {
        return ResponseEntity.ok(duenioMapper.toResponseList(duenioService.findAll()));
    }

    @Operation(summary = "Obtener dueño por ID", description = "Retorna los datos de un dueño a partir de su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dueño encontrado"),
        @ApiResponse(responseCode = "404", description = "Dueño no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<DuenioResponse> getDuenioById(@PathVariable Long id) {
        return ResponseEntity.ok(duenioMapper.toResponse(duenioService.findById(id)));
    }

    @Operation(summary = "Obtener dueño por cédula", description = "Retorna los datos de un dueño buscando por su número de cédula o DNI")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dueño encontrado"),
        @ApiResponse(responseCode = "404", description = "Dueño no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/cedula/{cedula}")
    public ResponseEntity<DuenioResponse> getDuenioByCedula(@PathVariable String cedula) {
        return ResponseEntity.ok(duenioMapper.toResponse(duenioService.findByCedula(cedula)));
    }

    @Operation(summary = "Obtener dueño por email", description = "Retorna los datos de un dueño buscando por su correo electrónico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dueño encontrado"),
        @ApiResponse(responseCode = "404", description = "Dueño no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/email/{email}")
    public ResponseEntity<DuenioResponse> getDuenioByEmail(@PathVariable String email) {
        return ResponseEntity.ok(duenioMapper.toResponse(duenioService.findByEmail(email)));
    }

    @Operation(summary = "Buscar dueños por nombre y apellido", description = "Retorna los dueños cuyo nombre y apellido coincidan con los parámetros de búsqueda")
    @ApiResponse(responseCode = "200", description = "Búsqueda realizada exitosamente")
    @GetMapping("/search")
    public ResponseEntity<List<DuenioResponse>> searchDuenios(
            @RequestParam String nombre,
            @RequestParam String apellido) {
        return ResponseEntity.ok(duenioMapper.toResponseList(duenioService.search(nombre, apellido)));
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
    public ResponseEntity<DuenioResponse> createDuenio(@Valid @RequestBody DuenioRequest duenioRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(duenioMapper.toResponse(duenioService.save(duenioMapper.toDomain(duenioRequest))));
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
    public ResponseEntity<DuenioResponse> updateDuenio(
            @PathVariable Long id,
            @Valid @RequestBody DuenioRequest duenioRequest) {
        return ResponseEntity.ok(duenioMapper.toResponse(duenioService.update(id, duenioMapper.toDomain(duenioRequest))));
    }

    @Operation(summary = "Eliminar dueño", description = "Elimina un dueño del sistema a partir de su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Dueño eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Dueño no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDuenio(@PathVariable Long id) {
        duenioService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
