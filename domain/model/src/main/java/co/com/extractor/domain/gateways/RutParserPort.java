package co.com.extractor.domain.gateways;

import co.com.extractor.domain.model.RutResponse;
import java.io.InputStream;

public interface RutParserPort {
    RutResponse parse(InputStream fileStream, String originalFilename) throws Exception;
}

