package com.veterinaria.veterinario.domain.port.out;

import com.veterinaria.veterinario.domain.model.Veterinario;

import java.util.List;

/** Puerto de salida: contrato de persistencia de Veterinario. */
public interface VeterinarioRepositoryPort {
    List<Veterinario> buscarTodos();
    Veterinario buscarPorId(Long id);
    List<Veterinario> buscarPorEspecialidad(String especialidad);
    boolean existePorMatricula(String matricula);
    boolean existePorEmail(String email);
    Veterinario guardar(Veterinario veterinario);
    void eliminarPorId(Long id);
}
