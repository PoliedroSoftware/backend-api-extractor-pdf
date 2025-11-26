package co.com.extractor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("RUT Extractor API")
                        .description("API para extraer datos desde PDF del RUT (DIAN) y devolver JSON normalizado")
                        .version("0.1.0")
                        .contact(new Contact().name("Equipo").email("dev@example.com"))
                        .license(new License().name("MIT")));
    }
}

