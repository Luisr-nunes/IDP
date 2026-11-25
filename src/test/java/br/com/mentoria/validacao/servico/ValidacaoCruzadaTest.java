package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.DadosIdentificacao;
import br.com.mentoria.validacao.dominio.StatusValidacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Comprovante + RG/CNH: legibilidade, nome e CPF iguais nos dois documentos e validade da CNH. */
class ValidacaoCruzadaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 25);
    private static final String CPF = "529.982.247-25";
    private static final DadosComprovante COMPROVANTE =
            new DadosComprovante("Joao da Silva", CPF, "Rua das Flores, 150", "Recife", HOJE.minusDays(5));
    private final ValidadorComprovante validador = new ValidadorComprovante(90);
    private final ExtratorTexto extrator = new ExtratorTexto();

    @Test
    void aprovaQuandoNomeECpfBatemEACnhEstaValida() {
        var cnh = new DadosIdentificacao("CNH", "JOÃO DA SILVA", "52998224725", HOJE.plusYears(1));
        var resultado = validador.validar(COMPROVANTE, 100, cnh, 80, HOJE);
        assertEquals(StatusValidacao.APROVADO, resultado.status());
        assertTrue(resultado.regrasAtendidas().containsAll(List.of("Documento legivel", "CNH legivel",
                "Nome igual no comprovante e na CNH", "CPF igual no comprovante e na CNH", "CNH dentro da validade")));
    }

    @Test
    void reprovaNomeDivergente() {
        var rg = new DadosIdentificacao("RG", "Maria Souza", CPF, null);
        var resultado = validador.validar(COMPROVANTE, 100, rg, 80, HOJE);
        assertEquals(StatusValidacao.REPROVADO, resultado.status());
        assertEquals(List.of("Falhou: Nome igual no comprovante e no RG"), resultado.pendencias());
    }

    @Test
    void reprovaCpfDivergente() {
        var rg = new DadosIdentificacao("RG", "Joao da Silva", "111.444.777-35", null);
        assertEquals(List.of("Falhou: CPF igual no comprovante e no RG"),
                validador.validar(COMPROVANTE, 100, rg, 80, HOJE).pendencias());
    }

    @Test
    void reprovaCnhVencida() {
        var cnh = new DadosIdentificacao("CNH", "Joao da Silva", CPF, HOJE.minusDays(1));
        var resultado = validador.validar(COMPROVANTE, 100, cnh, 80, HOJE);
        assertEquals(StatusValidacao.REPROVADO, resultado.status());
        assertEquals(List.of("Falhou: CNH dentro da validade"), resultado.pendencias());
    }

    @Test
    void reprovaDocumentoIlegivel() {
        var resultado = validador.validar(COMPROVANTE, 5, null, null, HOJE);
        assertEquals(StatusValidacao.REPROVADO, resultado.status());
        assertEquals(List.of("Falhou: Documento legivel"), resultado.pendencias());
    }

    @Test
    void rgSemCpfVaiParaRevisaoManual() {
        var rg = new DadosIdentificacao("RG", "Joao da Silva", null, null);
        assertEquals(StatusValidacao.REVISAO_MANUAL, validador.validar(COMPROVANTE, 100, rg, 80, HOJE).status());
    }

    @Test
    void extraiCnhERg() {
        var cnh = extrator.extrairIdentificacao(
                "CARTEIRA NACIONAL DE HABILITAÇÃO\nNome: Joao da Silva\nCPF: 529.982.247-25\nValidade: 10/05/2028");
        assertEquals(new DadosIdentificacao("CNH", "Joao da Silva", CPF, LocalDate.of(2028, 5, 10)), cnh);

        var rg = extrator.extrairIdentificacao("REGISTRO GERAL\nNome: Maria Souza\nCPF: 52998224725");
        assertEquals(new DadosIdentificacao("RG", "Maria Souza", "52998224725", null), rg);
    }
}
