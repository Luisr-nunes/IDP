package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.dominio.DadosIdentificacao;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** RG escaneado: texto do OCR com nome entre << >>, numero de serie com 11 digitos e frente e verso no PDF. */
class LeituraRgTest {

    private final ExtratorTexto extrator = new ExtratorTexto();

    @Test
    void leNomeEntreSetasEIgnoraNumeroDeSerieComoCpf() {
        String ocr = """
                REGISTRO GERAL
                CU 9.400.649 20/11/2017
                << JOAO DA SILVA >>
                << MARIA DA SILVA >>
                CPF
                529.982.247-25
                15175836824111911207788188
                """;

        assertEquals(new DadosIdentificacao("RG", "JOAO DA SILVA", "529.982.247-25", null),
                extrator.extrairIdentificacao(ocr));
    }

    @Test
    void semCpfLegivelNaoUsaONumeroDeSerie() {
        DadosIdentificacao rg = extrator.extrairIdentificacao("REGISTRO GERAL\n«<< JOAO DA SILVA >>\n15175836824111911207788188");

        assertEquals("JOAO DA SILVA", rg.nome());
        assertNull(rg.cpf());
    }

    @Test
    void pdfEscaneadoViraUmaImagemPorPagina() throws IOException {
        byte[] duasPaginas = LeituraPdfTest.pdfComPaginas(2);
        assertEquals(2, new PdfService().paginasComoImagem(duasPaginas).size());
    }
}
