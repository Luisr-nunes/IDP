package br.com.mentoria.validacao.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo sem chave de IA, sem Tesseract e sem OpenCV: exemplo, validacao, consulta do registro salvo.
 * As datas de emissao usadas ficam fora dos 90 dias em qualquer data atual, entao os testes nao dependem de hoje.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FluxoPontaAPontaTest {

    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Test
    void exemploPassaPorTodasAsEtapasEFicaSalvo() throws Exception {
        String exemplo = mvc.perform(get("/api/documentos/exemplo")).andReturn().getResponse().getContentAsString();

        MockHttpServletResponse resposta = validar(exemplo);
        JsonNode corpo = json.readTree(resposta.getContentAsString());

        assertEquals(List.of("Documento", "OpenCV", "Tesseract", "IA", "Regras"),
                corpo.path("etapas").findValuesAsText("nome"));
        assertEquals(List.of("1", "2", "3", "4", "5"), corpo.path("etapas").findValuesAsText("ordem"));
        assertEquals("comprovante-residencia.pdf", corpo.path("etapas").path(0).path("resultado").asText());
        assertEquals("Nao executado", corpo.path("etapas").path(1).path("resultado").asText());
        assertTrue(corpo.path("etapas").path(2).path("resultado").asText().startsWith("OCR nao configurado"));
        assertTrue(corpo.path("etapas").path(3).path("resultado").asText().contains("extracao local por regex"));
        assertEquals("Joao da Silva", corpo.path("dadosExtraidos").path("nome").asText());

        String status = corpo.path("resultado").path("status").asText();
        assertEquals(status, corpo.path("etapas").path(4).path("resultado").asText());

        JsonNode registro = consultar(resposta.getHeader("Location"));
        assertEquals("comprovante-residencia.pdf", registro.path("nomeArquivo").asText());
        assertEquals(status, registro.path("status").asText());
        assertEquals(corpo.path("resultado").path("aprovado").asBoolean(), registro.path("aprovado").asBoolean());
    }

    @Test
    void documentoCompletoForaDoPrazoEReprovado() throws Exception {
        JsonNode corpo = json.readTree(validar(requisicao(
                "Nome: Ana Lima\\nCPF: 529.982.247-25\\nEndereco: Rua C, 3\\nCidade: Natal\\nData de emissao: 05/03/2021"))
                .getContentAsString());

        assertEquals("REPROVADO", corpo.path("resultado").path("status").asText());
        assertEquals(false, corpo.path("resultado").path("aprovado").asBoolean());
        assertEquals(List.of("Falhou: Emissao dentro de 90 dias"),
                json.convertValue(corpo.path("resultado").path("pendencias"), List.class));
    }

    @Test
    void documentoComCamposFaltandoVaiParaRevisaoManual() throws Exception {
        JsonNode corpo = json.readTree(validar(requisicao("Nome: Ana Lima\\nCidade: Natal")).getContentAsString());

        assertEquals("REVISAO_MANUAL", corpo.path("resultado").path("status").asText());
        assertEquals("REVISAO_MANUAL", corpo.path("etapas").path(4).path("resultado").asText());
    }

    private static String requisicao(String conteudo) {
        return "{\"nomeArquivo\":\"doc.pdf\",\"mimeType\":\"application/pdf\",\"conteudoDocumento\":\"" + conteudo + "\"}";
    }

    private MockHttpServletResponse validar(String corpo) throws Exception {
        return mvc.perform(post("/api/documentos/validar").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    private JsonNode consultar(String location) throws Exception {
        return json.readTree(mvc.perform(get(location)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }
}
