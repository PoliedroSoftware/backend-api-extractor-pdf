package co.com.extractor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import co.com.extractor.mapper.RutResponseMapper;

@Configuration
public class UseCasesConfig {

    private final RutResponseMapper mapper;

    @Autowired
    public UseCasesConfig(RutResponseMapper mapper) {
        this.mapper = mapper;
    }

    @Bean
    public Function<co.com.extractor.domain.model.RutResponse, co.com.extractor.api.dto.RutResponse> domainToApiRutResponseMapper() {
        return mapper::toApi;
    }

    @Bean
    public co.com.extractor.domain.gateways.RutParserPort rutParserPort(
            co.com.extractor.domain.gateways.PdfTextExtractorPort pdfTextExtractor,
            co.com.extractor.domain.gateways.AreaExtractorPort areaExtractor) {
        return new co.com.extractor.domain.usecase.RutParserService(pdfTextExtractor, areaExtractor);
    }

    @Bean
    public co.com.extractor.usecase.ParseRutUseCase parseRutUseCase(
            co.com.extractor.domain.gateways.RutParserPort rutParserPort) {
        return new co.com.extractor.usecase.ParseRutUseCase(rutParserPort);
    }

    @Bean
    public co.com.extractor.domain.gateways.InvoiceParserPort invoiceParserPort(
            co.com.extractor.domain.gateways.PdfTextExtractorPort pdfTextExtractor) {
        return new co.com.extractor.domain.usecase.InvoiceParserService(pdfTextExtractor);
    }

    @Bean
    public co.com.extractor.usecase.ParseInvoiceUseCase parseInvoiceUseCase(
            co.com.extractor.domain.gateways.InvoiceParserPort invoiceParserPort) {
        return new co.com.extractor.usecase.ParseInvoiceUseCase(invoiceParserPort);
    }

    @Bean
    public Function<co.com.extractor.domain.model.InvoiceResponse, co.com.extractor.api.dto.InvoiceResponse> domainToApiInvoiceResponseMapper() {
        return domain -> {
            if (domain == null)
                return null;
            co.com.extractor.api.dto.InvoiceResponse api = new co.com.extractor.api.dto.InvoiceResponse();
            api.setInvoiceNumber(domain.getInvoiceNumber());
            api.setIssueDate(domain.getIssueDate());
            api.setIssueTime(domain.getIssueTime());
            api.setExpirationDate(domain.getExpirationDate());
            api.setPaymentMethod(domain.getPaymentMethod());
            api.setPaymentForm(domain.getPaymentForm());
            api.setSubtotal(domain.getSubtotal());
            api.setTaxableAmount(domain.getTaxableAmount());
            api.setTotalTax(domain.getTotalTax());
            api.setTotalAmount(domain.getTotalAmount());
            api.setCurrency(domain.getCurrency());
            api.setCufe(domain.getCufe());
            api.setNotes(domain.getNotes());
            api.setRawData(domain.getRawData());

            if (domain.getIssuer() != null) {
                co.com.extractor.api.dto.InvoiceResponse.CompanyInfo issuer = new co.com.extractor.api.dto.InvoiceResponse.CompanyInfo();
                issuer.setName(domain.getIssuer().getName());
                issuer.setNit(domain.getIssuer().getNit());
                issuer.setAddress(domain.getIssuer().getAddress());
                issuer.setEmail(domain.getIssuer().getEmail());
                issuer.setPhone(domain.getIssuer().getPhone());
                api.setIssuer(issuer);
            }

            if (domain.getAcquirer() != null) {
                co.com.extractor.api.dto.InvoiceResponse.CompanyInfo acquirer = new co.com.extractor.api.dto.InvoiceResponse.CompanyInfo();
                acquirer.setName(domain.getAcquirer().getName());
                acquirer.setNit(domain.getAcquirer().getNit());
                acquirer.setAddress(domain.getAcquirer().getAddress());
                acquirer.setEmail(domain.getAcquirer().getEmail());
                acquirer.setPhone(domain.getAcquirer().getPhone());
                api.setAcquirer(acquirer);
            }

            if (domain.getItems() != null) {
                java.util.List<co.com.extractor.api.dto.InvoiceResponse.InvoiceItem> items = new java.util.ArrayList<>();
                for (co.com.extractor.domain.model.InvoiceResponse.InvoiceItem i : domain.getItems()) {
                    co.com.extractor.api.dto.InvoiceResponse.InvoiceItem item = new co.com.extractor.api.dto.InvoiceResponse.InvoiceItem();
                    item.setCode(i.getCode());
                    item.setDescription(i.getDescription());
                    item.setQuantity(i.getQuantity());
                    item.setUnitOfMeasure(i.getUnitOfMeasure());
                    item.setUnitPrice(i.getUnitPrice());
                    item.setTaxAmount(i.getTaxAmount());
                    item.setTotal(i.getTotal());
                    items.add(item);
                }
                api.setItems(items);
            }
            return api;
        };
    }
}
