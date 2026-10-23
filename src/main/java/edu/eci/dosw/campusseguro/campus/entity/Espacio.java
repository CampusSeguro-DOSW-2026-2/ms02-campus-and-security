package edu.eci.dosw.campusseguro.campus.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "espacio",
        uniqueConstraints = @UniqueConstraint(name = "uk_espacio_edificio_codigo", columnNames = {"edificio_id", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Espacio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "edificio_id", nullable = false)
    private UUID edificioId;

    @Column(name = "piso_id", nullable = false)
    private UUID pisoId;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEspacio tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;
}
