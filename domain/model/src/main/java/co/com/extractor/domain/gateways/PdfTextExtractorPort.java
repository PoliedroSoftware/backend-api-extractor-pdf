package co.com.extractor.domain.gateways;

import java.io.InputStream;

public interface PdfTextExtractorPort {
    String extractText(InputStream fileStream, String originalFilename) throws Exception;
}
