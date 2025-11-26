package co.com.extractor.appservice.adapter;

import co.com.extractor.domain.gateways.AreaExtractorPort;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

@Component
@ConditionalOnMissingBean(AreaExtractorPort.class)
public class DefaultAreaExtractor implements AreaExtractorPort {
    @Override
    public Map<String, String> extractAreas(InputStream fileStream, String originalFilename) throws Exception {
        return Collections.emptyMap();
    }
}

