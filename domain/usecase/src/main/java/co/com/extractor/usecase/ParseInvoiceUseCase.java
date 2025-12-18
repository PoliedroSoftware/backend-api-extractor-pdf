package co.com.extractor.usecase;

import co.com.extractor.domain.gateways.InvoiceParserPort;
import co.com.extractor.domain.model.InvoiceResponse;
import java.io.InputStream;

/**
 * Caso de uso para orquestar el parseo de facturas.
 * Actúa como intermediario entre la capa de entrada y el puerto de dominio.
 */
public class ParseInvoiceUseCase {

    private final InvoiceParserPort parserPort;

    public ParseInvoiceUseCase(InvoiceParserPort parserPort) {
        this.parserPort = parserPort;
    }

    /**
     * Ejecuta la lógica de parseo de factura.
     * 
     * @param fileStream stream del archivo a procesar
     * @param filename   nombre del archivo
     * @return respuesta estructurada de la factura
     * @throws Exception si ocurre un error durante el proceso
     */
    public InvoiceResponse execute(InputStream fileStream, String filename) throws Exception {
        return parserPort.parse(fileStream, filename);
    }
}
