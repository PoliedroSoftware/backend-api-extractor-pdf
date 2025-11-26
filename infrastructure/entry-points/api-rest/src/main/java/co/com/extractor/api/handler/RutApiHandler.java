package co.com.extractor.api.handler;

import co.com.extractor.usecase.ParseRutUseCase;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/v1/rut")
public class RutApiHandler {

    private final ParseRutUseCase parseRutUseCase;
    private final java.util.function.Function<co.com.extractor.domain.model.RutResponse, co.com.extractor.api.dto.RutResponse> domainToApiMapper;

    @Autowired
    public RutApiHandler(ParseRutUseCase parseRutUseCase,
                         java.util.function.Function<co.com.extractor.domain.model.RutResponse, co.com.extractor.api.dto.RutResponse> domainToApiMapper) {
        this.parseRutUseCase = parseRutUseCase;
        this.domainToApiMapper = domainToApiMapper;
    }

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<co.com.extractor.api.dto.RutResponse> parse(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo PDF requerido");
        if (!isPdf(file)) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Solo PDF");

        try (InputStream is = file.getInputStream()) {
            co.com.extractor.domain.model.RutResponse domainResp = parseRutUseCase.execute(is, file.getOriginalFilename());
            if (domainResp == null || (domainResp.getNit() == null && (domainResp.getFullName() == null || domainResp.getFullName().getDisplay() == null))) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No se pudo extraer NIT/DV desde el PDF proporcionado.");
            }
            co.com.extractor.api.dto.RutResponse api = domainToApiMapper.apply(domainResp);
            return ResponseEntity.ok(api);
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error procesando el PDF", e);
        }
    }

    private boolean isPdf(MultipartFile file) {
        try {
            String ct = file.getContentType();
            if (ct != null && ct.toLowerCase().contains("pdf")) return true;
            try (InputStream in = file.getInputStream()) {
                byte[] header = new byte[4];
                int read = in.read(header);
                if (read == 4) {
                    String s = new String(header, java.nio.charset.StandardCharsets.US_ASCII);
                    return s.startsWith("%PDF");
                }
            }
        } catch (Exception e) {}
        return false;
    }
}
