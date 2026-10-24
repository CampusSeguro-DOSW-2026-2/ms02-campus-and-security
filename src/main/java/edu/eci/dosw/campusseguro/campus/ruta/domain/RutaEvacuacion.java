package edu.eci.dosw.campusseguro.campus.ruta.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

// Una ruta por espacio de origen (unique en espacio_origen_id). Espacio y zona se guardan solo por id.
@Entity
@Table(name = "ruta_evacuacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_ruta_espacio_origen", columnNames = "espacio_origen_id"))
public class RutaEvacuacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "espacio_origen_id", nullable = false)
    private UUID espacioOrigenId;

    @Column(name = "zona_segura_destino_id", nullable = false)
    private UUID zonaSeguraDestinoId;

    @Column(name = "ruta_alternativa_id")
    private UUID rutaAlternativaId;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRuta estado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RutaEvacuacion() {
        // requerido por JPA
    }

    public RutaEvacuacion(UUID espacioOrigenId, UUID zonaSeguraDestinoId, UUID rutaAlternativaId,
                          String descripcion, EstadoRuta estado) {
        this.espacioOrigenId = espacioOrigenId;
        this.zonaSeguraDestinoId = zonaSeguraDestinoId;
        this.rutaAlternativaId = rutaAlternativaId;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        this.createdAt = ahora;
        this.updatedAt = ahora;
    }

    @PreUpdate
    void alActualizar() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getEspacioOrigenId() {
        return espacioOrigenId;
    }

    public UUID getZonaSeguraDestinoId() {
        return zonaSeguraDestinoId;
    }

    public void setZonaSeguraDestinoId(UUID zonaSeguraDestinoId) {
        this.zonaSeguraDestinoId = zonaSeguraDestinoId;
    }

    public UUID getRutaAlternativaId() {
        return rutaAlternativaId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoRuta getEstado() {
        return estado;
    }

    public void setEstado(EstadoRuta estado) {
        this.estado = estado;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
