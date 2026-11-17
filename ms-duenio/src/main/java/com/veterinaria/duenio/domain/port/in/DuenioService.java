package com.veterinaria.duenio.domain.port.in;

import com.veterinaria.duenio.domain.model.Duenio;

import java.util.List;

/** Puerto de entrada: el contrato de casos de uso de Duenio (qué, no cómo). */
public interface DuenioService {
    List<Duenio> listarTodos();
    Duenio buscarPorId(Long id);
    Duenio buscarPorCedula(String cedula);
    Duenio buscarPorEmail(String email);
    List<Duenio> buscarPorNombreYApellido(String nombre, String apellido);
    Duenio registrar(Duenio duenio);
    Duenio actualizar(Long id, Duenio duenio);
    void eliminarPorId(Long id);
}
