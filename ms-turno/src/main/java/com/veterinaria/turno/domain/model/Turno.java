package com.veterinaria.turno.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad de dominio Turno (raiz del agregado Turno + Participacion).
 * mascotaId es un Long (referencia por ID a ms-mascota), NO un @ManyToOne:
 * cada MS tiene su propia BD.
 * Se usa @Getter/@Setter en lugar de @Data: la relacion bidireccional con Participacion
 * haria que equals/hashCode/toString se llamen en bucle.
 */
@Entity
@Table(name = "turnos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_turno")
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(name = "duracion_minutos", nullable = false, columnDefinition = "INT DEFAULT 30")
    private Integer duracionMinutos = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('PENDIENTE','CONFIRMADO','CANCELADO','AUSENTE','ATENDIDO') DEFAULT 'PENDIENTE'")
    private EstadoTurno estado = EstadoTurno.PENDIENTE;

    @Column(name = "id_mascota", nullable = false)
    private Long mascotaId;   // referencia por ID al ms-mascota (sin @ManyToOne)

    @OneToMany(mappedBy = "turno", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Participacion> participaciones = new HashSet<>();

    /** Asigna un veterinario al turno con el rol indicado, manteniendo la relacion bidireccional. */
    public void agregarParticipacion(Long veterinarioId, RolVeterinario rol) {
        Participacion participacion = new Participacion();
        participacion.setTurno(this);
        participacion.setVeterinarioId(veterinarioId);
        participacion.setRol(rol);
        participaciones.add(participacion);
    }
}
