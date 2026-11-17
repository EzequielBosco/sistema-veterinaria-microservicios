package com.veterinaria.duenio.adapter.out.persistence;

import com.veterinaria.duenio.domain.model.Duenio;
import com.veterinaria.duenio.domain.port.out.DuenioRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida: implementa el puerto de persistencia con Spring Data JPA.
 * Los finders los genera Spring Data a partir del nombre del método.
 */
@Repository
public interface DuenioRepository extends JpaRepository<Duenio, Long>, DuenioRepositoryPort {

    Optional<Duenio> findByCedula(String cedula);

    Optional<Duenio> findByEmail(String email);

    List<Duenio> findByNombreAndApellido(String nombre, String apellido);

    boolean existsByCedula(String cedula);

    boolean existsByEmail(String email);

    @Override
    default List<Duenio> buscarTodos() {
        return findAll();
    }

    @Override
    default Duenio buscarPorId(Long id) {
        return findById(id).orElse(null);
    }

    @Override
    default Optional<Duenio> buscarPorCedula(String cedula) {
        return findByCedula(cedula);
    }

    @Override
    default Optional<Duenio> buscarPorEmail(String email) {
        return findByEmail(email);
    }

    @Override
    default List<Duenio> buscarPorNombreYApellido(String nombre, String apellido) {
        return findByNombreAndApellido(nombre, apellido);
    }

    @Override
    default boolean existePorCedula(String cedula) {
        return existsByCedula(cedula);
    }

    @Override
    default boolean existePorEmail(String email) {
        return existsByEmail(email);
    }

    @Override
    default Duenio guardar(Duenio duenio) {
        return save(duenio);
    }

    @Override
    default void eliminarPorId(Long id) {
        deleteById(id);
    }
}
