package edu.eci.dosw.campusseguro.campus.ruta.service;

import edu.eci.dosw.campusseguro.campus.ruta.event.SalidaUbicacionCambiadaEvent;
import edu.eci.dosw.campusseguro.campus.ruta.event.ZonaSeguraUbicacionCambiadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Genera las rutas cuando cambia una salida o una zona. Corre después del commit (la salida o zona ya existe
// en la base) y en otro hilo. Un fallo aquí nunca afecta al administrador: queda en el log y se puede repetir
// con POST /api/v1/evacuation-routes/exterior/generate.
@Component
@ConditionalOnProperty(prefix = "campusseguro.rutas.auto", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RutasAutoListener {

    private static final Logger log = LoggerFactory.getLogger(RutasAutoListener.class);

    private final GeneradorRutasExterioresService generador;

    public RutasAutoListener(GeneradorRutasExterioresService generador) {
        this.generador = generador;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alCambiarSalida(SalidaUbicacionCambiadaEvent evento) {
        try {
            generador.generarParaSalida(evento.salidaId());
        } catch (RuntimeException e) {
            log.error("No se pudieron generar las rutas de la salida {}", evento.salidaId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alCambiarZona(ZonaSeguraUbicacionCambiadaEvent evento) {
        try {
            generador.generarParaZona(evento.zonaId());
        } catch (RuntimeException e) {
            log.error("No se pudieron generar las rutas de la zona segura {}", evento.zonaId(), e);
        }
    }
}
