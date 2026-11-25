package br.com.mentoria.validacao.dominio;

/** O documento nao esta em revisao manual, entao nao aceita decisao do analista (HTTP 409). */
public class RevisaoNaoPermitidaException extends RuntimeException {

    public RevisaoNaoPermitidaException() {
        super("So documentos em revisao manual podem ser revisados");
    }
}
