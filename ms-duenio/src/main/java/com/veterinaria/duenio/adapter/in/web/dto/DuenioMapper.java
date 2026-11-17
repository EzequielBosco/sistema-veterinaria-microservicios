package com.veterinaria.duenio.adapter.in.web.dto;

import com.veterinaria.duenio.domain.model.Duenio;

/**
 * Traductor entre DTOs (mundo HTTP) y la entidad de dominio (mundo interno).
 * Mapeo manual a proposito. Vive en el adaptador de entrada: el dominio no conoce estos DTOs.
 */
public final class DuenioMapper {

    private DuenioMapper() {
        // clase de utilidades: no se instancia
    }

    /** DTO de entrada -> entidad de dominio (sin id: lo asigna la BD). */
    public static Duenio toEntity(DuenioRequest req) {
        Duenio duenio = new Duenio();
        duenio.setNombre(req.getNombre());
        duenio.setApellido(req.getApellido());
        duenio.setCedula(req.getCedula());
        duenio.setTelefono(req.getTelefono());
        duenio.setEmail(req.getEmail());
        return duenio;
    }

    /** Entidad de dominio -> DTO de salida. */
    public static DuenioResponse toResponse(Duenio duenio) {
        return new DuenioResponse(
                duenio.getId(),
                duenio.getNombre(),
                duenio.getApellido(),
                duenio.getCedula(),
                duenio.getTelefono(),
                duenio.getEmail()
        );
    }
}
