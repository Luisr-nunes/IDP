package br.com.mentoria.validacao.dominio;

import java.util.List;

public record ResultadoValidacao(
        StatusValidacao status,
        List<String> regrasAtendidas,
        List<String> pendencias
) {
}
