package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.api.ValidacaoRequest;
import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.StatusValidacao;
import br.com.mentoria.validacao.dominio.Texto;
import br.com.mentoria.validacao.persistencia.ValidacaoRegistroRepository;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/** Conferencia anti-alucinacao dos valores da IA, revisao manual e fallback quando um processo externo falha. */
class ConferenciaERevisaoManualTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);
    private static final String TEXTO = "Nome: JOAO DA SILVA\nCidade: Guanambi  -  BA\nData de emissao: 10/08/2026";
    private final ValidadorComprovante validador = new ValidadorComprovante(90);

    @Test
    void normalizaSemAcentoEmMaiusculas() {
        assertEquals("JOAO DA SILVA", Texto.normalizar("  João   da Silva "));
        assertEquals("SAO PAULO", Texto.normalizar("são paulo"));
        assertNull(Texto.normalizar(null));
    }

    @Test
    void aceitaValorDaIaQueApareceNoTexto() {
        assertEquals("João da Silva", InterpretadorIaService.noTexto("João da Silva", TEXTO));
        assertEquals("Guanambi - BA", InterpretadorIaService.noTexto("Guanambi - BA", TEXTO));
        assertEquals("10/08/2026", InterpretadorIaService.noTexto("10/08/2026", TEXTO));
    }

    @Test
    void descartaValorDaIaQueNaoApareceNoTexto() {
        assertNull(InterpretadorIaService.noTexto("Maria Souza", TEXTO));
        assertNull(InterpretadorIaService.noTexto("11/08/2026", TEXTO));
        assertNull(InterpretadorIaService.noTexto(null, TEXTO));
    }

    @Test
    void extracaoParcialVaiParaRevisaoManual() {
        var dados = new DadosComprovante("Joao da Silva", null, null, "Recife", HOJE);
        var resultado = validador.validar(dados, HOJE);
        assertEquals(StatusValidacao.REVISAO_MANUAL, resultado.status());
        assertEquals(false, resultado.aprovado());
    }

    @Test
    void nadaExtraidoReprova() {
        var dados = new DadosComprovante(null, null, null, null, null);
        assertEquals(StatusValidacao.REPROVADO, validador.validar(dados, HOJE).status());
    }

    @Test
    void documentoCompletoForaDoPrazoReprovaSemRevisaoManual() {
        var dados = new DadosComprovante("Joao da Silva", "123.456.789-00", "Rua A, 1", "Recife", HOJE.minusDays(91));
        assertEquals(StatusValidacao.REPROVADO, validador.validar(dados, HOJE).status());
    }

    @Test
    void falhaDoTesseractCaiNoConteudoDocumento() throws IOException {
        var service = new ValidacaoDocumentoService(new InterpretadorIaService("", new ExtratorTexto()), validador,
                new OcrService("comando-inexistente-idp", "C:/tessdata", "por"), new OpenCvService(""), mock(ValidacaoRegistroRepository.class));
        String imagem = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});

        var resposta = service.validar(new ValidacaoRequest("doc.png", "image/png", "Nome: Maria Souza", imagem));

        assertEquals("Maria Souza", resposta.dadosExtraidos().nome());
        assertTrue(resposta.etapas().get(2).resultado().startsWith("Falha no OCR"));
    }
}
