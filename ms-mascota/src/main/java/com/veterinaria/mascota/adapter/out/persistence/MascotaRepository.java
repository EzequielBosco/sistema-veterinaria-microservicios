package com.veterinaria.mascota.adapter.out.persistence;

import com.veterinaria.mascota.domain.model.Mascota;
import com.veterinaria.mascota.domain.port.out.MascotaRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Adaptador de salida: implementa el puerto de persistencia con Spring Data JPA.
 * Los finders por duenioId los genera Spring Data a partir del nombre del método.
 */
@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Long>, MascotaRepositoryPort {

    List<Mascota> findByDuenioId(Long duenioId);

    long countByDuenioId(Long duenioId);

    @Override
    default List<Mascota> buscarTodos() {
        return findAll();
    }

    @Override
    default Mascota buscarPorId(Long id) {
        return findById(id).orElse(null);
    }

    @Override
    default List<Mascota> buscarPorDuenioId(Long duenioId) {
        return findByDuenioId(duenioId);
    }

    @Override
    default long contarPorDuenioId(Long duenioId) {
        return countByDuenioId(duenioId);
    }

    @Override
    default Mascota guardar(Mascota mascota) {
        return save(mascota);
    }

    @Override
    default void eliminarPorId(Long id) {
        deleteById(id);
    }
}
