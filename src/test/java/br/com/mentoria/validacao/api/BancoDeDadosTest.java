package br.com.mentoria.validacao.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dados extraidos: abertos na tela Banco de Dados e no JSON, cifrados na coluna do banco. */
@SpringBootTest
@AutoConfigureMockMvc
class BancoDeDadosTest {

    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long validarExemplo() throws Exception {
        String exemplo = mvc.perform(get("/api/documentos/exemplo")).andReturn().getResponse().getContentAsString();
        String location = mvc.perform(post("/api/documentos/validar").contentType(MediaType.APPLICATION_JSON).content(exemplo))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private JsonNode registro(JsonNode lista, long id) {
        for (JsonNode r : lista) {
            if (r.path("id").asLong() == id) {
                return r;
            }
        }
        throw new AssertionError("registro " + id + " nao listado");
    }

    @Test
    void bancoMostraOsDadosExtraidosDosDoisDocumentos() throws Exception {
        long id = validarExemplo();

        JsonNode r = registro(json.readTree(mvc.perform(get("/api/banco")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()), id);

        assertEquals("APROVADO", r.path("status").asText());
        assertEquals("Joao da Silva", r.path("comprovante").path("nome").asText());
        assertEquals("529.982.247-25", r.path("comprovante").path("cpf").asText());
        assertEquals("Guanambi - BA", r.path("comprovante").path("cidade").asText());
        assertEquals("CNH", r.path("identificacao").path("tipo").asText());
    }

    @Test
    void colunaNoBancoFicaCifradaEHistoricoSegueSemDadosPessoais() throws Exception {
        long id = validarExemplo();

        String coluna = jdbc.queryForObject("select dados_extraidos from validacao_registro where id = ?", String.class, id);
        assertFalse(coluna.contains("Joao") || coluna.contains("529.982.247-25") || coluna.contains("Flores"));

        String historico = mvc.perform(get("/api/documentos")).andReturn().getResponse().getContentAsString();
        String consulta = mvc.perform(get("/api/documentos/" + id)).andReturn().getResponse().getContentAsString();
        assertFalse(historico.contains("Joao") || historico.contains("529.982.247-25"));
        assertFalse(consulta.contains("Joao") || consulta.contains("dadosExtraidos"));
    }

    @Test
    void exportaJsonComoArquivoParaMigracao() throws Exception {
        long id = validarExemplo();

        var resposta = mvc.perform(get("/api/banco/exportar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"idp-banco-de-dados-")))
                .andReturn().getResponse();
        JsonNode corpo = json.readTree(resposta.getContentAsString());

        assertEquals(corpo.path("registros").size(), corpo.path("total").asInt());
        assertTrue(corpo.path("exportadoEm").isTextual());
        assertEquals("Joao da Silva", registro(corpo.path("registros"), id).path("comprovante").path("nome").asText());
        assertEquals(registro(corpo.path("registros"), id).path("comprovante").path("dataEmissao").asText().length(), 10);
    }
}
