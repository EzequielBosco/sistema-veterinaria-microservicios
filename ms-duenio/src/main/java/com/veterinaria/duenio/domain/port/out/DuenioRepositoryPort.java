package com.veterinaria.duenio.domain.port.out;

import com.veterinaria.duenio.domain.model.Duenio;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: lo que el dominio necesita de la persistencia.
 * Las firmas coinciden con las de Spring Data para que el adaptador JPA
 * las satisfaga sin codigo extra, pero esta interface no depende de Spring.
 */
public interface DuenioRepositoryPort {

    List<Duenio> findAll();

    Optional<Duenio> findById(Long id);

    Optional<Duenio> findByCedula(String cedula);

    Optional<Duenio> findByEmail(String email);

    List<Duenio> findByNombreAndApellido(String nombre, String apellido);

    boolean existsByCedula(String cedula);

    boolean existsByEmail(String email);

    Duenio save(Duenio duenio);

    void deleteById(Long id);
}
