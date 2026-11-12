package br.com.mentoria.validacao.persistencia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Usa o H2 em memoria: valida, consulta o registro salvo e confere que nao ha dados pessoais nele. */
@SpringBootTest
@AutoConfigureMockMvc
class PersistenciaValidacaoTest {

    private static final String REQUISICAO = """
            {"nomeArquivo":"conta-luz.pdf","mimeType":"application/pdf",
             "conteudoDocumento":"Nome: Maria Souza\\nCPF: 529.982.247-25\\nEndereco: Rua B, 20\\nCidade: Recife\\nData de emissao: 01/01/2020"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ValidacaoRegistroRepository registros;

    @Test
    void salvaEConsultaOResultadoDaValidacao() throws Exception {
        String location = mvc.perform(post("/api/documentos/validar")
                        .contentType(MediaType.APPLICATION_JSON).content(REQUISICAO))
                .andExpect(status().isOk())
                .andExpect(header().string("Location", startsWith("/api/documentos/")))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andReturn().getResponse().getHeader("Location");

        // emissao em 2020: fora dos 90 dias em qualquer data atual, entao o resultado e estavel
        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeArquivo").value("conta-luz.pdf"))
                .andExpect(jsonPath("$.status").value("REPROVADO"))
                .andExpect(jsonPath("$.aprovado").value(false))
                .andExpect(jsonPath("$.pendencias[0]").value("Falhou: Emissao dentro de 90 dias"))
                .andExpect(jsonPath("$.criadoEm").isNotEmpty());
    }

    @Test
    void registroNaoGuardaTextoNemDadosPessoais() throws Exception {
        mvc.perform(post("/api/documentos/validar").contentType(MediaType.APPLICATION_JSON).content(REQUISICAO))
                .andExpect(status().isOk());

        for (ValidacaoRegistro registro : registros.findAll()) {
            String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(registro);
            assertFalse(json.contains("Maria Souza"));
            assertFalse(json.contains("529.982.247-25"));
            assertFalse(json.contains("Rua B"));
        }
        assertEquals(false, registros.findAll().isEmpty());
    }

    @Test
    void idInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/documentos/999999")).andExpect(status().isNotFound());
    }
}
