package com.veterinaria.mascota.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Entidad de dominio Mascota.
 * OJO: duenioId es un Long, NO un @ManyToOne hacia Duenio.
 * En microservicios cada servicio tiene su propia BD: no hay foreign key cruzada.
 * La lista de turnos tampoco vive aca: ms-turno guarda el mascotaId.
 */
@Entity
@Table(name = "mascotas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mascota")
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String especie;

    @Column(length = 100)
    private String raza;

    @Column(length = 50)
    private String color;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('MACHO','HEMBRA')")
    private SexoMascota sexo;

    @Column(name = "id_duenio", nullable = false)
    private Long duenioId;   // referencia por ID al ms-duenio (sin @ManyToOne)
}
