package co.com.extractor.service;

import co.com.extractor.api.dto.RutResponse;
import co.com.extractor.extractor.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class RutParserServiceDebugTest {

    @Test
    void parse_should_extract_nit_dv_formnumber_and_name_from_actual_pdf_text() throws Exception {
        PdfTextExtractor extractor = Mockito.mock(PdfTextExtractor.class);
        String sampleText = "27. Fecha expedición    Exportadores Para uso exclusivo de la DIAN 5. Número de Identificación Tributaria (NIT) 6. DV      984. Nombre 51. Código 38. País    56. Tipo 985. Cargo 50. Código    4. Número de formulario 36. Nombre comercial 37. Sigla 53. Código 59. Anexos          SI NO 61. Fecha 55. Forma 57. Mod 58. CPC 60. No. de Folios: Ocupación 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 Actividad secundaria Otras actividades 49. Fecha inicio actividad48. Código 1 2 Lugar de expedición 28.  País Actividad principal Actividad económica 47. Fecha inicio actividad46. Código 1 2 3 35. Razón social 31. Primer apellido  32. Segundo apellido 33. Primer nombre 52. Número  establecimientos 24. Tipo de contribuyente 12. Dirección seccional 14. Buzón electrónico 34. Otros nombres 25. Tipo de documento 29. Departamento 26. Número de Identificación 39. Departamento Firma del solicitante: 2. Concepto 19 20 21 22 23 24 25 26 1 2 3 4 5 6 7 8 9 10 54. Código 11 12 13 14 15 16 17 18 19 20 Responsabilidades, Calidades y Atributos IMPORTANTE: Sin perjuicio de las actualizaciones a que haya lugar, la inscripción en el Registro Único Tributario -RUT-, tendrá vigencia indefinida y en consecuencia no se exigirá su renovación      40. Ciudad/Municipio Parágrafo del artículo 1.6.1.2.20 del Decreto 1625 de 2016 Sin perjuicio de las verificaciones que la DIAN realice.   Firma autorizada: IDENTIFICACIÓN 41. Dirección principal ExportadoresObligados aduaneros UBICACIÓN 30. Ciudad/Municipio 42. Correo electrónico 44. Teléfono 143. Código postal 45. Teléfono 2 CLASIFICACIÓN Actualización0 2 14824701795     1 0 9 1 6 5 8 5 5 1 3 Impuestos de Cúcuta  7 Persona natural o sucesión ilíquida 2 Cédula de Ciudadanía 1 3 1 0 9 1 6 5 8 5 5 1           2 0 0 6 0 5 0 5 COLOMBIA 1 6 9 Norte de Santander 5 4    Ocaña 4 9 8 SANCHEZ PACHECO EDUAR LEONARDO COLOMBIA 1 6 9 Norte de Santander 5 4 Ocaña 4 9 8 CR 16   8   109 BRR SAN CAYETANO leosanchez_19@hotmail.com 5 4 4 9 8            3 1 5 7 6 9 0 5 7 9                  6 2 0 1 2 0 2 0 0 3 0 3                               05- Impto. renta y compl.  régimen  ordinar 5   49 - No responsable de IVA 4 9                                                                                                                               X   0 2022 - 04 - 25 / 15 : 31: 12 SANCHEZ PACHECO EDUAR LEONARDO  CONTRIBUYENTE Fecha generación documento PDF: 25-04-2022 03:29:53PM";
        Mockito.when(extractor.extractText(Mockito.any(MultipartFile.class))).thenReturn(sampleText);

        RutParserService service = new RutParserService(extractor);
        RutResponse resp = service.parse(Mockito.mock(MultipartFile.class));

        assertNotNull(resp);
        assertEquals("10916585513", resp.getNit());
        assertEquals("3", resp.getDv());
        assertEquals("14824701795", resp.getFormNumber());
        assertNotNull(resp.getFullName());
        assertTrue(resp.getFullName().getDisplay().toUpperCase().contains("SANCHEZ PACHECO"));
        assertTrue(resp.getFullName().getDisplay().toUpperCase().contains("EDUAR"));
    }
}

