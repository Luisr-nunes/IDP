# IDP — Validação Inteligente de Documentos

<!-- badges -->
![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white) ![JUnit 5](https://img.shields.io/badge/JUnit%205-25A162?style=flat-square&logo=junit5&logoColor=white)

[Read in English](README.en.md)

API em Java e Spring Boot para validar documentos automaticamente: recebe um PDF ou imagem, trata a imagem, extrai o texto, estrutura os dados com IA e aplica regras de negócio para aprovar, reprovar ou encaminhar para revisão manual. Projeto da Mentoria Avanade 2026.2.

## Status

Em desenvolvimento. Hoje o repositório tem o domínio e o motor de regras para comprovante de residência, com testes unitários. A API REST e as integrações de OCR, pré-processamento e IA ainda não foram implementadas.

| Etapa | Situação |
| --- | --- |
| Domínio (`DadosComprovante`, `ResultadoValidacao`, `StatusValidacao`) | Implementado |
| Regras de validação (`ValidadorComprovante`) | Implementado e testado |
| Endpoints REST de validação | Planejado |
| Pré-processamento de imagem com OpenCV | Planejado |
| Extração de texto com Tesseract (OCR) | Planejado |
| Interpretação do texto com IA (OpenAI) | Planejado |

## Fluxo planejado

```
Documento (PDF/imagem)
   → OpenCV: pré-processamento da imagem
   → Tesseract: extração do texto
   → IA: texto em dados estruturados (nome, CPF, endereço, cidade, data de emissão)
   → Regras de negócio
   → APROVADO | REPROVADO | REVISAO_MANUAL
```

## Regras implementadas

Para um comprovante de residência ser aprovado, todas precisam ser atendidas:

- nome identificado;
- CPF encontrado;
- endereço encontrado;
- cidade encontrada;
- data de emissão reconhecida;
- emissão dentro do prazo, sem data futura (padrão de 90 dias, configurável em `app.validacao.prazo-emissao-dias`).

O resultado lista as regras atendidas e as pendências, por exemplo `Falhou: CPF encontrado`.

## Tecnologias

- Java 21
- Spring Boot 3.5 (Web, Validation)
- JUnit 5 (`spring-boot-starter-test`)
- Maven (Maven Wrapper incluído)

Planejadas: OpenCV, Tesseract OCR e API da OpenAI.

## Estrutura

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

## Como executar

Pré-requisito: JDK 21.

```bash
git clone https://github.com/Luisr-nunes/IDP.git
cd IDP
./mvnw test              # Linux/macOS
mvnw.cmd test            # Windows
```

`./mvnw spring-boot:run` sobe a aplicação em `http://localhost:8080`, ainda sem endpoints expostos.

## Autor

Luís Nunes — [GitHub](https://github.com/Luisr-nunes) · [LinkedIn](https://www.linkedin.com/in/luis-nunes-dev/)
