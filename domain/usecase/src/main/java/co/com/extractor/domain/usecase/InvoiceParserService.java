package co.com.extractor.domain.usecase;

import co.com.extractor.domain.gateways.InvoiceParserPort;
import co.com.extractor.domain.gateways.PdfTextExtractorPort;
import co.com.extractor.domain.model.InvoiceResponse;
import co.com.extractor.domain.model.InvoiceResponse.CompanyInfo;
import co.com.extractor.domain.model.InvoiceResponse.InvoiceItem;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio de dominio para el análisis de documentos PDF de facturas.
 * <p>
 * Este servicio utiliza expresiones regulares para extraer datos estructurados
 * del texto plano obtenido de archivos PDF. Implementa
 * {@link InvoiceParserPort}.
 * </p>
 *
 * @see InvoiceParserPort
 * @see PdfTextExtractorPort
 */
public class InvoiceParserService implements InvoiceParserPort {

    private static final Logger log = Logger.getLogger(InvoiceParserService.class.getName());
    private final PdfTextExtractorPort extractor;

    /**
     * Construye un nuevo InvoiceParserService.
     *
     * @param extractor el proveedor para extraer texto plano de archivos PDF
     */
    public InvoiceParserService(PdfTextExtractorPort extractor) {
        this.extractor = extractor;
    }

    /**
     * Analiza un flujo de PDF de factura y extrae información financiera y de
     * entidades relevante.
     *
     * @param fileStream       el flujo de entrada del archivo PDF
     * @param originalFilename el nombre original del archivo PDF
     * @return un {@link InvoiceResponse} que contiene los datos extraídos,
     *         incluyendo emisor,
     *         adquirente, totales y líneas de ítems
     * @throws Exception si ocurre un error durante la extracción de texto o la
     *                   lógica de análisis
     */
    @Override
    public InvoiceResponse parse(InputStream fileStream, String originalFilename) throws Exception {
        InvoiceResponse response = new InvoiceResponse();
        try {
            byte[] rawBytes = fileStream.readAllBytes();
            String text = "";
            if (extractor != null) {
                try (InputStream is = new ByteArrayInputStream(rawBytes)) {
                    text = extractor.extractText(is, originalFilename);
                }
            }

            if (text == null || text.isBlank()) {
                return response;
            }

            // Normalizar texto a espacios simples y eliminar espacios de no separación
            String normalized = text.replaceAll("\u00A0", " ").replaceAll("\\s+", " ").trim();

            extractInvoiceDetails(response, normalized);

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error parsing invoice", e);
            throw e;
        }
        return response;
    }

    /**
     * Orquesta la extracción de campos individuales del texto normalizado.
     *
     * @param response   el objeto de respuesta a poblar
     * @param normalized el contenido de texto normalizado
     */
    private void extractInvoiceDetails(InvoiceResponse response, String normalized) {
        response.setInvoiceNumber(extractValue(normalized, "(?i)No\\.\\s*([A-Z0-9-]{2,20})"));

        extractDates(response, normalized);

        response.setCufe(extractValue(normalized, "(?i)CUFE\\s*[:]?\\s*([0-9a-fA-F]{40,})"));

        extractTotals(response, normalized);

        extractPaymentInfo(response, normalized);

        extractCurrency(response, normalized);

        extractEntities(response, normalized);

        extractItems(response, normalized);
    }

    private void extractDates(InvoiceResponse response, String normalized) {
        String dateRegex = "(?i)(?:EXPEDICIÓN|Fecha)\\s*[:]?\\s*(\\d{2}[-/][A-Za-z]{3}[-/]\\d{4}|\\d{4}[-/]\\d{2}[-/]\\d{2})(?:\\s+(\\d{1,2}:\\d{2}(?:[AP]M)?))?";
        Matcher dateMatcher = Pattern.compile(dateRegex).matcher(normalized);
        if (dateMatcher.find()) {
            response.setIssueDate(dateMatcher.group(1));
            if (dateMatcher.groupCount() >= 2) {
                response.setIssueTime(dateMatcher.group(2));
            }
        }

        response.setExpirationDate(extractValue(normalized,
                "(?i)(?:Vencimiento|Vence)\\s*[:]?\\s*(\\d{2}[-/][A-Za-z]{3}[-/]\\d{4}|\\d{4}[-/]\\d{2}[-/]\\d{2})"));
    }

    /**
     * Extrae los valores totales de la factura, incluyendo subtotal, carga
     * impositiva y monto total.
     * <p>
     * Utiliza expresiones regulares específicas para identificar etiquetas comunes.
     * Para el 'Total a Pagar', emplea un "lookbehind negativo" {@code (?<!SUB)}
     * para asegurar
     * que la palabra "TOTAL" no sea parte de "SUBTOTAL", evitando falsos positivos.
     * </p>
     *
     * @param response   el objeto de respuesta donde se guardarán los valores
     * @param normalized el texto completo de la factura normalizado
     */
    private void extractTotals(InvoiceResponse response, String normalized) {
        response.setSubtotal(extractMoney(normalized, "(?i)(?:Subtotal|Valor Bruto)\\s*[:]?\\s*"));

        response.setTaxableAmount(
                extractMoney(normalized, "(?i)(?:Base Gravable|Base Imponible|BASE IMPUESTOS)\\s*[:]?\\s*"));

        Double tax = extractMoney(normalized, "(?i)TASA IVA(?:\\s*\\(\\d+%\\))?\\s*[:]?\\s*");
        if (tax == null) {
            tax = extractMoney(normalized, "(?i)(?:Total Impuestos|IVA|I\\.V\\.A\\.|Total IVA)\\s*[:]?\\s*");
        }
        response.setTotalTax(tax);

        Double total = extractMoney(normalized, "(?i)(?<!SUB)\\bTOTAL\\s*[:]\\s*");

        if (total == null || total < 100) {
            total = extractMoney(normalized,
                    "(?i)(?:Total a Pagar|Valor Total|Total Factura|Total General)\\s*[:]?\\s*");
        }

        if (total == null || total < 100) {
            total = extractMoney(normalized, "(?i)(?<!SUB)\\bTOTAL\\s+");
        }
        response.setTotalAmount(total);
    }

    private void extractPaymentInfo(InvoiceResponse response, String normalized) {
        String lower = normalized.toLowerCase();
        if (lower.contains("contado")) {
            response.setPaymentForm("Contado");
        } else if (lower.contains("crédito") || lower.contains("credito")) {
            response.setPaymentForm("Crédito");
        }

        if (lower.contains("efectivo")) {
            response.setPaymentMethod("Efectivo");
        } else if (lower.contains("transferencia")) {
            response.setPaymentMethod("Transferencia");
        }
    }

    private void extractCurrency(InvoiceResponse response, String normalized) {
        String currencyVal = extractValue(normalized, "(?i)(?:Moneda|Divisa)\\s*[:]?\\s*([A-Za-z ]{3,20})");
        if (currencyVal != null) {
            if (currencyVal.toLowerCase().contains("peso") || currencyVal.contains("COP")) {
                response.setCurrency("COP");
            } else {
                response.setCurrency(currencyVal.trim());
            }
        } else if (normalized.contains("$")) {
            response.setCurrency("COP");
        }
    }

    private void extractEntities(InvoiceResponse response, String normalized) {
        // Emisor
        CompanyInfo issuer = new CompanyInfo();
        String issuerBlock = extractValue(normalized, "(?i)EMISOR\\s+(.*?)(?:ADQUIRIENTE|CLIENTE|DETALLE|ÍTEMS)");
        if (issuerBlock != null) {
            issuer.setNit(extractValue(issuerBlock, "(?i)(?:NIT|Identificación)\\.?\\s*([0-9-]{9,15})"));
            issuer.setName(extractValue(issuerBlock, "^(.*?)(?:IDENTIFICACIÓN|NIT)"));
            issuer.setEmail(extractValue(issuerBlock, "(?i)EMAIL\\s*([\\w.-]+@[\\w.-]+)"));
            issuer.setPhone(extractValue(issuerBlock, "(?i)TELÉFONO\\s*([0-9]+)"));
            issuer.setAddress(extractValue(issuerBlock, "(?i)DIRECCIÓN\\s*(.*?)(?:$|EMAIL|TELÉFONO|#|Código)"));
        } else {
            issuer.setNit(extractValue(normalized, "(?i)(?:NIT|Identificación)\\.?\\s*([0-9-]{9,15})"));
        }
        response.setIssuer(issuer);

        // Adquirente
        CompanyInfo acquirer = new CompanyInfo();
        String acquirerBlock = extractValue(normalized,
                "(?i)(?:ADQUIRIENTE|CLIENTE|SEÑOR\\(ES\\))\\s+(.*?)(?:DETALLE|ÍTEMS|TOTAL|SUBTOTAL|CUFE|Forma de Pago|Medio de Pago)");
        if (acquirerBlock != null) {
            acquirer.setNit(extractValue(acquirerBlock, "(?i)(?:NIT|Identificación)\\.?\\s*[:]?\\s*([0-9-]{9,15})"));
            acquirer.setName(extractValue(acquirerBlock, "^(.*?)(?:IDENTIFICACIÓN|NIT)"));
            acquirer.setEmail(extractValue(acquirerBlock, "(?i)EMAIL\\s*([\\w.-]+@[\\w.-]+)"));
            acquirer.setPhone(extractValue(acquirerBlock, "(?i)TELÉFONO\\s*([0-9]+)"));
            acquirer.setAddress(extractValue(acquirerBlock,
                    "(?i)DIRECCIÓN\\s*(.*?)(?:$|EMAIL|TELÉFONO|\\s#\\s|Código|Descripción|Ítem|Cantidad)"));
        }
        response.setAcquirer(acquirer);
    }

    private void extractItems(InvoiceResponse response, String normalized) {
        List<InvoiceItem> items = new ArrayList<>();
        String itemsBlock = extractValue(normalized,
                "(?i)(?:Recargos Total|Precio unitario.*?Total)\\s+(.*?)\\s+(?:SUBTOTAL|FORMA Y MÉTODO)");

        if (itemsBlock != null) {
            String itemRegex = "(\\d+)\\s+(.*?)\\s+(\\d+)\\s+(\\d+)\\s+\\$\\s*([0-9.,]+)\\s+[A-Za-z]+\\s+\\d+%\\s+\\$\\s*([0-9.,]+)";

            Matcher itemMatcher = Pattern.compile(itemRegex).matcher(itemsBlock);

            while (itemMatcher.find()) {
                InvoiceItem item = new InvoiceItem();

                item.setCode(itemMatcher.group(1));
                item.setDescription(itemMatcher.group(2).trim());
                item.setUnitOfMeasure(itemMatcher.group(3));

                try {
                    item.setQuantity(Double.parseDouble(itemMatcher.group(4)));
                    String priceStr = normalizeCurrency(itemMatcher.group(5));
                    item.setUnitPrice(Double.parseDouble(priceStr));

                    String taxStr = normalizeCurrency(itemMatcher.group(6));
                    item.setTaxAmount(Double.parseDouble(taxStr));

                    double lineTotal = (item.getQuantity() * item.getUnitPrice()) + item.getTaxAmount();
                    item.setTotal(lineTotal);

                    items.add(item);
                } catch (NumberFormatException e) {
                    log.log(Level.WARNING,
                            "Data Integrity Error: Skipping malformed item row matching " + item.getCode(), e);
                }
            }
        }
        response.setItems(items);
    }

    /**
     * Helper para normalizar formatos de moneda latinos (1.000,00 -> 1000.00)
     */
    private String normalizeCurrency(String raw) {
        return raw.replace(".", "").replace(",", ".");
    }

    /**
     * Método auxiliar para extraer un valor de cadena basado en un grupo de patrón
     * regex.
     *
     * @param text  el texto a buscar
     * @param regex el patrón regex con al menos un grupo de captura
     * @return la cadena extraída, o null si no se encuentra
     */
    private String extractValue(String text, String regex) {
        try {
            Matcher m = Pattern.compile(regex).matcher(text);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Método auxiliar para extraer un valor monetario, manejando varios formatos.
     *
     * @param text        el texto a buscar
     * @param prefixRegex el regex para la etiqueta que precede al monto (ej.
     *                    "Total:")
     * @return el valor doble del monto, o null si no se encuentra
     */
    private Double extractMoney(String text, String prefixRegex) {
        try {
            Matcher m = Pattern.compile(prefixRegex + "[$]?\\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?)")
                    .matcher(text);
            if (m.find()) {
                String val = m.group(1);
                if (val.contains(",") && val.lastIndexOf(',') > val.lastIndexOf('.')) {
                    val = val.replace(".", "").replace(",", ".");
                } else {
                    val = val.replace(",", "");
                }
                return Double.parseDouble(val);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
