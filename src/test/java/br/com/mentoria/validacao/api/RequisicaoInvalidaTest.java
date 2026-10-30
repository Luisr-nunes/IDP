package br.com.mentoria.validacao.api;

import br.com.mentoria.validacao.servico.ValidacaoDocumentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentoController.class)
class RequisicaoInvalidaTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ValidacaoDocumentoService service;

    private ResultActions validar(String json) throws Exception {
        return mvc.perform(post("/api/documentos/validar").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void rejeitaSemConteudoESemImagem() throws Exception {
        validar("{\"nomeArquivo\":\"a.pdf\",\"mimeType\":\"application/pdf\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasItem("Informe conteudoDocumento ou imagemBase64")));
    }

    @Test
    void rejeitaMimeTypeNaoSuportado() throws Exception {
        validar("{\"mimeType\":\"text/html\",\"conteudoDocumento\":\"texto\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros",
                        hasItem("mimeType deve ser application/pdf, image/png ou image/jpeg")));
    }

    @Test
    void rejeitaSemMimeType() throws Exception {
        validar("{\"conteudoDocumento\":\"texto\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasItem("Informe o mimeType")));
    }

    @Test
    void rejeitaImagemAcimaDoLimite() throws Exception {
        String imagem = "A".repeat(ValidacaoRequest.LIMITE_IMAGEM_BASE64 + 1);
        validar("{\"mimeType\":\"image/png\",\"imagemBase64\":\"" + imagem + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasItem("imagemBase64 excede o limite de 5 MB")));
    }

    @Test
    void jsonMalFormadoNaoEcoaOConteudoEnviado() throws Exception {
        validar("{\"conteudoDocumento\":\"CPF 529.982.247-25\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("JSON invalido ou mal formatado"))
                .andExpect(content().string(not(containsString("529.982.247-25"))));
    }

    @Test
    void erroInternoNaoExpoeMensagemDaExcecao() throws Exception {
        when(service.validar(any())).thenThrow(new IllegalStateException("CPF 529.982.247-25"));
        validar("{\"mimeType\":\"application/pdf\",\"conteudoDocumento\":\"texto\"}")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Erro interno ao processar o documento"))
                .andExpect(content().string(not(containsString("529.982.247-25"))));
    }
}
