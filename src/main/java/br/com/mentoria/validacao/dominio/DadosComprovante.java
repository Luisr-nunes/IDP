package br.com.mentoria.validacao.dominio;

import java.time.LocalDate;

public record DadosComprovante(
        String nome,
        String endereco,
        String cep,
        String cidade,
        LocalDate dataEmissao
) {
}
