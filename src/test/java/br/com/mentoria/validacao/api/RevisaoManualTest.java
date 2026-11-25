package br.com.mentoria.validacao.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O analista decide os documentos que caem em revisao manual. */
@SpringBootTest
@AutoConfigureMockMvc
class RevisaoManualTest {

    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    private String validar(String conteudo) throws Exception {
        return mvc.perform(post("/api/documentos/validar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeArquivo\":\"doc.pdf\",\"mimeType\":\"application/pdf\",\"conteudoDocumento\":\"" + conteudo + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Location");
    }

    private ResultActions revisar(String location, String corpo) throws Exception {
        return mvc.perform(post(location + "/revisao").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void analistaAprovaDocumentoEmRevisaoEOMotivoFicaSalvoSemCpf() throws Exception {
        String location = validar("Nome: Ana Lima\\nCidade: Natal");

        revisar(location, "{\"decisao\":\"APROVADO\",\"motivo\":\"Conferi o original, CPF 529.982.247-25\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.revisadoManualmente").value(true));

        JsonNode registro = json.readTree(mvc.perform(get(location)).andReturn().getResponse().getContentAsString());
        assertEquals("APROVADO", registro.path("status").asText());
        assertEquals("Conferi o original, CPF ***.***.***-**", registro.path("motivoRevisao").asText());
    }

    @Test
    void naoRevisaDocumentoQueNaoEstaEmRevisao() throws Exception {
        String location = validar("Nome: Ana Lima\\nCPF: 529.982.247-25\\nEndereco: Rua C, 3\\nCidade: Natal\\nData de emissao: 05/03/2021");

        revisar(location, "{\"decisao\":\"APROVADO\",\"motivo\":\"ok\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("So documentos em revisao manual podem ser revisados"));
    }

    @Test
    void exigeDecisaoValidaEMotivo() throws Exception {
        String location = validar("Nome: Ana Lima\\nCidade: Natal");

        revisar(location, "{\"decisao\":\"TALVEZ\",\"motivo\":\" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasItem("decisao deve ser APROVADO ou REPROVADO")))
                .andExpect(jsonPath("$.erros", hasItem("Informe o motivo da decisao")));
    }

    @Test
    void idInexistenteDa404() throws Exception {
        revisar("/api/documentos/999999", "{\"decisao\":\"REPROVADO\",\"motivo\":\"x\"}").andExpect(status().isNotFound());
    }
}
