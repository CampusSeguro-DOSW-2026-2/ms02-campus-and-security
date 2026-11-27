package edu.eci.dosw.campusseguro.campus.entity;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "escalera")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Escalera {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "edificio_id", nullable = false)
    private UUID edificioId;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private DisponibilidadEscalera disponibilidad = DisponibilidadEscalera.DISPONIBLE;

    @Column(length = 255)
    private String motivo;

    // 1 = escalera principal del edificio; las de numero mayor se ofrecen como alternas.
    @Column(nullable = false)
    private int prioridad = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "escalera_piso",
            joinColumns = @JoinColumn(name = "escalera_id"),
            inverseJoinColumns = @JoinColumn(name = "piso_id")
    )
    private Set<Piso> pisos = new LinkedHashSet<>();
}
