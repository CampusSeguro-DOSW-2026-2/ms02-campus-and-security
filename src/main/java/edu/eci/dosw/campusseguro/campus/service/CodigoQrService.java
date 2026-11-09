package edu.eci.dosw.campusseguro.campus.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import edu.eci.dosw.campusseguro.campus.exception.EspacioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.SalidaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaSeguraNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.SalidaRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CodigoQrService {

    private static final int TAMANO = 512;

    public enum TipoQr {
        ESPACIO("spaces"),
        SALIDA("exits"),
        ZONA_SEGURA("safe-zones");

        private final String ruta;

        TipoQr(String ruta) {
            this.ruta = ruta;
        }
    }

    private final String baseUrl;
    private final EspacioRepository espacioRepository;
    private final SalidaRepository salidaRepository;
    private final ZonaSeguraRepository zonaSeguraRepository;

    public CodigoQrService(@Value("${campusseguro.qr.base-url}") String baseUrl,
                           EspacioRepository espacioRepository,
                           SalidaRepository salidaRepository,
                           ZonaSeguraRepository zonaSeguraRepository) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.espacioRepository = espacioRepository;
        this.salidaRepository = salidaRepository;
        this.zonaSeguraRepository = zonaSeguraRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generar(TipoQr tipo, UUID id) {
        verificarQueExiste(tipo, id);
        log.info("Código QR generado para {} {}", tipo, id);
        return png(enlace(tipo, id));
    }

    public String enlace(TipoQr tipo, UUID id) {
        return baseUrl + "/api/v1/public/" + tipo.ruta + "?id=" + id;
    }

    private void verificarQueExiste(TipoQr tipo, UUID id) {
        switch (tipo) {
            case ESPACIO -> {
                if (!espacioRepository.existsById(id)) {
                    throw new EspacioNoEncontradoException(id);
                }
            }
            case SALIDA -> {
                if (!salidaRepository.existsById(id)) {
                    throw new SalidaNoEncontradaException(id);
                }
            }
            case ZONA_SEGURA -> {
                if (!zonaSeguraRepository.existsById(id)) {
                    throw new ZonaSeguraNoEncontradaException(id);
                }
            }
        }
    }

    private static byte[] png(String contenido) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, TAMANO, TAMANO,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 2));
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
            return salida.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("No se pudo generar el código QR", e);
        }
    }
}
