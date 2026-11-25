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
        String texto = "COMPROVANTE DE RESIDENCIA\nNome: Joao da Silva\nCPF: 529.982.247-25\n"
                + "Endereco: Rua das Flores, 150\nCidade: Guanambi - BA\nData de emissao: 10/08/2026";

        assertEquals(new DadosComprovante("Joao da Silva", "529.982.247-25", "Rua das Flores, 150",
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

    /** Layout de boleto real: rotulos depois dos valores, nome na linha do CPF, endereco na linha do CEP. */
    @Test
    void extraiBoletoSemRotulosNoFormatoNomeDoisPontos() {
        String texto = """
                JOAO DA SILVA (2025200183) 01.203.327/0001-23 274706 16/11/2099
                Responsavel: CNPJ da Instituicao: Boleto: Vencimento:
                Pagto ate o dia 16/11/2099......... R$ 1253,94
                 Pagador:
                Codigo de Baixa:
                RUA DAS FLORES, 150  - PIEDADE - JABOATAO DOS GUARARAPES/PE   CEP: 54420-170
                Pagador/Avalista
                JOAO DA SILVA - CPF/CNPJ: 529.982.247-25
                05/11/2026 274706 DS 05/11/2026N
                """;

        assertEquals(new DadosComprovante("JOAO DA SILVA", "529.982.247-25", "RUA DAS FLORES, 150 - PIEDADE",
                "JABOATAO DOS GUARARAPES/PE", LocalDate.of(2026, 11, 5), "54420-170"), extrator.extrair(texto));
    }

    @Test
    void usaRotuloDeEmissaoAlternativoERuaSemCep() {
        String texto = "Cliente: Maria Souza\nAv. Boa Viagem, 10\nMunicipio: Recife - PE\nData do documento: 01/10/2026";

        DadosComprovante dados = extrator.extrair(texto);

        assertEquals("Maria Souza", dados.nome());
        assertEquals("Av. Boa Viagem, 10", dados.endereco());
        assertEquals("Recife - PE", dados.cidade());
        assertEquals(LocalDate.of(2026, 10, 1), dados.dataEmissao());
    }
}
