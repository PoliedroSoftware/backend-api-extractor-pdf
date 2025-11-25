package co.com.extractor.appservice.adapter;

import co.com.extractor.domain.gateways.RutParserPort;
import co.com.extractor.domain.gateways.PdfTextExtractorPort;
import co.com.extractor.domain.model.RutResponse;
import co.com.extractor.domain.model.FullName;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleRutParserAdapter implements RutParserPort {

    private final PdfTextExtractorPort extractor;

    public SimpleRutParserAdapter(PdfTextExtractorPort extractor) {
        this.extractor = extractor;
    }

    @Override
    public RutResponse parse(InputStream fileStream, String originalFilename) throws Exception {
        String text = extractor.extractText(fileStream, originalFilename);
        RutResponse resp = new RutResponse();
        if (text == null) return resp;

        String normalized = text.replaceAll("\u00A0", " ").replaceAll("\\s+", " ").trim();

        // 1) Buscar NIT por etiquetas comunes (capturar con posibles separadores)
        try {
            Matcher mLabel = Pattern.compile("(?i)(?:NIT|Nit|Numero de Identificacion|Numero de Identificaci[oó]n|N[uú]mero de Identificaci[oó]n)[:\\s]*([0-9 .-]{6,20})").matcher(text);
            if (mLabel.find()) {
                String raw = mLabel.group(1);
                String cleaned = raw.replaceAll("\\D+", "");
                if (cleaned.length() >= 9 && cleaned.length() <= 15) {
                    resp.setNit(cleaned);
                    if (cleaned.length() >= 10) resp.setDv(cleaned.substring(cleaned.length()-1));
                }
            }
        } catch (Exception ignored) {}

        // 2) Si no se encontró, buscar cualquier secuencia de dígitos 9-11 en todo el texto normalizado
        if (resp.getNit() == null) {
            Matcher mDigits = Pattern.compile("\\d{9,11}").matcher(normalized);
            if (mDigits.find()) {
                String nit = mDigits.group(0).replaceAll("\\D+", "");
                resp.setNit(nit);
                if (nit.length()>=10) resp.setDv(nit.substring(nit.length()-1));
            }
        }

        // 3) Fallback con patrones que contienen separadores (puntos, espacios, guiones) y limpiarlos
        if (resp.getNit() == null) {
            Matcher mSep = Pattern.compile("([0-9](?:[0-9 .-]{7,20})[0-9])").matcher(text);
            while (mSep.find()) {
                String cand = mSep.group(1);
                String cleaned = cand.replaceAll("\\D+", "");
                if (cleaned.length() >= 9 && cleaned.length() <= 15) {
                    resp.setNit(cleaned);
                    if (cleaned.length()>=10) resp.setDv(cleaned.substring(cleaned.length()-1));
                    break;
                }
            }
        }

        // 4) Si aún no hay NIT, tomar primeros dígitos encontrados en todo el texto
        if (resp.getNit() == null) {
            String allDigits = text.replaceAll("\\D+", "");
            if (allDigits.length() >= 9) {
                String nit = allDigits.length() >= 11 ? allDigits.substring(0,11) : allDigits.substring(0,9);
                resp.setNit(nit);
                if (nit.length()>=10) resp.setDv(nit.substring(nit.length()-1));
            }
        }

        // Nombre: buscar por etiqueta 'Nombre' o 'Razón social' en el texto original (líneas)
        try {
            Matcher mNameLabel = Pattern.compile("(?i)(?:Nombre|NOMBRE|Raz[oó]n social|Razon social)[:\\s]*([A-Za-zÁÉÍÓÚÑáéíóúñ\\.,\\s]{4,120})").matcher(text);
            if (mNameLabel.find()) {
                String n = mNameLabel.group(1).trim();
                if (!n.isBlank()) {
                    FullName fn = new FullName();
                    fn.setDisplay(n);
                    resp.setFullName(fn);
                }
            }
        } catch (Exception ignored) {}

        // 5) Buscar nombre en mayúsculas como fallback
        if (resp.getFullName() == null) {
            Matcher name = Pattern.compile("\\b([A-ZÁÉÍÓÚÑ]{3,}(?:\\s+[A-ZÁÉÍÓÚÑ]{2,})+)\\b").matcher(normalized);
            if (name.find()) {
                FullName fn = new FullName();
                fn.setDisplay(name.group(1));
                resp.setFullName(fn);
            }
        }

        return resp;
    }
}
