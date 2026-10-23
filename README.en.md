# IDP — Intelligent Document Validation

<!-- badges -->
![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white) ![JUnit 5](https://img.shields.io/badge/JUnit%205-25A162?style=flat-square&logo=junit5&logoColor=white)

[Ler em Português](README.md)

Java and Spring Boot API to validate documents automatically: it takes a PDF or image, cleans up the image, extracts the text, structures the data with AI and applies business rules to approve, reject or send the document to manual review. Built for the Avanade Mentorship 2026.2.

## Status

In development. The repository currently contains the domain model and the rule engine for proof-of-address documents, with unit tests. The REST API and the OCR, preprocessing and AI integrations are not implemented yet.

| Stage | Status |
| --- | --- |
| Domain (`DadosComprovante`, `ResultadoValidacao`, `StatusValidacao`) | Implemented |
| Validation rules (`ValidadorComprovante`) | Implemented and tested |
| REST validation endpoints | Planned |
| Image preprocessing with OpenCV | Planned |
| Text extraction with Tesseract (OCR) | Planned |
| Text interpretation with AI (OpenAI) | Planned |

## Planned flow

```
Document (PDF/image)
   → OpenCV: image preprocessing
   → Tesseract: text extraction
   → AI: text into structured data (name, CPF, address, city, issue date)
   → Business rules
   → APROVADO | REPROVADO | REVISAO_MANUAL
```

## Implemented rules

A proof of address is approved only if all of these hold:

- name identified;
- CPF (Brazilian taxpayer ID) found;
- address found;
- city found;
- issue date recognized;
- issued within the allowed window and not in the future (90 days by default, set in `app.validacao.prazo-emissao-dias`).

The result lists the rules met and the pending ones, for example `Falhou: CPF encontrado`.

## Tech stack

- Java 21
- Spring Boot 3.5 (Web, Validation)
- JUnit 5 (`spring-boot-starter-test`)
- Maven (Maven Wrapper included)

Planned: OpenCV, Tesseract OCR and the OpenAI API.

## Structure

```
IDP/
├── src/main/java/br/com/mentoria/validacao/
│   ├── ValidacaoDocumentosApplication.java
│   ├── dominio/      # DadosComprovante, ResultadoValidacao, StatusValidacao
│   └── servico/      # ValidadorComprovante
├── src/main/resources/application.properties
├── src/test/java/.../servico/ValidadorComprovanteTest.java
└── pom.xml
```

## Running

Requirement: JDK 21.

```bash
git clone https://github.com/Luisr-nunes/IDP.git
cd IDP
./mvnw test              # Linux/macOS
mvnw.cmd test            # Windows
```

`./mvnw spring-boot:run` starts the app on `http://localhost:8080`, with no endpoints exposed yet.

## Author

Luís Nunes — [GitHub](https://github.com/Luisr-nunes) · [LinkedIn](https://www.linkedin.com/in/luis-nunes-dev/)
