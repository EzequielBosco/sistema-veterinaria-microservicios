package com.veterinaria.duenio.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad raiz del bounded context Duenio.
 * Los campos de la antigua clase abstracta Persona (nombre, apellido, telefono, email)
 * quedan aplanados aca: los microservicios no comparten clases de dominio.
 * La lista de mascotas ya no vive aca: ms-mascota guarda el duenioId.
 */
@Entity
@Table(name = "duenios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Duenio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_duenio")
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Column(length = 150, unique = true)
    private String email;

    @Column(nullable = false, unique = true, length = 30)
    private String cedula;
}
