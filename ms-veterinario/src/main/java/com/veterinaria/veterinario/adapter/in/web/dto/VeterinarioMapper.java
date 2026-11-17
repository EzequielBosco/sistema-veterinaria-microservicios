package com.veterinaria.veterinario.adapter.in.web.dto;

import com.veterinaria.veterinario.domain.model.Veterinario;

/**
 * Traductor entre DTOs (mundo HTTP) y la entidad de dominio (mundo interno).
 * Mapeo manual a proposito. Vive en el adaptador de entrada.
 */
public final class VeterinarioMapper {

    private VeterinarioMapper() {
        // clase de utilidades: no se instancia
    }

    /** DTO de entrada -> entidad de dominio (sin id: lo asigna la BD). */
    public static Veterinario toEntity(VeterinarioRequest req) {
        Veterinario veterinario = new Veterinario();
        veterinario.setNombre(req.getNombre());
        veterinario.setApellido(req.getApellido());
        veterinario.setTelefono(req.getTelefono());
        veterinario.setEmail(req.getEmail());
        veterinario.setMatricula(req.getMatricula());
        veterinario.setEspecialidad(req.getEspecialidad());
        return veterinario;
    }

    /** Entidad de dominio -> DTO de salida. */
    public static VeterinarioResponse toResponse(Veterinario veterinario) {
        return new VeterinarioResponse(
                veterinario.getId(),
                veterinario.getNombre(),
                veterinario.getApellido(),
                veterinario.getTelefono(),
                veterinario.getEmail(),
                veterinario.getMatricula(),
                veterinario.getEspecialidad()
        );
    }
}
