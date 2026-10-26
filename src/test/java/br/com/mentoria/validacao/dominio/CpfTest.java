package br.com.mentoria.validacao.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfTest {

    @Test
    void aceitaCpfComDigitosValidosComOuSemMascara() {
        assertTrue(Cpf.digitosValidos("529.982.247-25"));
        assertTrue(Cpf.digitosValidos("52998224725"));
    }

    @Test
    void rejeitaDigitoVerificadorErrado() {
        assertFalse(Cpf.digitosValidos("529.982.247-24"));
    }

    @Test
    void rejeitaSequenciaRepetidaTamanhoErradoENulo() {
        assertFalse(Cpf.digitosValidos("111.111.111-11"));
        assertFalse(Cpf.digitosValidos("1234567890"));
        assertFalse(Cpf.digitosValidos(null));
    }
}
