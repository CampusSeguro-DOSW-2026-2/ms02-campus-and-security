package edu.eci.dosw.campusseguro.campus.controller;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.security.Permiso;
import edu.eci.dosw.campusseguro.campus.security.Permisos;
import edu.eci.dosw.campusseguro.campus.service.CodigoQrService;
import edu.eci.dosw.campusseguro.campus.service.CodigoQrService.TipoQr;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Códigos QR", description = "ADR-003. QR generados a partir del UUID del elemento. Solo contienen el enlace a la pantalla pública del frontend.")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CodigoQrController {

    private final CodigoQrService codigoQrService;

    @Operation(summary = "QR de un espacio", description = "Solo ADMIN. Imagen PNG con el enlace a la pantalla pública #/qr/espacios/{id}.")
    @Permiso(Permisos.BUILDING_WRITE)
    @GetMapping("/spaces/{id}/qr")
    public ResponseEntity<byte[]> espacio(@PathVariable UUID id) {
        return imagen(TipoQr.ESPACIO, id);
    }

    @Operation(summary = "QR de una salida", description = "Solo ADMIN. Imagen PNG con el enlace a la pantalla pública #/qr/salidas/{id}.")
    @Permiso(Permisos.BUILDING_WRITE)
    @GetMapping("/exits/{id}/qr")
    public ResponseEntity<byte[]> salida(@PathVariable UUID id) {
        return imagen(TipoQr.SALIDA, id);
    }

    @Operation(summary = "QR de una zona segura", description = "Solo ADMIN. Imagen PNG con el enlace a la pantalla pública #/qr/zonas/{id}.")
    @Permiso(Permisos.SAFE_ZONE_WRITE)
    @GetMapping("/safe-zones/{id}/qr")
    public ResponseEntity<byte[]> zonaSegura(@PathVariable UUID id) {
        return imagen(TipoQr.ZONA_SEGURA, id);
    }

    private ResponseEntity<byte[]> imagen(TipoQr tipo, UUID id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qr-" + id + ".png\"")
                .body(codigoQrService.generar(tipo, id));
    }
}
