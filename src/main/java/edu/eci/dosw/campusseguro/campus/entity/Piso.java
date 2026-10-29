package edu.eci.dosw.campusseguro.campus.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "piso",
        uniqueConstraints = @UniqueConstraint(name = "uk_piso_edificio_numero", columnNames = {"edificio_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Piso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "edificio_id", nullable = false)
    private UUID edificioId;

    // Puede ser negativo para sotanos
    @Column(nullable = false)
    private Integer numero;

    @Column(length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;
}
