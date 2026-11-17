package com.veterinaria.duenio.domain.port.in;

import com.veterinaria.duenio.domain.model.Duenio;

import java.util.List;

/**
 * Puerto de entrada: define QUE se puede hacer con los duenios, no COMO.
 * Lo consumen los adaptadores de entrada (controller) y lo implementa la capa application.
 */
public interface DuenioService {

    List<Duenio> findAll();

    Duenio findById(Long id);

    Duenio findByCedula(String cedula);

    Duenio findByEmail(String email);

    List<Duenio> search(String nombre, String apellido);

    Duenio save(Duenio duenio);

    Duenio update(Long id, Duenio duenio);

    void deleteById(Long id);
}
