# Validação de Documentos com Java

Projeto didático para mentoria mostrando o fluxo da imagem:

1. Documento entra no sistema.
2. OpenCV faz o pré-processamento.
3. Tesseract extrai o texto.
4. IA interpreta o texto e monta um objeto estruturado.
5. Java aplica as regras de negócio e retorna o resultado.

Neste exemplo, a aplicação já suporta integração real com OpenAI e com OCR/pré-processamento via executáveis externos, mas continua com fallback local para a mentoria funcionar mesmo sem credenciais ou ferramentas instaladas.

> Este repositório está apenas preparado/documentado para um projeto Java com essas características. As funcionalidades descritas abaixo não estão sendo implementadas neste momento.

## Como executar

```bash
mvn spring-boot:run
```

## Endpoint de exemplo

`GET /api/documentos/exemplo`

Retorna um payload pronto para demonstração.

## Endpoint principal

`POST /api/documentos/validar`

Exemplo de request:

```json
{
  "nomeArquivo": "comprovante-residencia.pdf",
  "mimeType": "application/pdf",
  "conteudoDocumento": "COMPROVANTE DE RESIDENCIA\nNome: Joao da Silva\nCPF: 123.456.789-00\nEndereco: Rua das Flores, 150\nCidade: Guanambi - BA\nData de emissao: 10/08/2026",
  "imagemBase64": null
}
```

Você pode enviar apenas `conteudoDocumento`, apenas `imagemBase64`, ou ambos.

Exemplo de retorno:

```json
{
  "nomeArquivo": "comprovante-residencia.pdf",
  "etapas": [
    {
      "ordem": 1,
      "nome": "Documento",
      "descricao": "Entrada do arquivo para o pipeline",
      "resultado": "comprovante-residencia.pdf"
    }
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
    "regrasAtendidas": [
      "Nome identificado",
      "CPF encontrado",
      "Endereco encontrado",
      "Cidade encontrada",
      "Data de emissao reconhecida",
      "Emissao dentro de 90 dias"
    ],
    "pendencias": []
  }
}
```

## Integrações reais

### OpenAI

Defina a chave antes de subir a aplicação:

```powershell
$env:OPENAI_API_KEY="sua-chave"
```

Ou configure em `application.properties`:

```properties
app.ai.openai.api-key=${OPENAI_API_KEY:}
```

Quando a chave estiver configurada, `InterpretadorIaService` chama a API da OpenAI e pede um JSON estruturado.

### Tesseract

Instale o Tesseract e, se necessário, configure o caminho do `tessdata`:

```properties
app.ocr.command=tesseract
app.ocr.tessdata-path=C:/tesseract/tessdata
app.ocr.language=por
```

Se o caminho não estiver configurado, a aplicação usa o texto enviado em `conteudoDocumento` como fallback.

### OpenCV

Configure um comando externo que receba `arquivoEntrada arquivoSaida` e faça o pré-processamento real com OpenCV. Exemplo com Python:

```properties
app.opencv.command=python scripts/preprocessar_documento.py
```

Quando `imagemBase64` for enviado, `OpenCvService` chama esse comando, recebe a imagem tratada e a encaminha para o Tesseract.

## Fluxo recomendado para demo

1. Suba a API com `mvn spring-boot:run`.
2. Use `GET /api/documentos/exemplo` para pegar um payload base.
3. Comece sem chave OpenAI nem `tessdata` para mostrar o fallback local.
4. Depois configure OpenAI e `tessdata` para mostrar a versão mais próxima do mundo real.
5. Se quiser mostrar OpenCV real, aponte `app.opencv.command` para um script local com `cv2`.

## Estrutura base esperada do projeto Java

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.5</version>
        <relativePath />
    </parent>

    <groupId>br.com.mentoria</groupId>
    <artifactId>validacao-documentos</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>validacao-documentos</name>
    <description>Exemplo didático de validação de documentos com Java</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```
