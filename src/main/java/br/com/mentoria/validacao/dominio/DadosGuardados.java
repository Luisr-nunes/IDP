package br.com.mentoria.validacao.dominio;

/** O que foi extraido dos documentos, gravado cifrado junto com o resultado da validacao. */
public record DadosGuardados(
        DadosComprovante comprovante,
        DadosIdentificacao identificacao
) {
}
