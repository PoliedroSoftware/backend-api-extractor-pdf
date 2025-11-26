package co.com.extractor.domain.gateways;

import java.io.InputStream;
import java.util.Map;

public interface AreaExtractorPort {
    /**
     * Extrae áreas a partir del contenido del archivo.
     * Uso del dominio: no depende de Spring MultipartFile; los adaptadores se encargan de convertir.
     * @param fileStream stream del archivo (por ejemplo PDF)
     * @param originalFilename nombre original del archivo (opcional)
     * @return mapa con las áreas extraídas
     * @throws Exception en caso de error de extracción
     */
    Map<String, String> extractAreas(InputStream fileStream, String originalFilename) throws Exception;
}
