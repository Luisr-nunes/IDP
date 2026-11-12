# IDP — Intelligent Document Validation

<!-- badges -->
![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white) ![JUnit 5](https://img.shields.io/badge/JUnit%205-25A162?style=flat-square&logo=junit5&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)

[Ler em Português](README.md)

Java and Spring Boot API to validate documents automatically: it takes a PDF or image, cleans up the image, extracts the text, structures the data with AI and applies business rules to approve, reject or send the document to manual review. Built for the Avanade Mentorship 2026.2.

## Flow

```
Document (PDF/image)
   → OpenCV: image preprocessing                  (OpenCvService, external command)
   → Tesseract: text extraction                   (OcrService, external command)
   → AI: text into structured data                (InterpretadorIaService, OpenAI)
   → Business rules                               (ValidadorComprovante)
   → APROVADO | REPROVADO | REVISAO_MANUAL
```

Every stage has a local fallback, so the API works without an AI key and without Tesseract or OpenCV installed:

| Stage | When configured | When not configured or on failure |
| --- | --- | --- |
| OpenCV | `app.opencv.command` cleans up the image | keeps the original image |
| Tesseract | `app.ocr.tessdata-path` extracts text from the image | uses the text in `conteudoDocumento` |
| AI | `OPENAI_API_KEY` interprets the text | local regex extraction |

The `etapas` field of the response shows which path each stage took.

## Endpoints

| Method | Route | Description |
| --- | --- | --- |
| `GET` | `/api/documentos/exemplo` | Sample payload for validation |
| `POST` | `/api/documentos/validar` | Runs the pipeline and returns stages, extracted data and result |
| `GET` | `/api/documentos/{id}` | Returns the stored result of a validation |

`POST /api/documentos/validar` request (send `conteudoDocumento`, `imagemBase64` or both):

```json
{
  "nomeArquivo": "comprovante-residencia.pdf",
  "mimeType": "application/pdf",
  "conteudoDocumento": "COMPROVANTE DE RESIDENCIA\nNome: Joao da Silva\nCPF: 123.456.789-00\nEndereco: Rua das Flores, 150\nCidade: Guanambi - BA\nData de emissao: 10/08/2026",
  "imagemBase64": null
}
```

Response (shortened):

```json
{
  "nomeArquivo": "comprovante-residencia.pdf",
  "etapas": [
    { "ordem": 1, "nome": "Documento", "descricao": "Entrada do arquivo para o pipeline", "resultado": "comprovante-residencia.pdf" }
  ],
  "dadosExtraidos": {
    "nome": "Joao da Silva",
    "cpf": "123.456.789-00",
    "endereco": "Rua das Flores, 150",
    "cidade": "Guanambi - BA",
    "dataEmissao": "2026-08-10"
  },
  "resultado": {
    "aprovado": true,
    "status": "APROVADO",
    "regrasAtendidas": ["Nome identificado", "CPF encontrado", "Endereco encontrado", "Cidade encontrada", "Data de emissao reconhecida", "Emissao dentro de 90 dias"],
    "pendencias": []
  }
}
```

The `Location` response header points to `GET /api/documentos/{id}`. `mimeType` accepts `application/pdf`, `image/png` and `image/jpeg`; `imagemBase64` is limited to 5 MB.

## Rules

A proof of address is approved only if all of these hold (rule texts are returned in Portuguese):

- Nome identificado (name identified)
- CPF encontrado (CPF, the Brazilian taxpayer ID, found)
- Endereco encontrado (address found)
- Cidade encontrada (city found)
- Data de emissao reconhecida (issue date recognized)
- Emissao dentro de 90 dias (issued within 90 days and not in the future; set in `app.validacao.prazo-emissao-dias`)

Status:

- `APROVADO`: every rule met.
- `REPROVADO`: the document was fully read but a rule failed (for example, an old issue date), or nothing was extracted.
- `REVISAO_MANUAL`: uncertain extraction, only some fields were found (poor OCR or an AI value that was discarded).

## Running

Requirement: JDK 21.

```bash
git clone https://github.com/Luisr-nunes/IDP.git
cd IDP
./mvnw test                # Linux/macOS (mvnw.cmd on Windows)
./mvnw spring-boot:run     # http://localhost:8080, dev profile with in-memory H2
```

### Optional integrations

OpenAI (the key only comes from an environment variable):

```powershell
$env:OPENAI_API_KEY="your-key"
```

Tesseract, in `application.properties` or through environment variables:

```properties
app.ocr.command=tesseract
app.ocr.tessdata-path=C:/Program Files/Tesseract-OCR/tessdata
app.ocr.language=por
```

OpenCV, with the bundled script (takes `inputFile outputFile`; requires `pip install opencv-python`):

```properties
app.opencv.command=python scripts/preprocessar_documento.py
```

### prod profile with PostgreSQL

```powershell
$env:DB_PASSWORD="a-local-password"
docker compose up -d
$env:SPRING_PROFILES_ACTIVE="prod"
./mvnw spring-boot:run
```

prod profile variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` and `DB_PASSWORD` (required).

## Demo flow

1. Start the API with `./mvnw spring-boot:run`.
2. Call `GET /api/documentos/exemplo` and send the payload to `POST /api/documentos/validar`.
3. With no AI key and no `tessdata`, show the local fallback in `etapas`.
4. Change the issue date to an old one to get `REPROVADO`, or remove lines from the text to get `REVISAO_MANUAL`.
5. Fetch the stored result through the `Location` header (`GET /api/documentos/{id}`).
6. Set `OPENAI_API_KEY` and `app.ocr.tessdata-path` to show the setup closest to the real world; for OpenCV, point `app.opencv.command` to the script.

## Technical decisions

- **External processes instead of Java libraries**: Tesseract and OpenCV run as configurable commands, with arguments passed as a list (no concatenated user input), a 30-second timeout and temporary files always deleted.
- **Fallback at every stage**: an error or timeout in OpenCV, OCR or AI does not fail the request; the stage takes the local path and reports the failure.
- **Privacy with the AI**: the CPF is masked before the text is sent to OpenAI and is read locally with a regex.
- **Anti-hallucination**: name, address, city and date returned by the AI are only accepted if they appear in the document text (accent- and case-insensitive comparison).
- **Manual review**: partial extraction goes to `REVISAO_MANUAL` instead of being rejected outright.
- **Data minimization**: the database stores only the file name, status, pending rules and timestamp; never the text, the image, the name, the CPF or the address.
- **Logs**: errors only record the exception type, and a Logback converter masks any CPF that reaches a log message.
- **Secrets**: the AI key and the database password come only from environment variables; nothing is stored in the repository.

## Tech stack

- Java 21, Spring Boot 3.5 (Web, Validation, Data JPA)
- PostgreSQL (prod) and H2 (dev and tests)
- Tesseract OCR, OpenCV (Python) and the OpenAI API, as optional integrations
- JUnit 5 and MockMvc; CI with GitHub Actions
- Maven (Maven Wrapper included)

## Structure

```
IDP/
├── src/main/java/br/com/mentoria/validacao/
│   ├── api/           # DocumentoController, ValidacaoRequest, ValidacaoResponse, TratadorDeErros
│   ├── dominio/       # DadosComprovante, EtapaPipeline, ResultadoValidacao, StatusValidacao, Cpf, Texto
│   ├── servico/       # ValidacaoDocumentoService, OpenCvService, OcrService, InterpretadorIaService,
│   │                  # ExtratorTexto, ValidadorComprovante
│   ├── persistencia/  # ValidacaoRegistro, ValidacaoRegistroRepository
│   └── log/           # MascaraDadosPessoaisConverter
├── src/main/resources/  # application.properties, application-dev/prod.properties, logback-spring.xml
├── scripts/preprocessar_documento.py
├── docker-compose.yml
└── pom.xml
```

## Author

Luís Nunes — [GitHub](https://github.com/Luisr-nunes) · [LinkedIn](https://www.linkedin.com/in/luis-nunes-dev/)
