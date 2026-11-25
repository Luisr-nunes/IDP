# IDP — Intelligent Document Validation

<!-- badges -->
![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat-square&logo=javascript&logoColor=black) ![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=flat-square&logo=html5&logoColor=white) ![CSS](https://img.shields.io/badge/CSS-1572B6?style=flat-square&logo=css3&logoColor=white) ![Python](https://img.shields.io/badge/Python-3776AB?style=flat-square&logo=python&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white) ![JUnit 5](https://img.shields.io/badge/JUnit%205-25A162?style=flat-square&logo=junit5&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)

[Ler em Português](README.md)

Java and Spring Boot system that validates documents automatically for customer onboarding: it takes the proof of address and, optionally, the holder's ID card (RG) or driver's license (CNH), cleans up the image, extracts the text, structures the data with AI and applies business rules to approve, reject or send the case to manual review. It ships with a web page for the analyst and a REST API. Built for the Avanade Mentorship 2026.2.

## Flow

```
Upload (PDF/image)                                web page or POST /api/documentos/validar
   → PDF with text: read directly                 (PdfService, PDFBox)
   → OpenCV: image pre-processing                 (OpenCvService, external command)
   → Tesseract: text extraction                   (OcrService, external command)
   → AI: text into structured data                (InterpretadorIaService, OpenAI)
   → Business rules and cross-check               (ValidadorComprovante)
   → Database: result saved                       (ValidacaoRegistro, H2/PostgreSQL)
   → APROVADO | REPROVADO | REVISAO_MANUAL        query with GET /api/documentos/{id}
```

Every step has a local fallback, so the system works without an AI key, Tesseract or OpenCV:

| Step | Configured | Not configured or on failure |
| --- | --- | --- |
| PDF | text PDFs are read directly; scanned PDFs become an image (300 DPI) | invalid PDF uses `conteudoDocumento` |
| OpenCV | `app.opencv.command` cleans up the image | keeps the original image |
| Tesseract | `app.ocr.tessdata-path` extracts the text | uses `conteudoDocumento` |
| AI | `OPENAI_API_KEY` interprets the text | local regex extraction |

The response shows, in `etapas`, which path each of the 6 steps took.

## Web page

Open `http://localhost:8080`. The analyst uploads the proof of address and the RG/CNH as a file (PDF, PNG or JPG up to 5 MB) or pastes the text, and sees the status, the pipeline steps, the data read from each document, the rules met and failed, and the history of recent validations. Notion-style, with automatic dark mode.

## Endpoints

| Method | Route | Description |
| --- | --- | --- |
| `GET` | `/` | Web page |
| `GET` | `/api/documentos/exemplo` | Sample payload (proof of address + CNH), always approved |
| `POST` | `/api/documentos/validar` | Runs the pipeline and returns steps, extracted data and result |
| `GET` | `/api/documentos/{id}` | Saved result of one validation |
| `GET` | `/api/documentos` | The 20 most recent validations |
| `POST` | `/api/documentos/{id}/revisao` | Analyst approves or rejects a document in manual review (`decisao` and `motivo`) |

`POST /api/documentos/validar` body (for each document send `conteudoDocumento`, `imagemBase64` or both; `identificacao` is optional):

```json
{
  "nomeArquivo": "comprovante-residencia.pdf",
  "mimeType": "application/pdf",
  "conteudoDocumento": "COMPROVANTE DE RESIDENCIA\nNome: Joao da Silva\nCPF: 529.982.247-25\nEndereco: Rua das Flores, 150\nCidade: Guanambi - BA\nData de emissao: 15/11/2026",
  "identificacao": {
    "nomeArquivo": "cnh.pdf",
    "mimeType": "application/pdf",
    "conteudoDocumento": "CARTEIRA NACIONAL DE HABILITACAO\nNome: Joao da Silva\nCPF: 529.982.247-25\nValidade: 25/11/2028"
  }
}
```

The `Location` header points to `GET /api/documentos/{id}`. `mimeType` accepts `application/pdf`, `image/png` and `image/jpeg`; `imagemBase64` must be valid Base64, up to 5 MB.

## Rules

Proof of address: readable (at least 20 characters read); name, CPF, address, city and issue date found; valid CPF (check digits); issued within 90 days and not in the future (`app.validacao.prazo-emissao-dias`).

With RG or CNH (cross-check): readable; name and CPF found; same name on both documents (accent and case insensitive); same CPF on both; CNH not expired.

Status:

- `APROVADO`: every rule met.
- `REPROVADO`: unreadable document, invalid CPF, issue date out of range, name or CPF mismatch, expired CNH, or nothing extracted. The reason is always in `pendencias`.
- `REVISAO_MANUAL`: uncertain extraction, only some fields were found (poor OCR or AI value discarded). The analyst checks the original and approves or rejects it on the page (or via the API) with a required reason, which is kept in the history.

## Running

Requirement: JDK 21.

```bash
git clone https://github.com/Luisr-nunes/IDP.git
cd IDP
./mvnw test                # Linux/macOS (mvnw.cmd on Windows)
./mvnw spring-boot:run     # http://localhost:8080, dev profile with in-memory H2
```

### Optional integrations

```powershell
$env:OPENAI_API_KEY="your-key"
$env:APP_OCR_TESSDATAPATH="C:/Program Files/Tesseract-OCR/tessdata"     # Tesseract with the 'por' language
$env:APP_OPENCV_COMMAND="python scripts/preprocessar_documento.py"       # pip install opencv-python-headless
```

### prod profile with PostgreSQL

```powershell
$env:DB_PASSWORD="a-local-password"
docker compose up -d
$env:SPRING_PROFILES_ACTIVE="prod"
./mvnw spring-boot:run
```

prod variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` and `DB_PASSWORD` (required).

## Demo flow

1. Run `./mvnw spring-boot:run` and open `http://localhost:8080`.
2. Click **Usar exemplo**, then **Validar documentos**: approved, with the 6 steps and the rules.
3. Change the name or CPF on the CNH: rejected for mismatch between documents.
4. Use an old CNH expiry or issue date: rejected, with the reason.
5. Delete lines from the proof of address: manual review. Click **Revisar agora**, write the reason and approve or reject; the decision shows in the history.
6. Upload a real PDF (bank slip or utility bill) in the **Arquivo** tab: text is read straight from the PDF, and name, CPF, address, city, CEP and issue date are found even without the `Label: value` format.
7. Show the history and the API query (`GET /api/documentos/{id}`).
8. With `OPENAI_API_KEY` and Tesseract configured, repeat with a photo to show real OCR and AI.

## Technical decisions

- **External processes instead of Java bindings**: Tesseract and OpenCV run as configurable commands with list arguments (no string concatenation of user input), a 30-second timeout and temp files always deleted.
- **PDF before OCR**: text PDFs are read with PDFBox, faster and exact; only scanned PDFs are rendered to an image.
- **Fallback everywhere**: errors or timeouts in PDF, OpenCV, OCR or AI never break the request; the step uses the local path and reports it.
- **Local cross-check**: RG and CNH have fixed labels, so name, CPF and expiry are read by regex and the ID document is never sent to the AI.
- **Privacy with the AI**: CPFs are masked before the text goes to OpenAI and read locally.
- **Anti-hallucination**: AI values are only accepted if they appear in the document text.
- **Data minimisation**: the database stores only file name, status, pending items and date; never the text, image, name, CPF or address.
- **Front-end safety**: API data reaches the page only through `textContent`, never `innerHTML`; input is validated on the server (type, size and Base64).
- **Logs**: errors log only the exception type, and a Logback converter masks any CPF in a message.
- **Secrets**: AI key and database password only from environment variables.

## Tech stack

- Java 21, Spring Boot 3.5 (Web, Validation, Data JPA), PDFBox
- Plain HTML, CSS and JavaScript for the web page
- PostgreSQL (prod) and H2 (dev and tests)
- Tesseract OCR, OpenCV (Python) and the OpenAI API as optional integrations
- JUnit 5 and MockMvc; CI with GitHub Actions
- Maven (wrapper included)

## Author

Luís Nunes — [GitHub](https://github.com/Luisr-nunes) · [LinkedIn](https://www.linkedin.com/in/luis-nunes-dev/)
