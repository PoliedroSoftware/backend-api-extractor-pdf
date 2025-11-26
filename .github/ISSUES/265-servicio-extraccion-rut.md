Título: Tarea: Servicio de extracción de datos desde RUT (PDF) → JSON (#265)

Responsable: @Lfusmac
Prioridad: medium
Estimado: 2d
Etiquetas: type: feature, area: backend, priority: medium

Resumen
-------
Crear un microservicio Java Spring Boot cuya función sea recibir un PDF del RUT (DIAN) vía POST, extraer automáticamente los datos clave y devolver un JSON normalizado. El procesamiento debe ser stateless (no guardar archivo ni datos).

Objetivos
---------
- Implementar endpoint POST /api/v1/rut/parse que acepte un `multipart/form-data` con campo `file` (PDF).
- Extraer campos mínimos y recomendados del RUT y devolverlos en JSON según contrato.
- No persistir el PDF ni los datos. Limitar tamaño de archivo y validar content-type.

Requisitos funcionales (contrato API)
------------------------------------
Endpoint
- POST /api/v1/rut/parse
- Request: Content-Type: multipart/form-data
  - Campo: file (PDF del RUT)
- Respuestas:
  - 200 OK: devuelve JSON con los campos extraídos.
  - 400 Bad Request: archivo faltante, vacío o extensión distinta a PDF.
  - 415 Unsupported Media Type: content-type inválido.
  - 422 Unprocessable Entity: no se pudo extraer información mínima (NIT o identificación).
  - 500 Internal Server Error: error inesperado en parsing.

Ejemplo de respuesta (200)
{
  "source": "DIAN-RUT",
  "formNumber": "14824701795",
  "nit": "10916585513",
  "dv": "3",
  "contributorType": "Persona natural o sucesión ilíquida",
  "documentType": "Cédula de Ciudadanía",
  "documentNumber": "1091658551",
  "fullName": {
    "firstName": "EDUAR",
    "middleNames": "LEONARDO",
    "lastName": "SANCHEZ",
    "secondLastName": "PACHECO",
    "display": "SANCHEZ PACHECO EDUAR LEONARDO"
  },
  "email": "leosanchez_19@hotmail.com",
  "address": "CR 16 8 109 BRR SAN CAYETANO",
  "country": "COLOMBIA",
  "department": "Norte de Santander",
  "city": "Ocaña",
  "postalCode": "5498",
  "economicActivities": [ { "code": "6201", "description": "Desarrollo de sistemas informáticos (actividades de programación)", "startDate": "2020-03-03" } ],
  "responsibilities": [ { "code": "05", "description": "Impto. renta y compl. régimen ordinario" }, { "code": "49", "description": "No responsable de IVA" } ],
  "issueDate": "2022-04-25",
  "pdfGeneratedAt": "2022-04-25T15:29:53",
  "raw": { "dianSectional": "Impuestos de Cúcuta" }
}

Errores (ejemplo 422)
{
  "type": "UnprocessableEntity",
  "detail": "No se pudo extraer NIT/DV desde el PDF proporcionado."
}

Campos mínimos y recomendados a extraer
--------------------------------------
Mínimos:
- formNumber, nit, dv, fullName.display o razón social, documentType, documentNumber
Recomendados:
- email, address, country, department, city, postalCode
- economicActivities[]: code, startDate
- responsibilities[]: code
- issueDate, pdfGeneratedAt, raw.dianSectional

Reglas básicas de parsing
-------------------------
- Usar Apache PDFBox o Tika para extraer texto plano; si falla o detecta imágenes, usar Tesseract (OCR) opcionalmente.
- Normalizar espacios, saltos de línea y caracteres invisibles antes de aplicar regex.
- NIT/DV: buscar secuencias numéricas y dígito verificador.
- Nombres: construir display con formato "LAST SECONDLAST FIRST MIDDLE" según el PDF.
- Actividades: extraer código (ej. 6201) y buscar fecha de inicio si está disponible.

Diseño sugerido (capas)
-----------------------
- Controller: `RutController` (ruta: `/api/v1/rut/parse`)
- Service: `RutParserService` orchestration
- Extractors: `PdfTextExtractor` (PDFBox/Tika), `OcrExtractor` (opcional)
- Parsers/Normalizers: `NameParser`, `AddressParser`, `ActivityParser`, `ResponsibilityParser`
- DTOs: `RutResponse`, `RutExtractRequest` (si aplica)

Tareas (desglosadas)
--------------------
- [ ] Añadir el endpoint `RutController` con validación de `Content-Type` y tamaño máximo (ej. 5MB).
- [ ] Implementar `RutParserService` que orquesta extracción -> normalización -> mapeo a DTO.
- [ ] Implementar `PdfTextExtractor` usando PDFBox/Tika.
- [ ] Implementar parsers unit-testables: Name, Address, Activity, Responsibility.
- [ ] Implementar pruebas unitarias para parsers (mocks para PDF extractor).
- [ ] Implementar prueba de integración (SpringBootTest) que haga POST al endpoint con un PDF de ejemplo y valide el JSON.
- [ ] Documentar en `README.md` cómo ejecutar localmente y cómo probar con `curl`.

Criterios de aceptación
-----------------------
- El endpoint POST `/api/v1/rut/parse` responde 200 con JSON que incluye al menos `nit`, `dv`, y `fullName.display` o razón social.
- Cuando los campos no están presentes, se retornan `null` o se omiten (configurable mediante la implementación).
- El servicio no persiste el PDF ni los datos (stateless). Verificable mediante revisión de código y logs.
- Manejo de errores conforme a contrato (400, 415, 422, 500) con mensajes claros.
- Tests unitarios de parsers y prueba de integración que valida el flujo mínimo.
- P95 < 2s para PDF <= 2MB en pruebas locales (objetivo de rendimiento).

Consideraciones de seguridad y privacidad
----------------------------------------
- No almacenar el archivo ni los datos extraídos.
- Log mínimo: registrar hash del archivo (por ejemplo SHA-256) y duración del procesamiento; evitar logs con datos sensibles.
- Validar tamaño máximo y content-type.

Ejemplo de uso (curl)
---------------------
curl -v -F "file=@/path/to/RUT_EDUAR_SANCHEZ.pdf;type=application/pdf" http://localhost:8080/api/v1/rut/parse

PDF de referencia
-----------------
Adjuntar el PDF `RUT_EDUAR_SANCHEZ.pdf` a la issue o colocarlo en el repositorio bajo `deployment/samples/` y marcarlo como archivo de validación para las pruebas.

Notas/Decisiones técnicas
-------------------------
- Para acelerar entrega, la primera iteración puede devolver datos stub o aplicar reglas regex sobre el texto extraído. La integración con OCR y mejoras de robustez quedarán para iteraciones posteriores.
- `UseCasesConfig` del proyecto requiere que las clases de UseCase terminen con `UseCase` para ser detectadas automáticamente.

Checklist de revisión antes de cerrar la issue
---------------------------------------------
- [ ] Endpoint implementado y PR creada.
- [ ] Tests unitarios/integración pasan.
- [ ] Documentación actualizada.
- [ ] PDF de referencia adjuntado en la issue o incluido en `deployment/samples/`.

---

Si quieres, puedo:
- A) Implementar ahora la PoC (creo controller, service, extractors y tests) y ejecutar `gradlew.bat test` en tu repo.
- B) Subir este markdown a la issue tracker en GitHub si me das URL y permisos.

Indica qué prefieres y procedo.

