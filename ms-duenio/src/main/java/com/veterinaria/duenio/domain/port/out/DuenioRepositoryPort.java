package com.veterinaria.duenio.domain.port.out;

import com.veterinaria.duenio.domain.model.Duenio;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: lo que el dominio necesita de la persistencia (sin saber de JPA). */
public interface DuenioRepositoryPort {
    List<Duenio> buscarTodos();
    Duenio buscarPorId(Long id);
    Optional<Duenio> buscarPorCedula(String cedula);
    Optional<Duenio> buscarPorEmail(String email);
    List<Duenio> buscarPorNombreYApellido(String nombre, String apellido);
    boolean existePorCedula(String cedula);
    boolean existePorEmail(String email);
    Duenio guardar(Duenio duenio);
    void eliminarPorId(Long id);
}
