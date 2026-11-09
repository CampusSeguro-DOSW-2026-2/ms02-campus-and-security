package edu.eci.dosw.campusseguro.campus.controller;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Imagen;
import edu.eci.dosw.campusseguro.campus.entity.TipoElementoImagen;
import edu.eci.dosw.campusseguro.campus.service.ImagenService;
import edu.eci.dosw.campusseguro.campus.security.Permiso;
import edu.eci.dosw.campusseguro.campus.security.Permisos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(
    name = "Imágenes",
    description = "Almacenamiento y consulta de fotografías de elementos del campus."
)
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImagenController {

    private final ImagenService imagenService;

    @Operation(summary = "Registrar imagen de un elemento del campus")
    @Permiso(Permisos.BUILDING_WRITE)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Imagen> registrar(
            @RequestParam TipoElementoImagen tipoElemento,
            @RequestParam UUID elementoId,
            @RequestParam("archivo") MultipartFile archivo) throws java.io.IOException {

        Imagen imagen = imagenService.registrar(
                tipoElemento,
                elementoId,
                archivo.getOriginalFilename(),
                archivo.getBytes()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(imagen);
    }

    @Operation(summary = "Consultar archivo de imagen")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> obtener(@PathVariable UUID id) {
        Imagen imagen = imagenService.obtener(id);
        byte[] contenido = imagenService.obtenerContenido(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getTipoContenido()))
                .contentLength(contenido.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + imagen.getId() + "\""
                )
                .body(contenido);
    }

    @Operation(summary = "Eliminar imagen")
    @Permiso(Permisos.BUILDING_WRITE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        imagenService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
