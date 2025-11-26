package co.com.extractor.infrastructure.drivenadapters.pdf;

import co.com.extractor.model.gateways.PdfTextExtractorPort;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.util.Objects;

public class PdfBoxTextExtractor implements PdfTextExtractorPort {
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
