package co.com.extractor.api.handler;

import co.com.extractor.usecase.ParseInvoiceUseCase;
import co.com.extractor.api.dto.InvoiceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Controlador REST para manejar solicitudes de análisis de facturas.
 * <p>
 * Expone endpoints a través de la API para recibir archivos PDF de facturas,
 * procesarlos utilizando la lógica de negocio y devolver los datos extraídos
 * en formato JSON estructurado.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/invoice")
public class InvoiceApiHandler {

    private final ParseInvoiceUseCase parseInvoiceUseCase;
    private final java.util.function.Function<co.com.extractor.domain.model.InvoiceResponse, co.com.extractor.api.dto.InvoiceResponse> domainToApiMapper;

    /**
     * Construye un nuevo InvoiceApiHandler.
     *
     * @param parseInvoiceUseCase el caso de uso para analizar facturas
     * @param domainToApiMapper   función para mapear la respuesta del dominio al
     *                            DTO de la API
     */
    @Autowired
    public InvoiceApiHandler(ParseInvoiceUseCase parseInvoiceUseCase,
            java.util.function.Function<co.com.extractor.domain.model.InvoiceResponse, co.com.extractor.api.dto.InvoiceResponse> domainToApiMapper) {
        this.parseInvoiceUseCase = parseInvoiceUseCase;
        this.domainToApiMapper = domainToApiMapper;
    }

    /**
     * Endpoint para analizar un archivo de factura PDF subido.
     *
     * @param file el archivo PDF de la factura subido como multipart/form-data
     * @return una {@link ResponseEntity} que contiene los datos extraídos de la
     *         factura
     * @throws ResponseStatusException si el archivo falta, no es un PDF, o si
     *                                 ocurre un error en el procesamiento
     */
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InvoiceResponse> parse(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo PDF requerido");
        }
        if (!isPdf(file)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Solo se admiten archivos PDF");
        }

        try (InputStream is = file.getInputStream()) {
            co.com.extractor.domain.model.InvoiceResponse domainResp = parseInvoiceUseCase.execute(is,
                    file.getOriginalFilename());
            InvoiceResponse api = domainToApiMapper.apply(domainResp);
            return ResponseEntity.ok(api);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error procesando la factura", e);
        }
    }

    /**
     * Valida si el archivo proporcionado es un PDF comprobando su tipo de contenido
     * y su firma mágica.
     *
     * @param file el archivo a validar
     * @return true si el archivo es un PDF, false en caso contrario
     */
    private boolean isPdf(MultipartFile file) {
        try {
            String ct = file.getContentType();
            if (ct != null && ct.toLowerCase().contains("pdf")) {
                return true;
            }
            try (InputStream in = file.getInputStream()) {
                byte[] header = new byte[4];
                int read = in.read(header);
                if (read == 4) {
                    String s = new String(header, StandardCharsets.US_ASCII);
                    return s.startsWith("%PDF");
                }
            }
        } catch (Exception ignored) {
            // Ignorar errores de lectura, asumir que no es PDF
        }
        return false;
    }
}
