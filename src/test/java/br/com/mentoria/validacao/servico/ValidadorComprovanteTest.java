package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.StatusValidacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorComprovanteTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);
    private static final String CPF = "123.456.789-00";
    private final ValidadorComprovante validador = new ValidadorComprovante(90);

    @Test
    void aprovaComprovanteCompletoERecente() {
        var dados = new DadosComprovante("Joao da Silva", CPF, "Rua das Flores, 150", "Recife", HOJE.minusDays(10));
        var resultado = validador.validar(dados, HOJE);
        assertEquals(StatusValidacao.APROVADO, resultado.status());
        assertTrue(resultado.aprovado());
        assertEquals(java.util.List.of("Nome identificado", "CPF encontrado", "Endereco encontrado",
                "Cidade encontrada", "Data de emissao reconhecida", "Emissao dentro de 90 dias"), resultado.regrasAtendidas());
    }

    @Test
    void reprovaEmissaoComMaisDe90Dias() {
        var dados = new DadosComprovante("Joao da Silva", CPF, "Rua das Flores, 150", "Recife", HOJE.minusDays(91));
        assertEquals(StatusValidacao.REPROVADO, validador.validar(dados, HOJE).status());
    }

    @Test
    void aceitaEmissaoExatamenteNoLimite() {
        var dados = new DadosComprovante("Joao da Silva", CPF, "Rua das Flores, 150", "Recife", HOJE.minusDays(90));
        assertEquals(StatusValidacao.APROVADO, validador.validar(dados, HOJE).status());
    }

    @Test
    void reprovaSemNomeOuSemData() {
        var dados = new DadosComprovante(" ", CPF, "Rua das Flores, 150", "Recife", null);
        var resultado = validador.validar(dados, HOJE);
        assertEquals(StatusValidacao.REPROVADO, resultado.status());
        assertFalse(resultado.aprovado());
        assertEquals(java.util.List.of("Falhou: Nome identificado", "Falhou: Data de emissao reconhecida",
                "Falhou: Emissao dentro de 90 dias"), resultado.pendencias());
    }

    @Test
    void reprovaSemCpf() {
        var dados = new DadosComprovante("Joao da Silva", null, "Rua das Flores, 150", "Recife", HOJE);
        assertEquals(java.util.List.of("Falhou: CPF encontrado"), validador.validar(dados, HOJE).pendencias());
    }

    @Test
    void reprovaDataNoFuturo() {
        var dados = new DadosComprovante("Joao da Silva", CPF, "Rua das Flores, 150", "Recife", HOJE.plusDays(1));
        assertEquals(StatusValidacao.REPROVADO, validador.validar(dados, HOJE).status());
    }
}
