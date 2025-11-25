package co.com.extractor.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/** DTO usado sólo para documentar el request multipart en OpenAPI */
public class FileUpload {

    @Schema(description = "Archivo PDF (campo 'file')", type = "string", format = "binary")
    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}

