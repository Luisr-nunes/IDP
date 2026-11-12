# IDP — Validação Inteligente de Documentos

<!-- badges -->
![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white) ![JUnit 5](https://img.shields.io/badge/JUnit%205-25A162?style=flat-square&logo=junit5&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)

[Read in English](README.en.md)

API em Java e Spring Boot para validar documentos automaticamente: recebe um PDF ou imagem, trata a imagem, extrai o texto, estrutura os dados com IA e aplica regras de negócio para aprovar, reprovar ou encaminhar para revisão manual. Projeto da Mentoria Avanade 2026.2.

## Fluxo

```
Documento (PDF/imagem)
   → OpenCV: pré-processamento da imagem          (OpenCvService, comando externo)
   → Tesseract: extração do texto                 (OcrService, comando externo)
   → IA: texto em dados estruturados              (InterpretadorIaService, OpenAI)
   → Regras de negócio                            (ValidadorComprovante)
   → APROVADO | REPROVADO | REVISAO_MANUAL
```

Cada etapa tem fallback local, então a API funciona sem chave de IA, sem Tesseract e sem OpenCV instalados:

| Etapa | Com configuração | Sem configuração ou em caso de falha |
| --- | --- | --- |
| OpenCV | `app.opencv.command` trata a imagem | segue com a imagem original |
| Tesseract | `app.ocr.tessdata-path` extrai o texto da imagem | usa o texto de `conteudoDocumento` |
| IA | `OPENAI_API_KEY` interpreta o texto | extração local por regex |

A resposta mostra, em `etapas`, qual caminho foi usado em cada etapa.

## Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/documentos/exemplo` | Payload de exemplo para a validação |
| `POST` | `/api/documentos/validar` | Executa o pipeline e devolve etapas, dados extraídos e resultado |
| `GET` | `/api/documentos/{id}` | Consulta o resultado salvo de uma validação |

Request de `POST /api/documentos/validar` (envie `conteudoDocumento`, `imagemBase64` ou ambos):

```json
{
  "nomeArquivo": "comprovante-residencia.pdf",
  "mimeType": "application/pdf",
  "conteudoDocumento": "COMPROVANTE DE RESIDENCIA\nNome: Joao da Silva\nCPF: 123.456.789-00\nEndereco: Rua das Flores, 150\nCidade: Guanambi - BA\nData de emissao: 10/08/2026",
  "imagemBase64": null
}
```

Resposta (resumida):

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

O header `Location` da resposta aponta para `GET /api/documentos/{id}`. `mimeType` aceita `application/pdf`, `image/png` e `image/jpeg`; `imagemBase64` tem limite de 5 MB.

## Regras

Para um comprovante de residência ser aprovado, todas precisam ser atendidas:

- Nome identificado
- CPF encontrado
- Endereco encontrado
- Cidade encontrada
- Data de emissao reconhecida
- Emissao dentro de 90 dias, sem data futura (configurável em `app.validacao.prazo-emissao-dias`)

Status:

- `APROVADO`: todas as regras atendidas.
- `REPROVADO`: documento lido por completo, mas alguma regra falhou (por exemplo, emissão antiga), ou nada foi extraído.
- `REVISAO_MANUAL`: extração incerta, só parte dos campos foi encontrada (OCR ruim ou valor da IA descartado).

## Como executar

Pré-requisito: JDK 21.

```bash
git clone https://github.com/Luisr-nunes/IDP.git
cd IDP
./mvnw test                # Linux/macOS (mvnw.cmd no Windows)
./mvnw spring-boot:run     # http://localhost:8080, perfil dev com H2 em memória
```

### Integrações opcionais

OpenAI (a chave só vem da variável de ambiente):

```powershell
$env:OPENAI_API_KEY="sua-chave"
```

Tesseract, em `application.properties` ou por variável de ambiente:

```properties
app.ocr.command=tesseract
app.ocr.tessdata-path=C:/Program Files/Tesseract-OCR/tessdata
app.ocr.language=por
```

OpenCV, com o script incluído (recebe `arquivoEntrada arquivoSaida`; requer `pip install opencv-python`):

```properties
app.opencv.command=python scripts/preprocessar_documento.py
```

### Perfil prod com PostgreSQL

```powershell
$env:DB_PASSWORD="uma-senha-local"
docker compose up -d
$env:SPRING_PROFILES_ACTIVE="prod"
./mvnw spring-boot:run
```

Variáveis do perfil prod: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD` (obrigatória).

## Fluxo para demo

1. Suba a API com `./mvnw spring-boot:run`.
2. Chame `GET /api/documentos/exemplo` e envie o payload para `POST /api/documentos/validar`.
3. Sem chave de IA nem `tessdata`, mostre o fallback local nas `etapas`.
4. Troque a data de emissão por uma antiga para ver `REPROVADO`, ou remova linhas do texto para ver `REVISAO_MANUAL`.
5. Consulte o resultado salvo pelo header `Location` (`GET /api/documentos/{id}`).
6. Configure `OPENAI_API_KEY` e `app.ocr.tessdata-path` para mostrar a versão mais próxima do mundo real; para OpenCV, aponte `app.opencv.command` para o script.

## Decisões técnicas

- **Processos externos em vez de bibliotecas Java**: Tesseract e OpenCV são chamados por comando configurável, com argumentos em lista (sem concatenar entrada do usuário), timeout de 30 segundos e arquivos temporários sempre apagados.
- **Fallback em todas as etapas**: erro ou timeout em OpenCV, OCR ou IA não derruba a requisição; a etapa usa o caminho local e informa a falha.
- **Privacidade com a IA**: o CPF é mascarado antes de o texto ir para a OpenAI e é lido localmente por regex.
- **Anti-alucinação**: nome, endereço, cidade e data devolvidos pela IA só são aceitos se aparecem no texto do documento (comparação sem acento e sem diferenciar maiúsculas).
- **Revisão manual**: extração parcial vai para `REVISAO_MANUAL` em vez de reprovar direto.
- **Minimização de dados**: o banco guarda só nome do arquivo, status, pendências e data; nunca o texto, a imagem, o nome, o CPF ou o endereço.
- **Logs**: erros registram apenas o tipo da exceção e um conversor do Logback mascara qualquer CPF que chegue a uma mensagem.
- **Segredos**: chave da IA e senha do banco só por variável de ambiente; nada no repositório.

## Tecnologias

- Java 21, Spring Boot 3.5 (Web, Validation, Data JPA)
- PostgreSQL (prod) e H2 (dev e testes)
- Tesseract OCR, OpenCV (Python) e API da OpenAI, como integrações opcionais
- JUnit 5 e MockMvc; CI com GitHub Actions
- Maven (Maven Wrapper incluído)

## Estrutura

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

## Autor

Luís Nunes — [GitHub](https://github.com/Luisr-nunes) · [LinkedIn](https://www.linkedin.com/in/luis-nunes-dev/)
