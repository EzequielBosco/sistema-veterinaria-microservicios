package com.veterinaria.veterinario.adapter.out.persistence;

import com.veterinaria.veterinario.domain.model.Veterinario;
import com.veterinaria.veterinario.domain.port.out.VeterinarioRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Adaptador de salida: implementa el puerto de persistencia con Spring Data JPA. */
@Repository
public interface VeterinarioRepository
        extends JpaRepository<Veterinario, Long>, VeterinarioRepositoryPort {

    boolean existsByMatricula(String matricula);

    boolean existsByEmail(String email);

    List<Veterinario> findByEspecialidadIgnoreCaseOrderByApellidoAscNombreAsc(String especialidad);

    @Override
    default List<Veterinario> buscarTodos() {
        return findAll();
    }

    @Override
    default Veterinario buscarPorId(Long id) {
        return findById(id).orElse(null);
    }

    @Override
    default List<Veterinario> buscarPorEspecialidad(String especialidad) {
        return findByEspecialidadIgnoreCaseOrderByApellidoAscNombreAsc(especialidad);
    }

    @Override
    default boolean existePorMatricula(String matricula) {
        return existsByMatricula(matricula);
    }

    @Override
    default boolean existePorEmail(String email) {
        return existsByEmail(email);
    }

    @Override
    default Veterinario guardar(Veterinario veterinario) {
        return save(veterinario);
    }

    @Override
    default void eliminarPorId(Long id) {
        deleteById(id);
    }
}
