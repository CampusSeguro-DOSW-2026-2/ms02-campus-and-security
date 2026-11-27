package edu.eci.dosw.campusseguro.campus.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "zona_segura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZonaSegura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String ubicacion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Integer capacidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;
    
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    // Coordenadas opcionales: sin ellas la zona no puede ser destino de una ruta exterior.
    @Column(precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(name = "zona_abierta_id")
    private UUID zonaAbiertaId;

    // No hay entidad PuntoEncuentro: es una zona segura con esta marca (ADR-003).
    @Column(name = "es_punto_encuentro", nullable = false)
    private boolean puntoEncuentro = true;
}