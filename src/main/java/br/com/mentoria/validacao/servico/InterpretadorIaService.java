package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.dominio.Cpf;
import br.com.mentoria.validacao.dominio.DadosComprovante;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Interpreta o texto do OCR com a OpenAI, pedindo um JSON estruturado com os campos do comprovante. */
@Service
public class InterpretadorIaService {

    private static final URI URL = URI.create("https://api.openai.com/v1/chat/completions");
    private static final String MODELO = "gpt-4o-mini";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final DateTimeFormatter DD_MM_AAAA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final String INSTRUCOES = """
            Voce extrai dados de um comprovante de residencia a partir do texto de OCR.
            Responda somente com um JSON com as chaves nome, endereco, cidade e dataEmissao.
            dataEmissao no formato dd/MM/yyyy. Use null para o que nao estiver no texto. Nao invente valores.""";

    private final String apiKey;
    private final ExtratorTexto extrator;
    private final ObjectMapper json = new ObjectMapper();

    public InterpretadorIaService(@Value("${app.ai.openai.api-key:}") String apiKey, ExtratorTexto extrator) {
        this.apiKey = apiKey;
        this.extrator = extrator;
    }

    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** O texto vai para a IA com o CPF mascarado; o CPF e lido localmente. */
    public DadosComprovante interpretar(String texto) throws IOException {
        JsonNode campos = json.readTree(chamarOpenAi(Cpf.mascarar(texto == null ? "" : texto)));
        return new DadosComprovante(texto(campos, "nome"), extrator.extrair(texto).cpf(), texto(campos, "endereco"),
                texto(campos, "cidade"), data(texto(campos, "dataEmissao")));
    }

    private String chamarOpenAi(String textoMascarado) throws IOException {
        ObjectNode corpo = json.createObjectNode().put("model", MODELO).put("temperature", 0);
        corpo.putObject("response_format").put("type", "json_object");
        corpo.putArray("messages")
                .add(json.createObjectNode().put("role", "system").put("content", INSTRUCOES))
                .add(json.createObjectNode().put("role", "user").put("content", textoMascarado));
        HttpRequest requisicao = HttpRequest.newBuilder(URL)
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo)))
                .build();
        HttpResponse<String> resposta;
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()) {
            resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Chamada a OpenAI interrompida", e);
        }
        if (resposta.statusCode() != 200) {
            throw new IOException("OpenAI respondeu com status " + resposta.statusCode());
        }
        return json.readTree(resposta.body()).path("choices").path(0).path("message").path("content").asText("{}");
    }

    private static String texto(JsonNode campos, String chave) {
        JsonNode valor = campos.path(chave);
        return valor.isTextual() && !valor.asText().isBlank() ? valor.asText().trim() : null;
    }

    private static LocalDate data(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return LocalDate.parse(valor, DD_MM_AAAA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
