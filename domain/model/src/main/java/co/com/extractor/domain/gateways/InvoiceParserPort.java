package co.com.extractor.domain.gateways;

import co.com.extractor.domain.model.InvoiceResponse;
import java.io.InputStream;

/**
 * Puerto para el parseo de facturas.
 * Define el contrato para extraer información estructurada de una factura a
 * partir de un stream.
 */
public interface InvoiceParserPort {
    /**
     * Parsea una factura a partir del contenido del archivo.
     * Uso del dominio: no depende de Spring MultipartFile; los adaptadores se
     * encargan de convertir.
     * 
     * @param fileStream       stream del archivo (por ejemplo PDF)
     * @param originalFilename nombre original del archivo (opcional)
     * @return objeto InvoiceResponse con la información estructurada de la factura
     * @throws Exception en caso de error de parseo o lectura
     */
    InvoiceResponse parse(InputStream fileStream, String originalFilename) throws Exception;
}
