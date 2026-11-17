package com.veterinaria.turno.adapter.out.persistence;

import com.veterinaria.turno.domain.model.Turno;
import com.veterinaria.turno.domain.port.out.TurnoRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Adaptador de salida: implementa el puerto de persistencia con Spring Data JPA.
 * Los finders navegan participaciones.veterinarioId, que es una columna local de ms-turno.
 */
@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long>, TurnoRepositoryPort {

    List<Turno> findDistinctByParticipacionesVeterinarioIdAndFechaOrderByHoraAsc(Long veterinarioId, LocalDate fecha);

    List<Turno> findDistinctByParticipacionesVeterinarioIdOrderByFechaAscHoraAsc(Long veterinarioId);

    @Override
    default List<Turno> buscarTodos() {
        return findAll();
    }

    @Override
    default Turno buscarPorId(Long id) {
        return findById(id).orElse(null);
    }

    @Override
    default List<Turno> buscarPorVeterinarioYFecha(Long veterinarioId, LocalDate fecha) {
        return findDistinctByParticipacionesVeterinarioIdAndFechaOrderByHoraAsc(veterinarioId, fecha);
    }

    @Override
    default List<Turno> buscarPorVeterinarioId(Long veterinarioId) {
        return findDistinctByParticipacionesVeterinarioIdOrderByFechaAscHoraAsc(veterinarioId);
    }

    @Override
    default Turno guardar(Turno turno) {
        return save(turno);
    }

    @Override
    default void eliminarPorId(Long id) {
        deleteById(id);
    }
}
