package co.com.extractor.extractor;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StreamUtils;

import java.io.FileInputStream;
import java.nio.file.Paths;

public class PdfTextExtractorRunner {

    @Disabled("Helper to run locally to print extracted text from sample PDF")
    @Test
    public void runExtractorLocally() throws Exception {
        String repoRoot = Paths.get(".").toAbsolutePath().normalize().toString();
        String pdfPath = repoRoot + "/RUT_EDUAR_SANCHEZ.pdf"; // ensure file is at repo root
        try (FileInputStream fis = new FileInputStream(pdfPath)) {
            MockMultipartFile mm = new MockMultipartFile("file", "RUT_EDUAR_SANCHEZ.pdf", "application/pdf", fis);
            PdfTextExtractor extractor = new PdfTextExtractor();
            String text = extractor.extractText(mm);
            System.out.println("---- extracted text ----");
            System.out.println(text);
            System.out.println("------------------------");
        }
    }
}

