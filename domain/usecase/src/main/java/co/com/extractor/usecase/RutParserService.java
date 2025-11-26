package co.com.extractor.usecase;

import co.com.extractor.domain.model.RutResponse;
import java.io.InputStream;

public class RutParserService {
    public RutResponse parse(InputStream is, String filename) {
        // Minimal implementation: devolver un objeto vacío (la lógica real vive en el dominio si aplica)
        return new RutResponse();
    }
}
