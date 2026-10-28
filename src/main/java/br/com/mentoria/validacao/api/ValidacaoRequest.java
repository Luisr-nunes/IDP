package br.com.mentoria.validacao.api;

public record ValidacaoRequest(
        String nomeArquivo,
        String mimeType,
        String conteudoDocumento,
        String imagemBase64
) {
}
