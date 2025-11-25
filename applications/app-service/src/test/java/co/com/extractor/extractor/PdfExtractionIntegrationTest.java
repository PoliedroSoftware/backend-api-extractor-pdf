package co.com.extractor.extractor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdfExtractionIntegrationTest {

    private InputStream openSamplePdf() throws Exception {
        Path p1 = Paths.get("RUT_EDUAR_SANCHEZ.pdf");
        if (Files.exists(p1)) return new FileInputStream(p1.toFile());
        Path p2 = Paths.get(System.getProperty("user.dir"), "RUT_EDUAR_SANCHEZ.pdf");
        if (Files.exists(p2)) return new FileInputStream(p2.toFile());
        InputStream r = this.getClass().getClassLoader().getResourceAsStream("RUT_EDUAR_SANCHEZ.pdf");
        if (r != null) return r;
        throw new IllegalStateException("Sample PDF RUT_EDUAR_SANCHEZ.pdf not found in repo root, project root or classpath");
    }

    @Test
    public void extractTextFromSamplePdf() throws Exception {
        try (InputStream fis = openSamplePdf()) {
            MockMultipartFile mm = new MockMultipartFile("file", "RUT_EDUAR_SANCHEZ.pdf", "application/pdf", fis);
            PdfTextExtractor extractor = new PdfTextExtractor();
            String text = extractor.extractText(mm);
            // Basic assertions: must contain NIT and name fragments
            assertTrue(text.contains("10916585513") || text.contains("1091658551"), "Expected NIT in extracted text");
            assertTrue(text.toUpperCase().contains("SANCHEZ"), "Expected last name in extracted text");
        }
    }
}
