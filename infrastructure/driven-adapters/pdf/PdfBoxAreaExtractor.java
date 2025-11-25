package co.com.extractor.infrastructure.drivenadapters.pdf;

import co.com.extractor.model.gateways.AreaExtractorPort;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.Rectangle;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PdfBoxAreaExtractor implements AreaExtractorPort {
    @Override
    public Map<String, String> extractAreas(InputStream fileStream, String originalFilename) throws Exception {
        try (InputStream is = fileStream; PDDocument document = PDDocument.load(is)) {
            Map<String, String> out = new HashMap<>();
            if (document.getNumberOfPages() == 0) return out;
            PDPage page = document.getPage(0);
            PDRectangle mediaBox = page.getMediaBox();
            float w = mediaBox.getWidth();
            float h = mediaBox.getHeight();

            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);

            Rectangle formRect = new Rectangle((int)(w*0.72), (int)(h*0.78), (int)(w*0.22), (int)(h*0.06));
            Rectangle nitRect = new Rectangle((int)(w*0.02), (int)(h*0.58), (int)(w*0.38), (int)(h*0.06));
            Rectangle namesRect = new Rectangle((int)(w*0.03), (int)(h*0.50), (int)(w*0.94), (int)(h*0.12));
            Rectangle addrRect = new Rectangle((int)(w*0.03), (int)(h*0.40), (int)(w*0.90), (int)(h*0.07));
            Rectangle postRect = new Rectangle((int)(w*0.20), (int)(h*0.36), (int)(w*0.14), (int)(h*0.05));

            stripper.addRegion("formNumber", formRect);
            stripper.addRegion("nit", nitRect);
            stripper.addRegion("names", namesRect);
            stripper.addRegion("address", addrRect);
            stripper.addRegion("postal", postRect);

            stripper.extractRegions(page);

            String form = safeTrim(stripper.getTextForRegion("formNumber"));
            String nit = safeTrim(stripper.getTextForRegion("nit"));
            String names = safeTrim(stripper.getTextForRegion("names"));
            String addr = safeTrim(stripper.getTextForRegion("address"));
            String postal = safeTrim(stripper.getTextForRegion("postal"));

            if (form != null && !form.isEmpty()) out.put("formNumber", normalizeDigits(form));
            if (nit != null && !nit.isEmpty()) out.put("nit", normalizeDigits(nit));
            if (names != null && !names.isEmpty()) out.put("names", names.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll("\\s+", " ").trim());
            if (addr != null && !addr.isEmpty()) out.put("address", addr.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll("\\s+", " ").trim());
            if (postal != null && !postal.isEmpty()) {
                out.put("postalRaw", postal.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll("\\s+", " ").trim());
                out.put("postal", normalizeDigits(postal));
            }

            return out;
        }
    }

    private String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String normalizeDigits(String s) {
        if (s == null) return null;
        String only = s.replaceAll("\\D+", "");
        return only.isEmpty() ? null : only;
    }
}
