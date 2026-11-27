package edu.eci.dosw.campusseguro.campus.ruta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

// Dos tipos de ruta (ADR-003): de legado (espacio -> zona, descrita en texto, una por espacio) y
// exterior (salida -> zona, con el último recorrido calculado). Origen y destino se guardan solo por id.
@Entity
@Table(name = "ruta_evacuacion",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ruta_espacio_origen", columnNames = "espacio_origen_id"),
                @UniqueConstraint(name = "uk_ruta_salida_zona",
                        columnNames = {"salida_origen_id", "zona_segura_destino_id"})
        })
public class RutaEvacuacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "espacio_origen_id")
    private UUID espacioOrigenId;

    @Column(name = "salida_origen_id")
    private UUID salidaOrigenId;

    @Column(name = "zona_segura_destino_id", nullable = false)
    private UUID zonaSeguraDestinoId;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRuta estado;

    @Column(length = 255)
    private String motivo;

    @Column(name = "distancia_metros", precision = 10, scale = 2)
    private BigDecimal distanciaMetros;

    @Column(name = "duracion_segundos", precision = 10, scale = 2)
    private BigDecimal duracionSegundos;

    // Recorrido calculado, en texto plano (ver CodificadorRutaExterior). Así la consulta no depende de internet.
    @Column(columnDefinition = "TEXT")
    private String geometria;

    @Column(columnDefinition = "TEXT")
    private String pasos;

    @Column(name = "calculada_en")
    private Instant calculadaEn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RutaEvacuacion() {
        // requerido por JPA
    }

    public RutaEvacuacion(UUID espacioOrigenId, UUID zonaSeguraDestinoId,
                          String descripcion, EstadoRuta estado) {
        this.espacioOrigenId = espacioOrigenId;
        this.zonaSeguraDestinoId = zonaSeguraDestinoId;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    // Ruta exterior: sale de una salida del edificio y llega a una zona segura.
    public static RutaEvacuacion exterior(UUID salidaOrigenId, UUID zonaSeguraDestinoId, String descripcion) {
        RutaEvacuacion ruta = new RutaEvacuacion(null, zonaSeguraDestinoId, descripcion, EstadoRuta.DISPONIBLE);
        ruta.salidaOrigenId = salidaOrigenId;
        return ruta;
    }

    public boolean esExterior() {
        return salidaOrigenId != null;
    }

    // Guarda el último recorrido calculado (D2); se llama de nuevo cuando se recalcula.
    public void registrarCalculo(double distanciaMetros, double duracionSegundos,
                                 String geometria, String pasos, Instant calculadaEn) {
        this.distanciaMetros = BigDecimal.valueOf(distanciaMetros).setScale(2, RoundingMode.HALF_UP);
        this.duracionSegundos = BigDecimal.valueOf(duracionSegundos).setScale(2, RoundingMode.HALF_UP);
        this.geometria = geometria;
        this.pasos = pasos;
        this.calculadaEn = calculadaEn;
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

    public UUID getSalidaOrigenId() {
        return salidaOrigenId;
    }

    public BigDecimal getDistanciaMetros() {
        return distanciaMetros;
    }

    public BigDecimal getDuracionSegundos() {
        return duracionSegundos;
    }

    public String getGeometria() {
        return geometria;
    }

    public String getPasos() {
        return pasos;
    }

    public Instant getCalculadaEn() {
        return calculadaEn;
    }

    public UUID getZonaSeguraDestinoId() {
        return zonaSeguraDestinoId;
    }

    public void setZonaSeguraDestinoId(UUID zonaSeguraDestinoId) {
        this.zonaSeguraDestinoId = zonaSeguraDestinoId;
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

    public String getMotivo() {
        return motivo;
    }

    public void cambiarEstado(EstadoRuta estado, String motivo) {
        this.estado = estado;
        this.motivo = estado == EstadoRuta.DISPONIBLE ? null : motivo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
