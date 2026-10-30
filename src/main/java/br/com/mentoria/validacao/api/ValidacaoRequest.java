package br.com.mentoria.validacao.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ValidacaoRequest(
        String nomeArquivo,
        @NotBlank(message = "Informe o mimeType")
        @Pattern(regexp = "application/pdf|image/png|image/jpeg",
                message = "mimeType deve ser application/pdf, image/png ou image/jpeg")
        String mimeType,
        String conteudoDocumento,
        String imagemBase64
) {

    /** 5 MB de arquivo equivalem a cerca de 7 milhoes de caracteres em Base64. */
    static final int LIMITE_IMAGEM_BASE64 = 5 * 1024 * 1024 / 3 * 4 + 4;

    @JsonIgnore
    @AssertTrue(message = "Informe conteudoDocumento ou imagemBase64")
    public boolean isConteudoOuImagemInformado() {
        return (conteudoDocumento != null && !conteudoDocumento.isBlank())
                || (imagemBase64 != null && !imagemBase64.isBlank());
    }

    @JsonIgnore
    @AssertTrue(message = "imagemBase64 excede o limite de 5 MB")
    public boolean isImagemDentroDoLimite() {
        return imagemBase64 == null || imagemBase64.length() <= LIMITE_IMAGEM_BASE64;
    }
}
