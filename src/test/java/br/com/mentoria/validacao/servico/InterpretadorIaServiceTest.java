package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.dominio.Cpf;
import br.com.mentoria.validacao.dominio.DadosComprovante;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Nao chama a OpenAI: cobre o mascaramento do CPF e o fallback por regex sem chave. */
class InterpretadorIaServiceTest {

    private static final String TEXTO = """
            COMPROVANTE DE RESIDENCIA
            Nome: Joao da Silva
            CPF: 123.456.789-00
            Endereco: Rua das Flores, 150
            Cidade: Guanambi - BA
            Data de emissao: 10/08/2026""";

    @Test
    void mascaraCpfComESemPontuacao() {
        assertEquals("CPF: ***.***.***-** e ***.***.***-**", Cpf.mascarar("CPF: 123.456.789-00 e 12345678900"));
        assertFalse(Cpf.mascarar(TEXTO).contains("123.456.789-00"));
        assertTrue(Cpf.mascarar(TEXTO).contains("Nome: Joao da Silva"));
        assertNull(Cpf.mascarar(null));
    }

    @Test
    void soEstaConfiguradoComChave() {
        assertFalse(new InterpretadorIaService("", new ExtratorTexto()).configurado());
        assertFalse(new InterpretadorIaService("  ", new ExtratorTexto()).configurado());
        assertFalse(new InterpretadorIaService(null, new ExtratorTexto()).configurado());
        assertTrue(new InterpretadorIaService("chave-de-teste", new ExtratorTexto()).configurado());
    }

    @Test
    void semChaveUsaExtracaoPorRegex() throws IOException {
        DadosComprovante dados = new InterpretadorIaService("", new ExtratorTexto()).interpretar(TEXTO);

        assertEquals(new DadosComprovante("Joao da Silva", "123.456.789-00", "Rua das Flores, 150",
                "Guanambi - BA", LocalDate.of(2026, 8, 10)), dados);
    }

    @Test
    void semChaveETextoVazioNaoFalha() throws IOException {
        DadosComprovante dados = new InterpretadorIaService("", new ExtratorTexto()).interpretar(null);

        assertNull(dados.nome());
        assertNull(dados.cpf());
    }
}
