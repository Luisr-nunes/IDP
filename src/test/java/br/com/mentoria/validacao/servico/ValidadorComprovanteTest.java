package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.StatusValidacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidadorComprovanteTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);
    private final ValidadorComprovante validador = new ValidadorComprovante(90);

    @Test
    void aprovaComprovanteCompletoERecente() {
        var dados = new DadosComprovante("Joao da Silva", null, "Rua das Flores, 150", "Recife", HOJE.minusDays(10));
        assertEquals(StatusValidacao.APROVADO, validador.validar(dados, HOJE).status());
    }

    @Test
    void reprovaEmissaoComMaisDe90Dias() {
        var dados = new DadosComprovante("Joao da Silva", null, "Rua das Flores, 150", null, HOJE.minusDays(91));
        assertEquals(StatusValidacao.REPROVADO, validador.validar(dados, HOJE).status());
    }

    @Test
    void aceitaEmissaoExatamenteNoLimite() {
        var dados = new DadosComprovante("Joao da Silva", null, "Rua das Flores, 150", null, HOJE.minusDays(90));
        assertEquals(StatusValidacao.APROVADO, validador.validar(dados, HOJE).status());
    }

    @Test
    void reprovaSemNomeOuSemData() {
        var dados = new DadosComprovante(" ", null, "Rua das Flores, 150", null, null);
        var resultado = validador.validar(dados, HOJE);
        assertEquals(StatusValidacao.REPROVADO, resultado.status());
        assertEquals(2, resultado.pendencias().size());
    }

    @Test
    void reprovaDataNoFuturo() {
        var dados = new DadosComprovante("Joao da Silva", null, "Rua das Flores, 150", null, HOJE.plusDays(1));
        assertEquals(StatusValidacao.REPROVADO, validador.validar(dados, HOJE).status());
    }
}
