package com.veterinaria.veterinario.domain.port.in;

import com.veterinaria.veterinario.domain.model.Veterinario;

import java.util.List;

/** Puerto de entrada: casos de uso de Veterinario. */
public interface VeterinarioService {
    List<Veterinario> listarTodos();
    Veterinario buscarPorId(Long id);
    Veterinario registrar(Veterinario veterinario);
    Veterinario actualizar(Long id, Veterinario veterinario);
    void eliminarPorId(Long id);
}
