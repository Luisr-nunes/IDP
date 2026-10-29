package br.com.mentoria.validacao.api;

import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.EtapaPipeline;
import br.com.mentoria.validacao.dominio.ResultadoValidacao;
import br.com.mentoria.validacao.dominio.StatusValidacao;
import br.com.mentoria.validacao.servico.ValidacaoDocumentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentoController.class)
class DocumentoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ValidacaoDocumentoService service;

    @Test
    void exemploRetornaPayloadDeRequisicao() throws Exception {
        mvc.perform(get("/api/documentos/exemplo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeArquivo").value("comprovante-residencia.pdf"))
                .andExpect(jsonPath("$.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.conteudoDocumento").isNotEmpty());
    }

    @Test
    void validarRetornaEtapasDadosEResultado() throws Exception {
        DadosComprovante dados = new DadosComprovante("Joao da Silva", "123.456.789-00",
                "Rua das Flores, 150", "Guanambi - BA", LocalDate.of(2026, 8, 10));
        ResultadoValidacao resultado = new ResultadoValidacao(true, StatusValidacao.APROVADO,
                List.of("Nome identificado"), List.of());
        when(service.validar(any())).thenReturn(new ValidacaoResponse("doc.pdf",
                List.of(new EtapaPipeline(1, "Documento", "Entrada do arquivo para o pipeline", "doc.pdf")),
                dados, resultado));

        mvc.perform(post("/api/documentos/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeArquivo\":\"doc.pdf\",\"mimeType\":\"application/pdf\",\"conteudoDocumento\":\"texto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeArquivo").value("doc.pdf"))
                .andExpect(jsonPath("$.etapas[0].ordem").value(1))
                .andExpect(jsonPath("$.dadosExtraidos.cidade").value("Guanambi - BA"))
                .andExpect(jsonPath("$.resultado.aprovado").value(true))
                .andExpect(jsonPath("$.resultado.status").value("APROVADO"));
    }
}
