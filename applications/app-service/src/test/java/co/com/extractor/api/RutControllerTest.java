package co.com.extractor.api;

import co.com.extractor.api.dto.FullName;
import co.com.extractor.api.dto.RutResponse;
import co.com.extractor.service.RutParserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RutController.class)
class RutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RutParserService parserService;

    @Test
    void parse_endpoint_returns_200_when_service_returns_data() throws Exception {
        RutResponse r = new RutResponse();
        r.setNit("10916585513");
        r.setDv("3");
        FullName fn = new FullName();
        fn.setDisplay("SANCHEZ PACHECO EDUAR LEONARDO");
        r.setFullName(fn);

        Mockito.when(parserService.parse(Mockito.any())).thenReturn(r);

        MockMultipartFile file = new MockMultipartFile("file", "rut.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy".getBytes());

        mockMvc.perform(multipart("/api/v1/rut/parse").file(file)).andExpect(status().isOk());
    }
}

