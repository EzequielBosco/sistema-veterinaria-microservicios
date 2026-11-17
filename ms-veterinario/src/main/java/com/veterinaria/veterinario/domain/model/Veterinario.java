package com.veterinaria.veterinario.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad de dominio Veterinario.
 * Los campos de la antigua clase abstracta Persona (nombre, apellido, telefono, email)
 * quedan aplanados aca: los microservicios no comparten clases de dominio.
 * Las participaciones en turnos viven en ms-turno, que guarda el veterinarioId.
 */
@Entity
@Table(name = "veterinarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Veterinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_veterinario")
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Column(length = 150, unique = true)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String matricula;

    @Column(length = 100)
    private String especialidad;
}
