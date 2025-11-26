package co.com.extractor.service;

import co.com.extractor.api.dto.RutResponse;
import co.com.extractor.extractor.PdfTextExtractor;
import co.com.extractor.extractor.AreaExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class RutParserServiceTest {

    @Test
    public void parse_samplePdf_shouldExtractKeyFields() throws Exception {
        byte[] pdfBytes = Files.readAllBytes(Path.of("RUT_EDUAR_SANCHEZ.pdf"));
        MockMultipartFile file = new MockMultipartFile("file", "RUT_EDUAR_SANCHEZ.pdf", "application/pdf", pdfBytes);

        PdfTextExtractor extractor = new PdfTextExtractor();
        AreaExtractor areaExtractor = new AreaExtractor();
        RutParserService service = new RutParserService(extractor, areaExtractor);

        RutResponse resp = service.parse(file);
        assertNotNull(resp, "Response should not be null");
        assertNotNull(resp.getNit(), "NIT should be extracted");
        assertTrue(resp.getNit().length() >= 9, "NIT length should be >=9");
        assertNotNull(resp.getFullName(), "FullName should be present");
        assertNotNull(resp.getFullName().getDisplay(), "Display name should be present");
        // postal code must be last 4 digits; assert it's 4-digit string when present
        if (resp.getPostalCode() != null) {
            assertEquals(4, resp.getPostalCode().length(), "Postal code should be 4 digits");
        }
    }
}

