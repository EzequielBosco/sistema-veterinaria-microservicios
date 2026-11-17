package com.veterinaria.mascota.domain.port.out;

import com.veterinaria.mascota.domain.model.Mascota;

import java.util.List;

/** Puerto de salida: lo que el dominio necesita de la persistencia (sin saber de JPA). */
public interface MascotaRepositoryPort {
    List<Mascota> buscarTodos();
    Mascota buscarPorId(Long id);
    List<Mascota> buscarPorDuenioId(Long duenioId);
    long contarPorDuenioId(Long duenioId);
    Mascota guardar(Mascota mascota);
    void eliminarPorId(Long id);
}
