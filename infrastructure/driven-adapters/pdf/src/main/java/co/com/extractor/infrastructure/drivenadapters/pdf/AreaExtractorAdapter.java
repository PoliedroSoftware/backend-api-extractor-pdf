package co.com.extractor.infrastructure.drivenadapters.pdf;

import co.com.extractor.domain.gateways.AreaExtractorPort;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

@Component
public class AreaExtractorAdapter implements AreaExtractorPort {
    @Override
    public Map<String, String> extractAreas(InputStream fileStream, String originalFilename) throws Exception {
        return Collections.emptyMap();
    }
}
