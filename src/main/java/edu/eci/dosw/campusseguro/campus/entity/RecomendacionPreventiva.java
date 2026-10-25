package edu.eci.dosw.campusseguro.campus.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recomendacion_preventiva")
@Getter
@Setter
@NoArgsConstructor
public class RecomendacionPreventiva {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AmbitoRecomendacion ambito;

    @Column(name = "edificio_id")
    private UUID edificioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_espacio", length = 20)
    private TipoEspacio tipoEspacio;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recomendacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        createdAt = ahora;
        updatedAt = ahora;
    }

    @PreUpdate
    void alActualizar() {
        updatedAt = LocalDateTime.now();
    }
}
