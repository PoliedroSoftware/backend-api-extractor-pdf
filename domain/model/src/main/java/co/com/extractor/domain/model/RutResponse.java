package co.com.extractor.domain.model;

import java.util.List;
import java.util.Map;

public class RutResponse {
    private String source;
    private String formNumber;
    private String nit;
    private String dv;
    private String contributorType;
    private String documentType;
    private String documentNumber;
    private FullName fullName;
    private String email;
    private String address;
    private String country;
    private String department;
    private String city;
    private String postalCode;
    private List<Map<String, Object>> economicActivities;
    private List<Map<String, Object>> responsibilities;
    private String issueDate;
    private String pdfGeneratedAt;
    private Map<String, Object> raw;

    public RutResponse() {}

    // getters & setters
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getFormNumber() { return formNumber; }
    public void setFormNumber(String formNumber) { this.formNumber = formNumber; }

    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }

    public String getDv() { return dv; }
    public void setDv(String dv) { this.dv = dv; }

    public String getContributorType() { return contributorType; }
    public void setContributorType(String contributorType) { this.contributorType = contributorType; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public FullName getFullName() { return fullName; }
    public void setFullName(FullName fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public List<Map<String, Object>> getEconomicActivities() { return economicActivities; }
    public void setEconomicActivities(List<Map<String, Object>> economicActivities) { this.economicActivities = economicActivities; }

    public List<Map<String, Object>> getResponsibilities() { return responsibilities; }
    public void setResponsibilities(List<Map<String, Object>> responsibilities) { this.responsibilities = responsibilities; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }

    public String getPdfGeneratedAt() { return pdfGeneratedAt; }
    public void setPdfGeneratedAt(String pdfGeneratedAt) { this.pdfGeneratedAt = pdfGeneratedAt; }

    public Map<String, Object> getRaw() { return raw; }
    public void setRaw(Map<String, Object> raw) { this.raw = raw; }
}
