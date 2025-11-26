package co.com.extractor.service;

import co.com.extractor.api.dto.RutResponse;
import co.com.extractor.extractor.PdfTextExtractor;
import co.com.extractor.extractor.AreaExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RutParserServiceDianSectionalTest {

    @Test
    public void parse_shouldCleanDianSectional_removeIdentificacionSuffix() throws Exception {
        byte[] pdfBytes = Files.readAllBytes(Path.of("RUT_EDUAR_SANCHEZ.pdf"));
        MockMultipartFile file = new MockMultipartFile("file", "RUT_EDUAR_SANCHEZ.pdf", "application/pdf", pdfBytes);

        PdfTextExtractor extractor = new PdfTextExtractor();
        AreaExtractor areaExtractor = new AreaExtractor();
        RutParserService service = new RutParserService(extractor, areaExtractor);

        RutResponse resp = service.parse(file);
        assertNotNull(resp, "Response should not be null");
        Map<String,Object> raw = resp.getRaw();
        assertNotNull(raw, "raw map should not be null");
        Object dian = raw.get("dianSectional");
        assertNotNull(dian, "dianSectional must be present");
        String ds = String.valueOf(dian).trim();
        assertFalse(ds.toUpperCase().contains("IDENTIFIC"), "dianSectional must not contain IDENTIFICACIÓN");
        // Expect exactly 'Impuestos de Cúcuta' per example
        assertTrue(ds.startsWith("Impuestos de Cúcuta"), "dianSectional should start with 'Impuestos de Cúcuta'");
    }
}

