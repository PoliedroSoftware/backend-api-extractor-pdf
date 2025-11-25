package co.com.extractor.appservice.adapter;

import co.com.extractor.domain.gateways.PdfTextExtractorPort;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.util.Objects;

@Component
@ConditionalOnMissingBean(PdfTextExtractorPort.class)
public class DefaultPdfBoxTextExtractor implements PdfTextExtractorPort {
    @Override
    public String extractText(InputStream fileStream, String originalFilename) throws Exception {
        Objects.requireNonNull(fileStream, "fileStream required");
        try (InputStream is = fileStream; PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setWordSeparator(" ");
            stripper.setLineSeparator(" ");
            stripper.setParagraphStart("");
            stripper.setParagraphEnd(" ");
            return stripper.getText(document);
        }
    }
}

