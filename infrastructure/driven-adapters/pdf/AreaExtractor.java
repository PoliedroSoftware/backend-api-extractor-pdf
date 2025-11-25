package co.com.extractor.infrastructure.drivenadapters.pdf;

import co.com.extractor.model.gateways.AreaExtractorPort;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

public class AreaExtractor implements AreaExtractorPort {
    @Override
    public Map<String, String> extractAreas(InputStream fileStream, String originalFilename) {
        return Collections.emptyMap();
    }
}
