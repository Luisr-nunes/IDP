package br.com.mentoria.validacao.dominio;

public record EtapaPipeline(
        int ordem,
        String nome,
        String descricao,
        String resultado
) {
}
