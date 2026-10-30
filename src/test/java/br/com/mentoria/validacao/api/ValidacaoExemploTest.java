package br.com.mentoria.validacao.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fluxo real (sem mocks): busca o payload de exemplo e o envia para validacao. */
@SpringBootTest
@AutoConfigureMockMvc
class ValidacaoExemploTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void validaOPayloadDoEndpointDeExemplo() throws Exception {
        String exemplo = mvc.perform(get("/api/documentos/exemplo"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // a regra "Emissao dentro de 90 dias" depende da data de hoje, por isso nao e verificada aqui
        mvc.perform(post("/api/documentos/validar").contentType(MediaType.APPLICATION_JSON).content(exemplo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeArquivo").value("comprovante-residencia.pdf"))
                .andExpect(jsonPath("$.etapas", hasSize(5)))
                .andExpect(jsonPath("$.dadosExtraidos.nome").value("Joao da Silva"))
                .andExpect(jsonPath("$.dadosExtraidos.cpf").value("123.456.789-00"))
                .andExpect(jsonPath("$.dadosExtraidos.endereco").value("Rua das Flores, 150"))
                .andExpect(jsonPath("$.dadosExtraidos.cidade").value("Guanambi - BA"))
                .andExpect(jsonPath("$.dadosExtraidos.dataEmissao").value("2026-08-10"))
                .andExpect(jsonPath("$.resultado.regrasAtendidas", hasItems("Nome identificado", "CPF encontrado",
                        "Endereco encontrado", "Cidade encontrada", "Data de emissao reconhecida")));
    }
}
