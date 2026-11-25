# IDP — Validação Inteligente de Documentos

<!-- badges -->
![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat-square&logo=javascript&logoColor=black) ![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=flat-square&logo=html5&logoColor=white) ![CSS](https://img.shields.io/badge/CSS-1572B6?style=flat-square&logo=css3&logoColor=white) ![Python](https://img.shields.io/badge/Python-3776AB?style=flat-square&logo=python&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white) ![JUnit 5](https://img.shields.io/badge/JUnit%205-25A162?style=flat-square&logo=junit5&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)

[Read in English](README.en.md)

Sistema em Java e Spring Boot que valida documentos automaticamente para análise de cadastro: recebe o comprovante de residência e, opcionalmente, o RG ou a CNH do titular, trata a imagem, extrai o texto, estrutura os dados com IA e aplica as regras de negócio para aprovar, reprovar ou encaminhar para revisão manual. Tem tela web para o analista e API REST. Projeto da Mentoria Avanade 2026.2.

## Fluxo

```
Upload (PDF/imagem)                               tela web ou POST /api/documentos/validar
   → PDF com texto: leitura direta               (PdfService, PDFBox)
   → OpenCV: pré-processamento da imagem          (OpenCvService, comando externo)
   → Tesseract: extração do texto                 (OcrService, comando externo)
   → IA: texto em dados estruturados              (InterpretadorIaService, OpenAI)
   → Regras de negócio e validação cruzada        (ValidadorComprovante)
   → Banco: resultado salvo                       (ValidacaoRegistro, H2/PostgreSQL)
   → APROVADO | REPROVADO | REVISAO_MANUAL        consulta por GET /api/documentos/{id}
```

Cada etapa tem fallback local, então o sistema funciona sem chave de IA, sem Tesseract e sem OpenCV instalados:

| Etapa | Com configuração | Sem configuração ou em caso de falha |
| --- | --- | --- |
| PDF | PDF com texto é lido direto; PDF escaneado vira imagem (300 DPI) | PDF inválido usa o texto de `conteudoDocumento` |
| OpenCV | `app.opencv.command` trata a imagem | segue com a imagem original |
| Tesseract | `app.ocr.tessdata-path` extrai o texto da imagem | usa o texto de `conteudoDocumento` |
| IA | `OPENAI_API_KEY` interpreta o texto | extração local por regex |

A resposta mostra, em `etapas`, qual caminho foi usado em cada uma das 6 etapas.

## Tela web

Abra `http://localhost:8080`. O analista envia o comprovante e o RG/CNH por arquivo (PDF, PNG ou JPG até 5 MB) ou cola o texto, e vê o status, as etapas do pipeline, os dados extraídos de cada documento, as regras atendidas e as que falharam, além do histórico das últimas validações. Segue o estilo do Notion, com modo escuro automático.

## Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/` | Tela web |
| `GET` | `/api/documentos/exemplo` | Payload de exemplo (comprovante + CNH), sempre aprovado |
| `POST` | `/api/documentos/validar` | Executa o pipeline e devolve etapas, dados extraídos e resultado |
| `GET` | `/api/documentos/{id}` | Consulta o resultado salvo de uma validação |
| `GET` | `/api/documentos` | Histórico das 20 validações mais recentes |

Request de `POST /api/documentos/validar` (em cada documento, envie `conteudoDocumento`, `imagemBase64` ou ambos; `identificacao` é opcional):

```json
{
  "nomeArquivo": "comprovante-residencia.pdf",
  "mimeType": "application/pdf",
  "conteudoDocumento": "COMPROVANTE DE RESIDENCIA\nNome: Joao da Silva\nCPF: 529.982.247-25\nEndereco: Rua das Flores, 150\nCidade: Guanambi - BA\nData de emissao: 15/11/2026",
  "imagemBase64": null,
  "identificacao": {
    "nomeArquivo": "cnh.pdf",
    "mimeType": "application/pdf",
    "conteudoDocumento": "CARTEIRA NACIONAL DE HABILITACAO\nNome: Joao da Silva\nCPF: 529.982.247-25\nValidade: 25/11/2028"
  }
}
```

Resposta (resumida):

```json
{
  "nomeArquivo": "comprovante-residencia.pdf",
  "etapas": [
    { "ordem": 1, "nome": "Documento", "descricao": "Entrada do arquivo para o pipeline", "resultado": "comprovante-residencia.pdf + cnh.pdf" },
    { "ordem": 6, "nome": "Banco", "descricao": "Resultado salvo para consulta", "resultado": "Registro 1 salvo" }
  ],
  "dadosExtraidos": { "nome": "Joao da Silva", "cpf": "529.982.247-25", "endereco": "Rua das Flores, 150", "cidade": "Guanambi - BA", "dataEmissao": "2026-11-15" },
  "identificacao": { "tipo": "CNH", "nome": "Joao da Silva", "cpf": "529.982.247-25", "validade": "2028-11-25" },
  "resultado": { "aprovado": true, "status": "APROVADO", "regrasAtendidas": ["Documento legivel", "CPF valido", "Nome igual no comprovante e na CNH", "..."], "pendencias": [] }
}
```

O header `Location` da resposta aponta para `GET /api/documentos/{id}`. `mimeType` aceita `application/pdf`, `image/png` e `image/jpeg`; `imagemBase64` precisa ser Base64 válido e tem limite de 5 MB.

## Regras

Comprovante de residência:

- Documento legível (pelo menos 20 caracteres lidos)
- Nome, CPF, endereço, cidade e data de emissão encontrados
- CPF válido (dígitos verificadores)
- Emissão dentro de 90 dias, sem data futura (configurável em `app.validacao.prazo-emissao-dias`)

Com RG ou CNH (validação cruzada):

- Documento legível, nome e CPF encontrados
- Nome igual nos dois documentos (sem diferenciar acento e maiúsculas)
- CPF igual nos dois documentos
- CNH dentro da validade

Status:

- `APROVADO`: todas as regras atendidas.
- `REPROVADO`: documento ilegível, CPF inválido, emissão fora do prazo, nome ou CPF divergente, CNH vencida, ou nada extraído. A resposta sempre traz o motivo em `pendencias`.
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

Tesseract (necessário só para imagens e PDFs escaneados), com o idioma `por` instalado:

```powershell
$env:APP_OCR_TESSDATAPATH="C:/Program Files/Tesseract-OCR/tessdata"
```

OpenCV, com o script incluído (recebe `arquivoEntrada arquivoSaida`; requer `pip install opencv-python-headless`):

```powershell
$env:APP_OPENCV_COMMAND="python scripts/preprocessar_documento.py"
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

1. Suba com `./mvnw spring-boot:run` e abra `http://localhost:8080`.
2. Clique em **Usar exemplo** e depois em **Validar documentos**: comprovante e CNH aprovados, com as 6 etapas e as regras.
3. Na CNH, troque o nome ou o CPF e valide de novo: **Reprovado** por divergência entre os documentos.
4. Troque a validade da CNH ou a data de emissão por uma antiga: **Reprovado** com o motivo.
5. Apague linhas do comprovante: **Revisão manual**.
6. Envie um PDF real na aba **Arquivo**: o texto é lido direto do PDF, sem OCR.
7. Mostre o **Histórico** e a consulta pela API (`GET /api/documentos/{id}`).
8. Com `OPENAI_API_KEY` e Tesseract configurados, repita com uma foto do documento para mostrar OCR e IA reais.

## Decisões técnicas

- **Processos externos em vez de bibliotecas Java**: Tesseract e OpenCV são chamados por comando configurável, com argumentos em lista (sem concatenar entrada do usuário), timeout de 30 segundos e arquivos temporários sempre apagados.
- **PDF antes do OCR**: PDF com texto é lido pelo PDFBox, mais rápido e exato que OCR; só PDF escaneado vira imagem.
- **Fallback em todas as etapas**: erro ou timeout em PDF, OpenCV, OCR ou IA não derruba a requisição; a etapa usa o caminho local e informa a falha.
- **Validação cruzada local**: RG e CNH têm rótulos fixos, então nome, CPF e validade saem por regex, sem enviar o documento de identidade para a IA.
- **Privacidade com a IA**: o CPF é mascarado antes de o texto ir para a OpenAI e é lido localmente por regex.
- **Anti-alucinação**: valores devolvidos pela IA só são aceitos se aparecem no texto do documento.
- **Revisão manual**: extração parcial vai para `REVISAO_MANUAL`; falha de regra (CPF inválido, prazo, divergência) reprova.
- **Minimização de dados**: o banco guarda só nome do arquivo, status, pendências e data; nunca o texto, a imagem, o nome, o CPF ou o endereço.
- **Segurança na tela**: dados da API entram na página só por `textContent`, sem `innerHTML`; entrada validada no back-end (tipo, tamanho e Base64).
- **Logs**: erros registram apenas o tipo da exceção e um conversor do Logback mascara qualquer CPF que chegue a uma mensagem.
- **Segredos**: chave da IA e senha do banco só por variável de ambiente; nada no repositório.

## Tecnologias

- Java 21, Spring Boot 3.5 (Web, Validation, Data JPA), PDFBox
- HTML, CSS e JavaScript sem framework na tela web
- PostgreSQL (prod) e H2 (dev e testes)
- Tesseract OCR, OpenCV (Python) e API da OpenAI, como integrações opcionais
- JUnit 5 e MockMvc; CI com GitHub Actions
- Maven (Maven Wrapper incluído)

## Estrutura

```
IDP/
├── src/main/java/br/com/mentoria/validacao/
│   ├── api/           # DocumentoController, ValidacaoRequest, DocumentoIdentificacao, ValidacaoResponse, TratadorDeErros
│   ├── dominio/       # DadosComprovante, DadosIdentificacao, EtapaPipeline, ResultadoValidacao, StatusValidacao, Cpf, Texto
│   ├── servico/       # ValidacaoDocumentoService, PdfService, OpenCvService, OcrService, InterpretadorIaService,
│   │                  # ExtratorTexto, ValidadorComprovante
│   ├── persistencia/  # ValidacaoRegistro, ValidacaoRegistroRepository
│   └── log/           # MascaraDadosPessoaisConverter
├── src/main/resources/
│   ├── static/        # tela web: index.html, app.css, app.js
│   └── application.properties, application-dev/prod.properties, logback-spring.xml
├── scripts/preprocessar_documento.py
├── docker-compose.yml
└── pom.xml
```

## Autor

Luís Nunes — [GitHub](https://github.com/Luisr-nunes) · [LinkedIn](https://www.linkedin.com/in/luis-nunes-dev/)
