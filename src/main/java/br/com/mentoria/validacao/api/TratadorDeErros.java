package br.com.mentoria.validacao.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** Respostas de erro padronizadas, sem stack trace, valores enviados ou detalhes internos. */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail requisicaoInvalida(MethodArgumentNotValidException e) {
        List<String> erros = e.getBindingResult().getAllErrors().stream()
                .map(erro -> erro.getDefaultMessage())
                .sorted()
                .toList();
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Requisicao invalida");
        problema.setProperty("erros", erros);
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail jsonInvalido() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "JSON invalido ou mal formatado");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail erroInesperado(Exception e) {
        // so o tipo da excecao: a mensagem pode conter trechos do documento
        log.error("Erro inesperado ao processar requisicao: {}", e.getClass().getName());
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar o documento");
    }
}
