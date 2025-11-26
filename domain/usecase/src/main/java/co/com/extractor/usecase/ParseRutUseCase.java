package co.com.extractor.usecase;

import java.io.InputStream;

/** UseCase puro del módulo 'usecase' (sin dependencias de Spring ni web) */
public class ParseRutUseCase {

    private final co.com.extractor.domain.gateways.RutParserPort parserPort;

    public ParseRutUseCase(co.com.extractor.domain.gateways.RutParserPort parserPort) {
        this.parserPort = parserPort;
    }

    public co.com.extractor.domain.model.RutResponse execute(InputStream fileStream, String filename) throws Exception {
        return parserPort.parse(fileStream, filename);
    }
}
