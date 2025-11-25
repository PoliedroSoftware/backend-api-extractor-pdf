package co.com.extractor.api.dto;

/** Compatibility wrapper to keep API DTOs in the entry-points module. */
public class RutResponse extends co.com.extractor.domain.model.RutResponse {
    public RutResponse() { super(); }

    public RutResponse(co.com.extractor.domain.model.RutResponse src) {
        if (src == null) return;
        try {
            this.setSource(src.getSource());
            this.setFormNumber(src.getFormNumber());
            this.setNit(src.getNit());
            this.setDv(src.getDv());
            this.setContributorType(src.getContributorType());
            this.setDocumentType(src.getDocumentType());
            this.setDocumentNumber(src.getDocumentNumber());
            this.setFullName(src.getFullName());
            this.setEmail(src.getEmail());
            this.setAddress(src.getAddress());
            this.setCountry(src.getCountry());
            this.setDepartment(src.getDepartment());
            this.setCity(src.getCity());
            this.setPostalCode(src.getPostalCode());
            this.setEconomicActivities(src.getEconomicActivities());
            this.setResponsibilities(src.getResponsibilities());
            this.setIssueDate(src.getIssueDate());
            this.setPdfGeneratedAt(src.getPdfGeneratedAt());
            this.setRaw(src.getRaw());
        } catch (Exception ignore) {}
    }

    @Override
    public java.util.Map<String,Object> getRaw() { return super.getRaw(); }
}
