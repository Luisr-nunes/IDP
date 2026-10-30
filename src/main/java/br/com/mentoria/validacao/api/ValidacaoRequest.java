package br.com.mentoria.validacao.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;

public record ValidacaoRequest(
        String nomeArquivo,
        String mimeType,
        String conteudoDocumento,
        String imagemBase64
) {

    @JsonIgnore
    @AssertTrue(message = "Informe conteudoDocumento ou imagemBase64")
    public boolean isConteudoOuImagemInformado() {
        return (conteudoDocumento != null && !conteudoDocumento.isBlank())
                || (imagemBase64 != null && !imagemBase64.isBlank());
    }
}
