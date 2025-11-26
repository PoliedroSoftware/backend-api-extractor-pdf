package co.com.extractor.service;

import co.com.extractor.api.dto.RutResponse;
import co.com.extractor.extractor.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class RutParserIntegrationTest {

    private InputStream openSamplePdf() throws Exception {
        // Try repo root
        Path p1 = Paths.get("RUT_EDUAR_SANCHEZ.pdf");
        if (Files.exists(p1)) return new FileInputStream(p1.toFile());
        // Try project root
        Path p2 = Paths.get(System.getProperty("user.dir"), "RUT_EDUAR_SANCHEZ.pdf");
        if (Files.exists(p2)) return new FileInputStream(p2.toFile());
        // Try classpath
        InputStream r = this.getClass().getClassLoader().getResourceAsStream("RUT_EDUAR_SANCHEZ.pdf");
        if (r != null) return r;
        throw new IllegalStateException("Sample PDF RUT_EDUAR_SANCHEZ.pdf not found in repo root, project root or classpath");
    }

    @Test
    public void parseSampleRutPdf_should_extract_expected_fields() throws Exception {
        try (InputStream fis = openSamplePdf()) {
            MockMultipartFile mm = new MockMultipartFile("file", "RUT_EDUAR_SANCHEZ.pdf", "application/pdf", fis);
            PdfTextExtractor extractor = new PdfTextExtractor();
            RutParserService service = new RutParserService(extractor);
            RutResponse resp = service.parse(mm);

            assertNotNull(resp);
            // Expected values observed in the PDF screenshots
            assertEquals("14824701795", resp.getFormNumber());
            assertEquals("10916585513", resp.getNit());
            assertEquals("3", resp.getDv());
            assertNotNull(resp.getFullName());
            assertTrue(resp.getFullName().getDisplay().toUpperCase().contains("SANCHEZ"));
            assertTrue(resp.getFullName().getDisplay().toUpperCase().contains("EDUAR"));

            // Contact / location
            assertEquals("COLOMBIA", resp.getCountry());
            assertEquals("Norte de Santander", resp.getDepartment());
            assertTrue(resp.getCity() != null && resp.getCity().toUpperCase().contains("OCA"));
            assertEquals("5498", resp.getPostalCode());

            // Email and address
            assertEquals("leosanchez_19@hotmail.com", resp.getEmail());
            assertTrue(resp.getAddress() != null && resp.getAddress().toUpperCase().contains("CR 16"));

            // Activities and responsibilities may be parsed later; we accept null or present
            // Issue date and pdfGeneratedAt check (partial)
            assertTrue(resp.getPdfGeneratedAt() == null || resp.getPdfGeneratedAt().contains("2022-04-25"));
        }
    }
}
