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
    public co.com.extractor.usecase.ParseRutUseCase parseRutUseCase(co.com.extractor.domain.gateways.RutParserPort rutParserPort) {
        return new co.com.extractor.usecase.ParseRutUseCase(rutParserPort);
    }
}
