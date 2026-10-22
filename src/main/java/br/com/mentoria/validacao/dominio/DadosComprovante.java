package br.com.mentoria.validacao.dominio;

import java.time.LocalDate;

public record DadosComprovante(
        String nome,
        String cpf,
        String endereco,
        String cidade,
        LocalDate dataEmissao
) {
}
