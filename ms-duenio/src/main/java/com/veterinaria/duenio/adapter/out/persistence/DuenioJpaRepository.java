package com.veterinaria.duenio.adapter.out.persistence;

import com.veterinaria.duenio.domain.model.Duenio;
import com.veterinaria.duenio.domain.port.out.DuenioRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de salida: implementa el puerto DuenioRepositoryPort con Spring Data JPA.
 * Los metodos CRUD los hereda de JpaRepository y los finders los deriva Spring Data
 * a partir del nombre del metodo declarado en el puerto.
 */
@Repository
public interface DuenioJpaRepository extends JpaRepository<Duenio, Long>, DuenioRepositoryPort {
}
