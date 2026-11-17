package com.veterinaria.duenio.adapter.in.web.dto;

import com.veterinaria.duenio.domain.model.Duenio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Traduce entre los DTOs web y el modelo de dominio.
 * Vive en el adaptador de entrada para que el controller no tenga que conocer domain.model.
 */
@Mapper(componentModel = "spring")
public interface DuenioMapper {

    @Mapping(target = "id", ignore = true)
    Duenio toDomain(DuenioRequest duenioRequest);

    DuenioResponse toResponse(Duenio duenio);

    List<DuenioResponse> toResponseList(List<Duenio> duenios);
}
