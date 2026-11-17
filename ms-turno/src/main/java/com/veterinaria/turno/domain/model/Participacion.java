package com.veterinaria.turno.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad intermedia del muchos a muchos Turno <-> Veterinario, con el rol del veterinario.
 * Vive dentro de ms-turno porque su ciclo de vida depende del Turno.
 * veterinarioId es un Long (referencia por ID a ms-veterinario), NO un @ManyToOne.
 */
@Entity
@Table(name = "participaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Participacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_turno", nullable = false)
    private Turno turno;

    @Column(name = "id_veterinario", nullable = false)
    private Long veterinarioId;   // referencia por ID al ms-veterinario (sin @ManyToOne)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('PRINCIPAL','ASISTENTE','CONSULTOR')")
    private RolVeterinario rol;
}
