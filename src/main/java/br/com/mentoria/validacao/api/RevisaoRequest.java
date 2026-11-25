package br.com.mentoria.validacao.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Decisao do analista sobre um documento em revisao manual. */
public record RevisaoRequest(
        @NotBlank(message = "Informe a decisao")
        @Pattern(regexp = "APROVADO|REPROVADO", message = "decisao deve ser APROVADO ou REPROVADO")
        String decisao,
        @NotBlank(message = "Informe o motivo da decisao")
        @Size(max = 500, message = "motivo deve ter no maximo 500 caracteres")
        String motivo
) {
}
