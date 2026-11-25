package br.com.mentoria.validacao.dominio;

import java.time.LocalDate;

/** Dados do documento de identificacao (RG ou CNH). Validade so existe na CNH. */
public record DadosIdentificacao(
        String tipo,
        String nome,
        String cpf,
        LocalDate validade
) {
}
