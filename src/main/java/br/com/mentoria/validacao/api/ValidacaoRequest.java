package br.com.mentoria.validacao.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Base64;

/** Comprovante de residencia (campos do professor) e, opcionalmente, o RG ou a CNH do mesmo titular. */
public record ValidacaoRequest(
        String nomeArquivo,
        @NotBlank(message = "Informe o mimeType")
        @Pattern(regexp = "application/pdf|image/png|image/jpeg",
                message = "mimeType deve ser application/pdf, image/png ou image/jpeg")
        String mimeType,
        String conteudoDocumento,
        String imagemBase64,
        @Valid DocumentoIdentificacao identificacao
) {

    /** 5 MB de arquivo equivalem a cerca de 7 milhoes de caracteres em Base64. */
    static final int LIMITE_IMAGEM_BASE64 = 5 * 1024 * 1024 / 3 * 4 + 4;

    public ValidacaoRequest(String nomeArquivo, String mimeType, String conteudoDocumento, String imagemBase64) {
        this(nomeArquivo, mimeType, conteudoDocumento, imagemBase64, null);
    }

    @JsonIgnore
    @AssertTrue(message = "Informe conteudoDocumento ou imagemBase64")
    public boolean isConteudoOuImagemInformado() {
        return informado(conteudoDocumento) || informado(imagemBase64);
    }

    @JsonIgnore
    @AssertTrue(message = "imagemBase64 excede o limite de 5 MB")
    public boolean isImagemDentroDoLimite() {
        return imagemBase64 == null || imagemBase64.length() <= LIMITE_IMAGEM_BASE64;
    }

    @JsonIgnore
    @AssertTrue(message = "imagemBase64 nao e um Base64 valido")
    public boolean isImagemBase64Valida() {
        return !isImagemDentroDoLimite() || base64Valido(imagemBase64);
    }

    public byte[] decodificarImagem() {
        return decodificar(imagemBase64);
    }

    static boolean informado(String valor) {
        return valor != null && !valor.isBlank();
    }

    static boolean base64Valido(String valor) {
        if (!informado(valor)) {
            return true;
        }
        try {
            decodificar(valor);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Decodificador estrito: ignora so quebras de linha e espacos, rejeita qualquer outro caractere. */
    static byte[] decodificar(String valor) {
        return Base64.getDecoder().decode(valor.replaceAll("\\s", ""));
    }
}
