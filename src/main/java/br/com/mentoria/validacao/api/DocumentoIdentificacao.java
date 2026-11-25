package br.com.mentoria.validacao.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** RG ou CNH enviado junto com o comprovante, para a validacao cruzada de nome e CPF. */
public record DocumentoIdentificacao(
        String nomeArquivo,
        @NotBlank(message = "Informe o mimeType da identificacao")
        @Pattern(regexp = "application/pdf|image/png|image/jpeg",
                message = "mimeType da identificacao deve ser application/pdf, image/png ou image/jpeg")
        String mimeType,
        String conteudoDocumento,
        String imagemBase64
) {

    @JsonIgnore
    @AssertTrue(message = "Informe conteudoDocumento ou imagemBase64 da identificacao")
    public boolean isConteudoOuImagemInformado() {
        return ValidacaoRequest.informado(conteudoDocumento) || ValidacaoRequest.informado(imagemBase64);
    }

    @JsonIgnore
    @AssertTrue(message = "imagemBase64 da identificacao excede o limite de 5 MB")
    public boolean isImagemDentroDoLimite() {
        return imagemBase64 == null || imagemBase64.length() <= ValidacaoRequest.LIMITE_IMAGEM_BASE64;
    }

    @JsonIgnore
    @AssertTrue(message = "imagemBase64 da identificacao nao e um Base64 valido")
    public boolean isImagemBase64Valida() {
        return !isImagemDentroDoLimite() || ValidacaoRequest.base64Valido(imagemBase64);
    }

    public byte[] decodificarImagem() {
        return ValidacaoRequest.decodificar(imagemBase64);
    }
}
