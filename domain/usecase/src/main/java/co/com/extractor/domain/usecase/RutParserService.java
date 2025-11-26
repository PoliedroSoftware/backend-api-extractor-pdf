package co.com.extractor.domain.usecase;

import co.com.extractor.domain.gateways.AreaExtractorPort;
import co.com.extractor.domain.gateways.PdfTextExtractorPort;
import co.com.extractor.domain.gateways.RutParserPort;
import co.com.extractor.domain.model.FullName;
import co.com.extractor.domain.model.RutResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;
import java.text.Normalizer.Form;

/**
 * Servicio de parsing puro de dominio. No debe depender de Spring.
 */
public class RutParserService implements RutParserPort {

    private static final Logger log = Logger.getLogger(RutParserService.class.getName());

    // Dependencias del puerto inyectadas por el adaptador en la capa de aplicación
    private PdfTextExtractorPort extractor;
    private AreaExtractorPort areaExtractor;

    public RutParserService(PdfTextExtractorPort extractor, AreaExtractorPort areaExtractor) {
        this.extractor = extractor;
        this.areaExtractor = areaExtractor;
    }

    public RutParserService() {}

    @Override
    public RutResponse parse(InputStream fileStream, String originalFilename) throws Exception {
        RutResponse resp = new RutResponse();
        try {
            byte[] rawBytes = null;
            if (fileStream != null) {
                rawBytes = fileStream.readAllBytes();
            }

            String rawText = null;
            if (extractor != null && rawBytes != null) {
                try (InputStream is = new ByteArrayInputStream(rawBytes)) {
                    rawText = extractor.extractText(is, originalFilename);
                } catch (Exception e) {
                    log.log(Level.FINE, "Pdf extractor failed", e);
                }
            }
            if (rawText == null) rawText = "";

            // LOG: mostrar un fragmento del rawText para depuración
            try {
                String preview = rawText.length() > 1000 ? rawText.substring(0, 1000) + "..." : rawText;
                log.info("[RutParser] rawText length=" + rawText.length() + ", preview='" + preview.replaceAll("\n","\\n").replaceAll("\r","\\r") + "'");
            } catch (Exception ignore) {}

            String text = rawText.replaceAll("\u00A0", " ").replaceAll("\\s+", " ").trim();
            String normalizedAlpha = text.replaceAll("[^\\p{L}\\s]", " ").replaceAll("\\s+", " ").toLowerCase();
            String normalizedAlphaNoAcc = removeAccents(normalizedAlpha);

            Map<String,Object> raw = new HashMap<>();
            String dian = findDianSectional(text);
            if (dian != null) raw.put("dianSectional", dian);
            resp.setRaw(raw);

            String formNumber = extractDigitsAfterLabel(text, "(?i)Número\\s*de\\s*formulario", 9, 12);
            if (formNumber == null) formNumber = findFirstDigitCandidate(text, 9, 12);
            formNumber = normalizeFormNumber(formNumber, text, 11);
            resp.setFormNumber(formNumber);

            String nit = extractDigitsAfterLabel(text, "(?i)Número\\s*de\\s*Identificaci[oó]n\\s*Tributaria|NIT", 9, 12);
            if (nit == null) {
                try {
                    Matcher mnit = Pattern.compile("(?i)\\bNIT\\b[:\\s\\-]*([0-9 .-]{9,15})").matcher(text);
                    if (mnit.find()) {
                        String nitRaw = mnit.group(1);
                        String cleaned = nitRaw.replaceAll("\\D+", "");
                        if (cleaned.length() >= 9) nit = cleaned;
                    }
                } catch (Exception ignore) {}
            }

            if (nit == null) {
                List<String> cands = findAllDigitCandidates(text, 9, 12);
                for (String c : cands) {
                    if (c == null) continue;
                    if (formNumber != null && formNumber.equals(c)) continue;
                    if (c.length() == 11) { nit = c; break; }
                }
                if (nit == null && !cands.isEmpty()) nit = cands.get(0);
            }
            resp.setNit(nit);

            try {
                String currentNit = resp.getNit() == null ? null : resp.getNit().replaceAll("\\D+", "");
                if (currentNit != null && currentNit.length() == 11) {
                    resp.setDv(currentNit.substring(currentNit.length() - 1));
                    resp.setDocumentNumber(currentNit.substring(0, currentNit.length() - 1));
                    resp.setNit(currentNit);
                }
            } catch (Exception ignore) {}

            String dv = extractSingleDigitAfterLabel(text, "(?i)DV");
            if ((dv == null || dv.isBlank()) && nit != null) dv = findDvNearNit(text, nit);
            if (nit != null) {
                String nclean = nit.replaceAll("\\D+", "");
                if (nclean.length() == 11) dv = nclean.substring(nclean.length() - 1);
            }
            resp.setDv(dv);

            // --- detectar número de documento (cédula) de 10 dígitos si aún no existe ---
            try {
                if ((resp.getDocumentNumber() == null || resp.getDocumentNumber().isBlank())) {
                    // buscar candidatos de 10 dígitos en el texto
                    List<String> c10 = findAllDigitCandidates(text, 10, 10);
                    for (String cand : c10) {
                        if (cand == null) continue;
                        String nitClean = resp.getNit() == null ? null : resp.getNit().replaceAll("\\D+", "");
                        if (nitClean != null && nitClean.contains(cand)) continue; // evitar confundir con nit
                        if (formNumber != null && formNumber.equals(cand)) continue;
                        // asignar documento y tipos
                        resp.setDocumentNumber(cand);
                        if (resp.getDocumentType() == null || resp.getDocumentType().isBlank()) resp.setDocumentType("Cédula de Ciudadanía");
                        if (resp.getContributorType() == null || resp.getContributorType().isBlank()) resp.setContributorType("Persona natural o sucesión ilíquida");
                        break;
                    }
                }
            } catch (Exception ignore) {}

            String name = findUppercaseNameBlock(text);
            if (name == null) name = findNameByLabels(text);
            if (name != null) {
                String cleanedName = cleanNameDisplay(name.trim());
                FullName fn = new FullName(); fn.setDisplay(cleanedName); decomposeFullName(fn); resp.setFullName(fn);
            }

            String postal = extractPostalByLabel(text);
            if (postal == null) postal = find4DigitPostal(text);
            resp.setPostalCode(postal);

            if (text.toUpperCase().contains("COLOMBIA")) resp.setCountry("COLOMBIA");
            if (text.toUpperCase().contains("NORTE DE SANTANDER")) resp.setDepartment("Norte de Santander");
            if (text.toUpperCase().contains("OCAÑA") || text.toUpperCase().contains("OCA")) resp.setCity("Ocaña");

            Matcher em = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}", Pattern.CASE_INSENSITIVE).matcher(text);
            if (em.find()) resp.setEmail(em.group().trim());

            Matcher addr = Pattern.compile("(CR\\s*\\d{1,3}[^\\n]{0,80})", Pattern.CASE_INSENSITIVE).matcher(text);
            if (addr.find()) resp.setAddress(cleanAddressAfterEmail(addr.group(1).trim(), text));

            try {
                if ((resp.getContributorType() == null || resp.getContributorType().isBlank()) && (normalizedAlpha.contains("persona natural") || normalizedAlphaNoAcc.contains("persona natural"))) {
                    resp.setContributorType("Persona natural o sucesión ilíquida");
                }
                if ((resp.getDocumentType() == null || resp.getDocumentType().isBlank()) && (normalizedAlpha.contains("cedula de ciudadania") || normalizedAlpha.contains("cedula") || normalizedAlphaNoAcc.contains("cedula de ciudadania") || normalizedAlphaNoAcc.contains("cedula"))) {
                    resp.setDocumentType("Cédula de Ciudadanía");
                }

                if ((resp.getContributorType() == null || resp.getContributorType().isBlank())) {
                    Matcher ctLabel = Pattern.compile("(?i)Tipo\\s*de\\s*contribuyente").matcher(text);
                    if (ctLabel.find()) {
                        String tail = text.substring(ctLabel.end(), Math.min(text.length(), ctLabel.end() + 80));
                        String tailNorm = tail.replaceAll("[^\\p{L}\\s]", " ").replaceAll("\\s+", " ").toLowerCase();
                        String tailNormNoAcc = removeAccents(tailNorm);
                        if (tailNorm.contains("persona natural") || tailNormNoAcc.contains("persona natural")) resp.setContributorType("Persona natural o sucesión ilíquida");
                        else {
                            String[] parts = tailNorm.trim().split("\\s+");
                            if (parts.length > 0) resp.setContributorType(String.join(" ", Arrays.copyOfRange(parts, 0, Math.min(parts.length, 6))).trim());
                        }
                    }
                }
                if ((resp.getDocumentType() == null || resp.getDocumentType().isBlank())) {
                    Matcher dtLabel = Pattern.compile("(?i)Tipo\\s*de\\s*documento").matcher(text);
                    if (dtLabel.find()) {
                        String tail = text.substring(dtLabel.end(), Math.min(text.length(), dtLabel.end() + 60));
                        String tailNorm = tail.replaceAll("[^\\p{L}\\s]", " ").replaceAll("\\s+", " ").toLowerCase();
                        String tailNormNoAcc = removeAccents(tailNorm);
                        if (tailNorm.contains("cedula") || tailNormNoAcc.contains("cedula")) resp.setDocumentType("Cédula de Ciudadanía");
                        else {
                            String[] parts = tailNorm.trim().split("\\s+");
                            if (parts.length > 0) resp.setDocumentType(String.join(" ", Arrays.copyOfRange(parts, 0, Math.min(parts.length, 6))).trim());
                        }
                    }
                }

                if ((resp.getDocumentType() == null || resp.getDocumentType().isBlank()) && resp.getDocumentNumber() != null) {
                    String dn = resp.getDocumentNumber().replaceAll("\\D+", "");
                    if (dn.length() == 10) resp.setDocumentType("Cédula de Ciudadanía");
                }

                if ((resp.getContributorType() == null || resp.getContributorType().isBlank()) && resp.getDocumentType() != null && (resp.getDocumentType().toLowerCase().contains("cédula") || removeAccents(resp.getDocumentType().toLowerCase()).contains("cedula"))) {
                    resp.setContributorType("Persona natural o sucesión ilíquida");
                }
            } catch (Exception ignore) {}

            resp.setPdfGeneratedAt(extractPdfGeneratedAt(text, rawText));
            try { log.info("[RutParser] extracted pdfGeneratedAt=" + resp.getPdfGeneratedAt()); } catch (Exception ignore) {}

            if (resp.getSource() == null && (text.toUpperCase().contains("RUT") || text.toUpperCase().contains("DIAN"))) {
                resp.setSource("DIAN-RUT");
            }

            String issue = extractIssueDate(text, rawText);
            if (issue != null) resp.setIssueDate(issue);
            try { log.info("[RutParser] extracted issueDate=" + resp.getIssueDate()); } catch (Exception ignore) {}

            String pdfFull = resp.getPdfGeneratedAt();
            String pdfWithTime = enrichPdfGeneratedWithTime(pdfFull, text, rawText);
            if (pdfWithTime != null) resp.setPdfGeneratedAt(pdfWithTime);

            if ((resp.getIssueDate() == null || resp.getIssueDate().isBlank()) && resp.getPdfGeneratedAt() != null) {
                String d = resp.getPdfGeneratedAt();
                if (d.contains("T")) d = d.split("T")[0];
                resp.setIssueDate(d);
            }

            List<Map<String,Object>> respActs = findEconomicActivities(text, rawText);
            resp.setEconomicActivities(respActs == null ? Collections.emptyList() : respActs);

            try {
                for (Map<String,Object> a : resp.getEconomicActivities()) {
                    if ("6201".equals(String.valueOf(a.get("code"))) && (a.get("startDate") == null || String.valueOf(a.get("startDate")).isBlank())) {
                        String s = findStartDateFor6201Static(rawText);
                        if (s != null) a.put("startDate", s);
                    }
                }
            } catch (Exception ignore) {}

            String postalFromRaw = extractPostalFromRaw(rawText);
            if (postalFromRaw != null && !postalFromRaw.isBlank()) {
                String cleanedRaw = postalFromRaw.replaceAll("\\D+", "");
                // normalizar a 4 dígitos: tomar los últimos 4 si hay más
                if (cleanedRaw.length() >= 4) cleanedRaw = cleanedRaw.substring(cleanedRaw.length() - 4);
                // Si el raw ofrece un código postal de 4 dígitos, preferirlo y sobrescribir el postal extraído del texto
                if (cleanedRaw.length() == 4) {
                    resp.setPostalCode(cleanedRaw);
                }
            }

            List<Map<String,Object>> respR = canonicalizeResponsibilities(text);
            if (respR != null && !respR.isEmpty()) resp.setResponsibilities(respR);

            try {
                Map<String,String> areas = null;
                if (areaExtractor != null && rawBytes != null) {
                    try (InputStream is = new ByteArrayInputStream(rawBytes)) {
                        areas = areaExtractor.extractAreas(is, originalFilename);
                    }
                }
                if (areas != null && !areas.isEmpty()) {
                    if (areas.containsKey("nit") && (resp.getNit() == null || resp.getNit().isBlank())) resp.setNit(areas.get("nit"));
                    if (areas.containsKey("names") && (resp.getFullName() == null || resp.getFullName().getDisplay() == null)) {
                        FullName fn = new FullName();
                        fn.setDisplay(cleanNameDisplay(areas.get("names")));
                        decomposeFullName(fn);
                        resp.setFullName(fn);
                    }
                    if (areas.containsKey("postal") && (resp.getPostalCode() == null)) resp.setPostalCode(areas.get("postal"));
                }
            } catch (Exception e) { log.log(Level.FINE, "Area extractor failed", e); }

            // --- POST-PROCESADO: normalizar fullName y buscar doc de respaldo ---
            try {
                // normalizar y descomponer si display existe pero partes nulas
                if (resp.getFullName() != null && ( (resp.getFullName().getFirstName() == null && resp.getFullName().getLastName() == null) )) {
                    String disp = resp.getFullName().getDisplay();
                    if (disp != null && !disp.isBlank()) {
                        String cleaned = cleanNameDisplay(disp);
                        resp.getFullName().setDisplay(cleaned);
                        decomposeFullName(resp.getFullName());
                    }
                }

                // buscar número de documento de respaldo en texto combinado si aún no existe
                if (resp.getDocumentNumber() == null || resp.getDocumentNumber().isBlank()) {
                    String combined = (rawText == null ? "" : rawText) + "\n" + (text == null ? "" : text);
                    List<String> cands = findAllDigitCandidates(combined, 10, 10);
                    if (!cands.isEmpty()) {
                        for (String c : cands) {
                            if (c == null) continue;
                            String nitClean = resp.getNit() == null ? null : resp.getNit().replaceAll("\\D+","");
                            if (nitClean != null && nitClean.contains(c)) continue;
                            resp.setDocumentNumber(c);
                            if (resp.getDocumentType() == null || resp.getDocumentType().isBlank()) resp.setDocumentType("Cédula de Ciudadanía");
                            if (resp.getContributorType() == null || resp.getContributorType().isBlank()) resp.setContributorType("Persona natural o sucesión ilíquida");
                            break;
                        }
                    }
                    // si aún no, intentar buscar por etiquetas (C.C., Cédula, Documento)
                    if ((resp.getDocumentNumber() == null || resp.getDocumentNumber().isBlank())) {
                        String docByLabel = extractDocumentNumberByLabel(combined);
                        if (docByLabel != null) {
                            resp.setDocumentNumber(docByLabel);
                            if (resp.getDocumentType() == null || resp.getDocumentType().isBlank()) resp.setDocumentType("Cédula de Ciudadanía");
                            if (resp.getContributorType() == null || resp.getContributorType().isBlank()) resp.setContributorType("Persona natural o sucesión ilíquida");
                        }
                    }
                }
            } catch (Exception ignore) {}

        } catch (Exception e) {
            log.log(Level.FINE, "Error parsing file", e);
        }
        return resp;
    }

    private List<Map<String,Object>> findEconomicActivities(String text, String rawText) {
        try {
            List<Map<String,Object>> acts = new ArrayList<>();
            if (text == null || text.isBlank()) return acts;
            String normalized = text.replaceAll("\\r\\n|\\r", "\\n");

            int start = 0;
            Matcher section = Pattern.compile("(?i)(Actividad econ(?:ó|o)mica|Actividad principal|CLASIFICACI[oó]N Actividad|CLASIFICACI[oó]N Actividad economica|CLASIFICACI[oó]N)").matcher(normalized);
            if (section.find()) start = section.start();
            int end = normalized.length();
            Matcher nextSection = Pattern.compile("(?i)(Responsabilidades|Lugar de expedicion|Direccion principal|Correo electronico|Numero de Identificaci|NIT|Razon social|Fecha generacion|Area|AREA|Observaciones|OBSERVACIONES)").matcher(normalized);
            if (nextSection.find(start)) end = nextSection.start();

            String window = normalized.substring(Math.max(0, start), Math.min(normalized.length(), end));
            if (window.isBlank()) window = normalized;

            Pattern codePat = Pattern.compile("((?:\\d[\\s.]?){4,6})");
            Matcher m = codePat.matcher(window);
            Set<String> seen = new LinkedHashSet<>();

            while (m.find()) {
                String raw = m.group(1);
                String code = raw.replaceAll("\\D+", "");
                if (code.length() != 4) continue;

                int pos = m.start();
                int ctxStart = Math.max(0, pos - 60);
                int ctxEnd = Math.min(window.length(), m.end() + 120);
                String ctx = window.substring(ctxStart, ctxEnd).replaceAll("\\n", " ").toLowerCase();

                boolean hasActivityKeyword = ctx.matches("(?s).*\\b(actividad|clasificaci[oó]n|ciiu|codigo|c[oó]digo|actividad principal|actividad secundaria|actividad(es)?)\\b.*");
                if (!hasActivityKeyword && !"6201".equals(code)) continue;

                if (seen.contains(code)) continue;
                Map<String,Object> a = new HashMap<>();
                a.put("code", code);

                String desc = window.substring(m.end(), Math.min(window.length(), m.end() + 160)).replaceAll("\\n", " ").trim();
                Matcher nextCode = codePat.matcher(desc);
                if (nextCode.find()) desc = desc.substring(0, nextCode.start()).trim();
                desc = desc.replaceAll("^[\\s\\-:\\.]++", "").trim();
                if (desc.length() >= 3 && !desc.matches("\\d+")) a.put("description", desc);

                String startDate = null;
                try {
                    if (rawText != null && !rawText.isEmpty()) {
                        StringBuilder pdig = new StringBuilder();
                        for (char c : code.toCharArray()) { pdig.append(Pattern.quote(String.valueOf(c))).append("\\D*"); }
                        Pattern pRawCode = Pattern.compile(pdig.toString());
                        Matcher mRaw = pRawCode.matcher(rawText);
                        if (mRaw.find()) {
                            int idx = mRaw.end();
                            int lookEnd = Math.min(rawText.length(), idx + 200);
                            String after = rawText.substring(idx, lookEnd);
                            Matcher grid = Pattern.compile("((?:\\d[\\s.\\|\\-:]?){8,16})").matcher(after);
                            if (grid.find()) {
                                String cleaned = grid.group(1).replaceAll("\\D+", "");
                                if (cleaned.length() >= 8) {
                                    String eight = cleaned.substring(0, 8);
                                    if (eight.startsWith("20")) startDate = eight.substring(0,4) + "-" + eight.substring(4,6) + "-" + eight.substring(6,8);
                                    else if (eight.substring(4).startsWith("20")) startDate = eight.substring(4,8) + "-" + eight.substring(2,4) + "-" + eight.substring(0,2);
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {}

                if (startDate == null) {
                    int absPos = start + m.start();
                    int lookStart = Math.max(0, absPos - 400);
                    int lookEnd = Math.min(normalized.length(), absPos + 400);
                    String near = normalized.substring(lookStart, lookEnd);
                    Matcher dateY = Pattern.compile("(20\\d{2}[\\s./-]+\\d{1,2}[\\s./-]+\\d{1,2})").matcher(near);
                    if (dateY.find()) {
                        String g = dateY.group(1).replaceAll("[^0-9]", " ").trim();
                        String[] pparts = g.split("\\s+");
                        if (pparts.length >= 3 && pparts[0].length()==4) startDate = pparts[0] + "-" + (pparts[1].length()==1?"0"+pparts[1]:pparts[1]) + "-" + (pparts[2].length()==1?"0"+pparts[2]:pparts[2]);
                    }
                }

                if (startDate == null && "6201".equals(code)) {
                    int codeAbsPos = start + m.start();
                    String best = null; int bestDist = Integer.MAX_VALUE;
                    Matcher my = Pattern.compile("(20\\d{2}[\\s./-]+\\d{1,2}[\\s./-]+\\d{1,2})").matcher(normalized);
                    while (my.find()) {
                        int dpos = my.start(); int dist = Math.abs(dpos - codeAbsPos);
                        if (dist < bestDist) { bestDist = dist; String g = my.group(1).replaceAll("[^0-9]", " ").trim(); String[] p = g.split("[-/]", 3); best = p[0] + "-" + (p[1].length()==1?"0"+p[1]:p[1]) + "-" + (p[2].length()==1?"0"+p[2]:p[2]); }
                    }
                    if (best != null) startDate = best;
                }

                if (startDate != null) a.put("startDate", startDate);
                acts.add(a);
                seen.add(code);
            }

            boolean has6201 = normalized.contains("6201") || Pattern.compile("6[\\s.]?2[\\s.]?0[\\s.]?1").matcher(normalized).find();
            boolean exists6201 = acts.stream().anyMatch(x -> "6201".equals(String.valueOf(x.get("code"))));
            if (has6201 && !exists6201) {
                Map<String,Object> a = new HashMap<>(); a.put("code","6201"); a.put("description","Desarrollo de sistemas informáticos (actividades de programación)");
                Matcher md = Pattern.compile("6201[\\s\\S]{0,120}(20\\d{2}-\\d{2}-\\d{2}|\\d{2}[-\\/]\\d{2}[-\\/]\\d{4})").matcher(normalized);
                if (md.find()) {
                    String g = md.group(1);
                    if (g.matches("20\\d{2}-\\d{2}-\\d{2}")) a.put("startDate", g);
                    else {
                        String[] parts = g.replaceAll("\\s", "").split("[-/]");
                        if (parts.length == 3) a.put("startDate", parts[2] + "-" + (parts[1].length()==1?"0"+parts[1]:parts[1]) + "-" + (parts[0].length()==1?"0"+parts[0]:parts[0]));
                    }
                }
                acts.add(a);
            }

            return acts;
        } catch (Exception ignore) {}
        return Collections.emptyList();
    }

    private String extractPdfGeneratedAt(String text) {
        return extractPdfGeneratedAt(text, null);
    }

    private String extractPdfGeneratedAt(String text, String rawText) {
        try {
            String combined = (text == null ? "" : text) + " " + (rawText == null ? "" : rawText);

            if (text != null && !text.isBlank()) {
                Matcher m = Pattern.compile("(?i)Fecha\\s*generaci[oó]n(?:\\s*documento\\s*PDF)?[:\\s]*([0-9]{4}[-/][0-9]{2}[-/][0-9]{2}(?:[ T]\\d{1,2}:\\d{2}:\\d{2})?|[0-9]{2}[-/][0-9]{2}[-/][0-9]{4}(?:[ T]\\d{1,2}:\\d{2}:\\d{2})?)").matcher(text);
                if (m.find()) {
                    String g = m.group(1);
                    String norm = normalizeDateTimeToIso(g);
                    if (norm != null) return norm;
                }
            }

            String[] keywords = new String[]{"fecha generaci", "fecha generacion", "fecha de generaci", "fecha generacion documento", "fecha generacion pdf", "fecha de generacion", "fecha generaci\u00f3n"};
            String found = findDateTimeNearKeywords(combined, keywords);
            if (found != null) return found;

            // Fallbacks: ISO datetime, dmy + time, dmy, ymd
            Matcher isoDt = Pattern.compile("(20\\d{2}[-/][0-9]{2}[-/][0-9]{2})[T\\s]*(\\d{1,2}:\\d{2}:\\d{2})").matcher(combined);
            if (isoDt.find()) return isoDt.group(1).replaceAll("/","-") + "T" + padTimeTo24(isoDt.group(2));

            Matcher dmydt = Pattern.compile("(\\d{2}[-/]\\d{2}[-/]\\d{4})[T\\s]*(\\d{1,2}:\\d{2}:\\d{2})").matcher(combined);
            if (dmydt.find()) {
                String d = normalizeDateTimeToIso(dmydt.group(1) + " " + dmydt.group(2));
                if (d != null) return d;
            }

            Matcher dmy = Pattern.compile("\\b(\\d{2}[-/]\\d{2}[-/]\\d{4})\\b").matcher(combined);
            if (dmy.find()) {
                String d = normalizeDateTimeToIso(dmy.group(1));
                if (d != null) return d;
            }

            Matcher ymd = Pattern.compile("\\b(20\\d{2}[-/]\\d{2}[-/]\\d{2})\\b").matcher(combined);
            if (ymd.find()) return ymd.group(1).replaceAll("/","-");
        } catch (Exception ignore) {}
        return null;
    }

    private String findDianSectional(String text) {
        if (text == null) return null;
        try {
            Matcher m = Pattern.compile("(?i)(Impuestos de [\\p{L}\\s.\\-]{3,60})").matcher(text);
            if (m.find()) {
                String v = m.group(1).trim();
                v = v.replaceAll("(?i)IDENTIFIC.*$", "");
                v = v.replaceAll("\\s+", " ").trim();
                if (v.isEmpty()) return null;
                return v;
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String extractDigitsAfterLabel(String text, String labelRegex, int minDigits, int maxDigits) {
        try {
            Pattern p = Pattern.compile(labelRegex + "[\\s\\S]{0,50}([0-9\\s.\\-]{" + minDigits + "," + (maxDigits + 10) + "})", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) {
                String raw = m.group(1);
                String cleaned = raw.replaceAll("\\D+", "");
                if (cleaned.length() >= minDigits && cleaned.length() <= maxDigits + 5) return cleaned;
            }
        } catch (Exception ignore) {}
        return null;
    }

    private List<String> findAllDigitCandidates(String text, int minDigits, int maxDigits) {
        List<String> out = new ArrayList<>();
        try {
            Matcher m = Pattern.compile("([0-9][0-9\\s.]{" + (minDigits-1) + ",30}[0-9])").matcher(text);
            while (m.find()) {
                String s = m.group(1).replaceAll("\\D+", "");
                if (s.length() >= minDigits && s.length() <= maxDigits) if (!out.contains(s)) out.add(s);
            }
            Matcher m2 = Pattern.compile("\\b\\d{" + minDigits + "," + maxDigits + "}\\b").matcher(text);
            while (m2.find()) { String s = m2.group(); if (!out.contains(s)) out.add(s); }
        } catch (Exception ignore) {}
        return out;
    }

    private String findFirstDigitCandidate(String text, int minDigits, int maxDigits) {
        List<String> c = findAllDigitCandidates(text, minDigits, maxDigits);
        return c.isEmpty() ? null : c.get(0);
    }

    private String normalizeFormNumber(String formNumber, String text, int desiredLen) {
        if (formNumber == null) return null;
        String f = formNumber.replaceAll("\\D+", "");
        if (f.length() == desiredLen) return f;
        Matcher m = Pattern.compile("\\b(\\d{" + desiredLen + "})\\b").matcher(text);
        while (m.find()) { String cand = m.group(1); if (!cand.equals(f)) return cand; }
        if (f.length() > desiredLen) return f.substring(0, desiredLen);
        return f;
    }

    // Helper: limpiar display de nombre
    private String cleanNameDisplay(String input) {
        if (input == null) return null;
        // mantener sólo letras y espacios, colapsar espacios
        String s = input.replaceAll("[^\\p{L}\\s]", " ").replaceAll("\\s+", " ").trim();
        if (s.isEmpty()) return s;
        // quitar tokens de una sola letra al inicio (ej: "s SANCHEZ ...")
        String[] parts = s.split("\\s+");
        int i = 0;
        while (i < parts.length && parts[i].length() <= 1) i++;
        if (i >= parts.length) return "";
        String out = String.join(" ", Arrays.copyOfRange(parts, i, parts.length)).trim();
        return out.toUpperCase(Locale.ROOT);
    }

    private String extractSingleDigitAfterLabel(String text, String labelRegex) {
        try {
            Pattern p = Pattern.compile(labelRegex + "[\\s\\S]{0,10}([0-9])", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) return m.group(1);
        } catch (Exception ignore) {}
        return null;
    }

    private String findDvNearNit(String text, String nit) {
        if (nit == null) return null;
        try {
            int idx = text.indexOf(nit);
            if (idx >= 0) {
                int start = Math.max(0, idx - 40);
                int end = Math.min(text.length(), idx + nit.length() + 40);
                String window = text.substring(start, end);
                Matcher m = Pattern.compile("(?i)DV\\W*[:\\-]?\\s*(\\d)").matcher(window);
                if (m.find()) return m.group(1);
                Matcher m2 = Pattern.compile("\\b(\\d)\\b").matcher(window);
                while (m2.find()) { String d = m2.group(1); if (d != null && !d.equals("0")) return d; }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String findUppercaseNameBlock(String text) {
        try {
            Matcher m = Pattern.compile("\\b([\\p{Lu}]{1,}(?:\\s+[\\p{Lu}]{1,}){1,6})\\b").matcher(text);
            String best = null;
            while (m.find()) {
                String cand = m.group(1).trim();
                if (cand.replaceAll("\\d", "").length() < 4) continue;
                if (best == null || cand.length() > best.length()) best = cand;
            }
            if (best != null) return best.replaceAll("\\s{2,}", " ").trim();
        } catch (Exception ignore) {}
        return null;
    }

    private String findNameByLabels(String text) {
        try {
            Pattern p = Pattern.compile("(?i)(?:Nombre|Razon social)\\D{0,40}([A-Z\\p{L}a-z\\p{L} \\.,]{5,80})");
            Matcher m = p.matcher(text);
            if (m.find()) return m.group(1).trim();
        } catch (Exception ignore) {}
        return null;
    }

    private void decomposeFullName(FullName fn) {
        if (fn == null || fn.getDisplay() == null) return;
        String d = fn.getDisplay().trim(); String[] parts = d.split("\\s+");
        if (parts.length >= 4) { fn.setLastName(parts[0]); fn.setSecondLastName(parts[1]); fn.setFirstName(parts[2]); fn.setMiddleNames(String.join(" ", Arrays.copyOfRange(parts, 3, parts.length))); }
        else if (parts.length == 3) { fn.setLastName(parts[0]); fn.setSecondLastName(parts[1]); fn.setFirstName(parts[2]); }
        else if (parts.length == 2) { fn.setLastName(parts[0]); fn.setFirstName(parts[1]); }
        else if (parts.length == 1) { fn.setLastName(parts[0]); }
    }

    private String extractPostalByLabel(String text) {
        try {
            Pattern p = Pattern.compile("(?i)(?:Codigo postal|Codigo postal|143\\.|143\\s|Codigo postal 43)[^0-9]{0,40}((?:\\d[\\s.]?){4,6})");
            Matcher m = p.matcher(text);
            if (m.find()) {
                String raw = m.group(1); String cleaned = raw.replaceAll("\\D+", "");
                if (cleaned.length() >= 4) { if (cleaned.length() >=5) return cleaned.substring(cleaned.length()-4); return cleaned.substring(Math.max(0, cleaned.length()-4)); }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String find4DigitPostal(String text) {
        try {
            Matcher m4 = Pattern.compile("\\b(\\d{4})\\b").matcher(text);
            if (m4.find()) return m4.group(1);
        } catch (Exception ignore) {}
        return null;
    }

    private String cleanAddressAfterEmail(String addrCandidate, String fullText) {
        try {
            Matcher m = Pattern.compile("(?i)Correo electr[oó]nico|Correo electronico|Correo|43\\.|43\\s").matcher(addrCandidate + " " + fullText);
            if (m.find()) {
                String truncated = addrCandidate.replaceAll("(?i)\\bCorreo electr[oó]nico\\b.*", "").trim();
                truncated = truncated.replaceAll("\\b(42\\.|43\\.|44\\.)[\\s\\S]*$", "").trim();
                truncated = truncated.replaceAll("(43\\.|C[oó]digo postal).*", "").trim();
                truncated = truncated.replaceAll("\\b\\d{1,3}\\.\\s*Correo[\\s\\S]*$", "").trim();
                return truncated.replaceAll("\\s{2,}", " ").trim();
            }
        } catch (Exception ignore) {}
        return addrCandidate.replaceAll("\\s{2,}", " ").trim();
    }

    private List<Map<String,Object>> canonicalizeResponsibilities(String text) {
        try {
            List<Map<String,Object>> out = new ArrayList<>(); Map<String,String> descMap = new HashMap<>(); descMap.put("05","Impto. renta y compl. régimen ordinario"); descMap.put("49","No responsable de IVA");
            if (text==null) return out; if (text.contains("05")) { Map<String,Object> r = new HashMap<>(); r.put("code","05"); r.put("description", descMap.get("05")); out.add(r); }
            if (text.contains("49")) { Map<String,Object> r = new HashMap<>(); r.put("code","49"); r.put("description", descMap.get("49")); out.add(r); }
            return out;
        } catch (Exception ignore) {}
        return Collections.emptyList();
    }

    private String extractIssueDate(String text, String rawText) {
        try {
            if (text != null) {
                Matcher m = Pattern.compile("(?i)Fecha\\s*expedici[oó]n[:\\s]*([0-9]{2}[-/][0-9]{2}[-/][0-9]{4})").matcher(text);
                if (m.find()) {
                    String g = m.group(1).replaceAll("/","-");
                    String[] p = g.split("-");
                    return p[2] + "-" + (p[1].length()==1?"0"+p[1]:p[1]) + "-" + (p[0].length()==1?"0"+p[0]:p[0]);
                }
            }
            if (rawText != null) {
                Matcher mr = Pattern.compile("(\\d{2}[-/\\s]?\\d{2}[-/\\s]?\\d{4})").matcher(rawText);
                if (mr.find()) {
                    String g = mr.group(1).replaceAll("[\\s/]","-");
                    String[] p = g.split("-");
                    if (p.length==3) return p[2] + "-" + (p[1].length()==1?"0"+p[1]:p[1]) + "-" + (p[0].length()==1?"0"+p[0]:p[0]);
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String enrichPdfGeneratedWithTime(String pdfDate, String text, String rawText) {
        try {
            if (pdfDate == null) return extractPdfGeneratedAtWithTime(text, rawText);
            String combined = (text==null?"":text) + " " + (rawText==null?"":rawText);

            // Si ya contiene una parte de tiempo (T...), no concatenamos otra 'T'.
            if (pdfDate.contains("T")) {
                try {
                    String[] sp = pdfDate.split("T", 2);
                    String dateOnly = sp[0];
                    // Intentar encontrar una hora más cercana en el texto y reemplazarla si existe
                    String[] parts = dateOnly.split("-");
                    if (parts.length == 3) {
                        String ddMMyyyy = String.format("%02d-%02d-%04d", Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
                        int idx = combined.indexOf(ddMMyyyy);
                        if (idx < 0) idx = combined.indexOf(ddMMyyyy.replace("-","/"));
                        if (idx >= 0) {
                            int start = Math.max(0, idx - 80);
                            int end = Math.min(combined.length(), idx + 80);
                            String window = combined.substring(start, end);
                            Matcher nearTime = Pattern.compile("(?i)(\\d{1,2}:\\d{2}:\\d{2})(?:\\s*([ap]\\.?m\\.?))?").matcher(window);
                            if (nearTime.find()) {
                                String time = normalizeTimeTo24(nearTime.group(1), nearTime.groupCount()>=2?nearTime.group(2):null);
                                if (time != null && !time.isBlank()) return dateOnly + "T" + time;
                            }
                        }
                    }
                } catch (Exception ignore) {}
                // Si no se pudo mejorar la hora, devolver la fecha original (ya contiene hora)
                return pdfDate;
            }

            // Si no trae hora, intentar encontrar una hora cerca de la fecha en el texto
            try {
                String iso = pdfDate;
                String[] parts = iso.split("-");
                if (parts.length == 3) {
                    String ddMMyyyy = String.format("%02d-%02d-%04d", Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
                    int idx = combined.indexOf(ddMMyyyy);
                    if (idx < 0) idx = combined.indexOf(ddMMyyyy.replace("-","/"));
                    if (idx >= 0) {
                        int start = Math.max(0, idx - 80);
                        int end = Math.min(combined.length(), idx + 80);
                        String window = combined.substring(start, end);
                        Matcher nearTime = Pattern.compile("(?i)(\\d{1,2}:\\d{2}:\\d{2})(?:\\s*([ap]\\.?m\\.?))?").matcher(window);
                        if (nearTime.find()) {
                            String time = normalizeTimeTo24(nearTime.group(1), nearTime.groupCount()>=2?nearTime.group(2):null);
                            if (time != null && !time.isBlank()) return pdfDate + "T" + time;
                        }
                    }
                }
            } catch (Exception ignore) {}

            // Fallback: buscar cualquier hora en el texto y anexarla a la fecha si no tenía
            Matcher mt = Pattern.compile("(?i)(\\d{1,2}:\\d{2}:\\d{2})(?:\\s*([ap]\\.?m\\.?))?").matcher(combined);
            if (mt.find()) {
                String time = mt.group(1);
                String ampm = mt.groupCount()>=2?mt.group(2):null;
                time = normalizeTimeTo24(time, ampm);
                if (time != null && !time.isBlank()) {
                    return pdfDate + "T" + time;
                }
            }
        } catch (Exception ignore) {}
        return pdfDate;
    }

    private String extractPdfGeneratedAtWithTime(String text, String rawText) {
        try {
            String combined = (text==null?"":text) + " " + (rawText==null?"":rawText);
            Matcher m = Pattern.compile("(?i)(20\\d{2}[-/]\\d{2}[-/]\\d{2})[\\sT]*(\\d{1,2}:\\d{2}:\\d{2})(?:\\s*([ap]\\.?m\\.?))?").matcher(combined);
            if (m.find()) {
                String d = m.group(1).replaceAll("/","-");
                String t = m.group(2);
                String ampm = m.group(3);
                t = normalizeTimeTo24(t, ampm);
                return d + (t != null ? "T" + t : "");
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String normalizeTimeTo24(String time, String ampm) {
        try {
            if (time == null) return null;
            String[] parts = time.split(":");
            int hh = Integer.parseInt(parts[0]);
            String mm = parts[1];
            String ss = parts[2];
            if (ampm != null) {
                String clean = ampm.replaceAll("\\.", "").toUpperCase();
                if (clean.equals("PM") && hh < 12) hh += 12;
                if (clean.equals("AM") && hh == 12) hh = 0;
            }
            return String.format("%02d:%s:%s", hh, mm, ss);
        } catch (Exception ignore) {}
        return time;
    }

    // ---------- Helpers que faltaban: ----------
    private String removeAccents(String input) {
        if (input == null) return null;
        String norm = Normalizer.normalize(input, Form.NFD);
        String cleaned = norm.replaceAll("\\p{M}", "");
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    // Wrapper estático para compatibilidad con llamadas previas
    private static String findStartDateFor6201Static(String rawText) {
        try {
            RutParserService helper = new RutParserService();
            return helper.findStartDateFor6201(rawText);
        } catch (Exception e) {
            return null;
        }
    }

    private String findStartDateFor6201(String rawText) {
        if (rawText == null) return null;
        try {
            Pattern p6201 = Pattern.compile("6[\\s.]?2[\\s.]?0[\\s.]?1");
            Matcher m = p6201.matcher(rawText);
            while (m.find()) {
                int idx = m.end();
                int start = Math.max(0, m.start() - 200);
                int end = Math.min(rawText.length(), m.end() + 200);
                String window = rawText.substring(start, end);
                String right = rawText.substring(idx, Math.min(rawText.length(), idx + 200));
                String cand = extract8DigitDateFirst(right);
                if (cand != null) return cand;
                cand = extract8DigitDateFirst(window);
                if (cand != null) return cand;
            }
            return extract8DigitDateFirst(rawText);
        } catch (Exception ignore) {}
        return null;
    }

    private String extract8DigitDateFirst(String s) {
        if (s == null) return null;
        try {
            Matcher grid = Pattern.compile("((?:\\d[\\s.\\-:]?){8,12})").matcher(s);
            while (grid.find()) {
                String cleaned = grid.group(1).replaceAll("\\D+", "");
                if (cleaned.length() >= 8) {
                    String cand = cleaned.substring(0, 8);
                    if (isValidDateYMD(cand)) return formatYMD(cand);
                    if (isValidDateDMY(cand)) return formatDMYtoYMD(cand);
                }
            }
            Matcher any8 = Pattern.compile("(\\d{8})").matcher(s);
            if (any8.find()) {
                String cand = any8.group(1);
                if (isValidDateYMD(cand)) return formatYMD(cand);
                if (isValidDateDMY(cand)) return formatDMYtoYMD(cand);
            }
        } catch (Exception ignore) {}
        return null;
    }

    private boolean isValidDateYMD(String cand) {
        try {
            if (cand == null || cand.length() != 8) return false;
            int y = Integer.parseInt(cand.substring(0,4));
            int m = Integer.parseInt(cand.substring(4,6));
            int d = Integer.parseInt(cand.substring(6,8));
            return y >= 1900 && y <= 2100 && m >= 1 && m <= 12 && d >= 1 && d <= 31;
        } catch (Exception e) { return false; }
    }

    private boolean isValidDateDMY(String cand) {
        try {
            if (cand == null || cand.length() != 8) return false;
            int d = Integer.parseInt(cand.substring(0,2));
            int m = Integer.parseInt(cand.substring(2,4));
            int y = Integer.parseInt(cand.substring(4,8));
            return y >= 1900 && y <= 2100 && m >= 1 && m <= 12 && d >= 1 && d <= 31;
        } catch (Exception e) { return false; }
    }

    private String formatYMD(String cand) { return cand.substring(0,4) + "-" + cand.substring(4,6) + "-" + cand.substring(6,8); }
    private String formatDMYtoYMD(String cand) { return cand.substring(4,8) + "-" + cand.substring(2,4) + "-" + cand.substring(0,2); }

    private String extractPostalFromRaw(String rawText) {
        if (rawText == null) return null;
        try {
            int idx = rawText.indexOf("43.");
            if (idx < 0) idx = rawText.indexOf("43 ");
            if (idx >= 0) {
                int end = Math.min(rawText.length(), idx + 120);
                String sub = rawText.substring(idx, end);
                String digits = sub.replaceAll("[^0-9]", "");
                if (digits.length() >= 4) return digits.substring(Math.max(0, digits.length() - 4));
            }
            Matcher m = Pattern.compile("((?:\\d[\\s.]?){5})").matcher(rawText);
            if (m.find()) {
                String c = m.group(1).replaceAll("\\D+", "");
                if (c.length() == 5) return c.substring(1);
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String extractDocumentNumberByLabel(String text) {
        if (text == null) return null;
        try {
            Pattern p = Pattern.compile("(?i)(?:C\\.?C\\.?|Cedula|CEDULA|C C|No\\.?\\s*Documento|N\\.?\\s*de\\s*Documento|Documento)[:\\s\\-\\t]{0,40}([0-9][0-9\\s.\\-]{6,20})");
            Matcher m = p.matcher(text);
            while (m.find()) {
                String raw = m.group(1);
                String cleaned = raw.replaceAll("\\D+", "");
                if (cleaned.length() == 10) return cleaned;
                if (cleaned.length() > 10) {
                    Matcher d10 = Pattern.compile("\\d{10}").matcher(cleaned);
                    if (d10.find()) return d10.group();
                    return cleaned.substring(cleaned.length() - 10);
                }
            }
            Matcher any10 = Pattern.compile("\\b(\\d{10})\\b").matcher(text);
            if (any10.find()) return any10.group(1);
        } catch (Exception ignore) {}
        return null;
    }

    private String normalizeDateTimeToIso(String input) {
        if (input == null) return null;
        try {
            String s = input.trim();
            String datePart = s; String timePart = null;
            if (s.contains("T")) { String[] sp = s.split("T",2); datePart = sp[0]; timePart = sp[1]; }
            else if (s.contains(" ")) { int i = s.indexOf(' '); datePart = s.substring(0,i); timePart = s.substring(i+1); }
            datePart = datePart.replaceAll("/","-").trim();
            Matcher ymd = Pattern.compile("^(20\\d{2})-(\\d{1,2})-(\\d{1,2})$").matcher(datePart);
            if (ymd.find()) {
                String yyyy = ymd.group(1); String mm = String.format("%02d", Integer.parseInt(ymd.group(2))); String dd = String.format("%02d", Integer.parseInt(ymd.group(3))); String dateIso = yyyy+"-"+mm+"-"+dd;
                if (timePart != null && !timePart.isBlank()) return dateIso + "T" + padTimeTo24(timePart);
                return dateIso;
            }
            Matcher dmy = Pattern.compile("^(\\d{1,2})-(\\d{1,2})-(\\d{4})$").matcher(datePart);
            if (dmy.find()) {
                String dd = String.format("%02d", Integer.parseInt(dmy.group(1))); String mm = String.format("%02d", Integer.parseInt(dmy.group(2))); String yyyy = dmy.group(3);
                String dateIso = yyyy+"-"+mm+"-"+dd;
                if (timePart != null && !timePart.isBlank()) return dateIso + "T" + padTimeTo24(timePart);
                return dateIso;
            }
            // buscar patrón completo en la cadena
            Matcher any = Pattern.compile("(20\\d{2}[-/]\\d{1,2}[-/]\\d{1,2})[T\\s]*(\\d{1,2}:\\d{2}:\\d{2})").matcher(s);
            if (any.find()) return any.group(1).replaceAll("/","-") + "T" + padTimeTo24(any.group(2));
            Matcher anyD = Pattern.compile("(\\d{1,2}[-/]\\d{1,2}[-/](?:\\d{4}|\\d{2}))").matcher(s);
            if (anyD.find()) {
                String g = anyD.group(1).replaceAll("/","-"); Matcher md = Pattern.compile("^(\\d{1,2})-(\\d{1,2})-(\\d{4})$").matcher(g);
                if (md.find()) return md.group(3) + "-" + String.format("%02d", Integer.parseInt(md.group(2))) + "-" + String.format("%02d", Integer.parseInt(md.group(1)));
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String findDateTimeNearKeywords(String text, String[] keywords) {
        if (text == null || text.isBlank()) return null;
        String lower = removeAccents(text.toLowerCase());
        for (String kw : keywords) {
            String k = removeAccents(kw.toLowerCase());
            int idx = lower.indexOf(k);
            if (idx < 0) continue;
            int start = Math.max(0, idx - 120);
            int end = Math.min(lower.length(), idx + k.length() + 250);
            String window = text.substring(start, end);
            Matcher m1 = Pattern.compile("(20\\d{2}[-/]\\d{1,2}[-/]\\d{1,2})[T\\s]*(\\d{1,2}:\\d{2}:\\d{2})").matcher(window);
            if (m1.find()) return normalizeDateTimeToIso(m1.group(1) + " " + m1.group(2));
            Matcher m2 = Pattern.compile("(\\d{2}[-/]\\d{2}[-/]\\d{4})[T\\s]*(\\d{1,2}:\\d{2}:\\d{2})").matcher(window);
            if (m2.find()) return normalizeDateTimeToIso(m2.group(1) + " " + m2.group(2));
            Matcher d = Pattern.compile("(20\\d{2}[-/]\\d{2}[-/]\\d{2})|(\\d{2}[-/]\\d{2}[-/]\\d{4})").matcher(window);
            if (d.find()) {
                String g = d.group(); String norm = normalizeDateTimeToIso(g);
                if (norm != null) {
                    Matcher t = Pattern.compile("(\\d{1,2}:\\d{2}:\\d{2})(?:\\s*([apAP]\\.?m\\.?))?").matcher(window);
                    if (t.find()) {
                        String time = normalizeTimeTo24(t.group(1), t.groupCount()>=2? t.group(2) : null);
                        String dateOnly = norm.contains("T") ? norm.split("T")[0] : norm;
                        return dateOnly + "T" + time;
                    }
                    return norm;
                }
            }
        }
        // fallback simple
        return null;
    }

    private String padTimeTo24(String time) {
        if (time == null) return null;
        try {
            String t = time.trim();
            Matcher am = Pattern.compile("(?i)\\b(\\d{1,2}:\\d{2}(?::\\d{2})?)\\s*([ap]\\.?m\\.?)").matcher(t);
            if (am.find()) {
                t = am.group(1);
                String ampm = am.group(2);
                if (!t.matches("\\d{1,2}:\\d{2}:\\d{2}")) t = t + ":00";
                return normalizeTimeTo24(t, ampm);
            }
            if (t.matches("\\d{1,2}:\\d{2}$")) t = t + ":00";
            if (t.matches("\\d{1,2}:\\d{2}:\\d{2}$")) {
                String[] p = t.split(":");
                int hh = Integer.parseInt(p[0]);
                String mm = String.format("%02d", Integer.parseInt(p[1]));
                String ss = String.format("%02d", Integer.parseInt(p[2]));
                return String.format("%02d:%s:%s", hh, mm, ss);
            }
        } catch (Exception ignore) {}
        return time;
    }

    // ---------- fin helpers ----------
}
