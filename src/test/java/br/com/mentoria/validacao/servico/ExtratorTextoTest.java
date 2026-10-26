package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.dominio.DadosComprovante;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExtratorTextoTest {

    private final ExtratorTexto extrator = new ExtratorTexto();

    @Test
    void extraiTodosOsCamposDoExemploDoReadme() {
        String texto = "COMPROVANTE DE RESIDENCIA\nNome: Joao da Silva\nCPF: 123.456.789-00\n"
                + "Endereco: Rua das Flores, 150\nCidade: Guanambi - BA\nData de emissao: 10/08/2026";

        assertEquals(new DadosComprovante("Joao da Silva", "123.456.789-00", "Rua das Flores, 150",
                "Guanambi - BA", LocalDate.of(2026, 8, 10)), extrator.extrair(texto));
    }

    @Test
    void aceitaRotulosComAcentoEmMaiusculasECpfSemMascara() {
        String texto = "NOME:  Maria Souza \r\nCPF 52998224725\r\nENDEREÇO: Av. Boa Viagem, 10\r\n"
                + "CIDADE: Recife - PE\r\nDATA DE EMISSÃO: 01/10/2026";

        DadosComprovante dados = extrator.extrair(texto);

        assertEquals("Maria Souza", dados.nome());
        assertEquals("52998224725", dados.cpf());
        assertEquals("Av. Boa Viagem, 10", dados.endereco());
        assertEquals("Recife - PE", dados.cidade());
        assertEquals(LocalDate.of(2026, 10, 1), dados.dataEmissao());
    }

    @Test
    void retornaNuloParaCamposAusentesOuDataInvalida() {
        DadosComprovante dados = extrator.extrair("Nome: Joao\nData de emissao: 31/02/2026");

        assertNull(dados.cpf());
        assertNull(dados.endereco());
        assertNull(dados.cidade());
        assertNull(dados.dataEmissao());
    }

    @Test
    void textoNuloNaoQuebra() {
        assertEquals(new DadosComprovante(null, null, null, null, null), extrator.extrair(null));
    }
}
