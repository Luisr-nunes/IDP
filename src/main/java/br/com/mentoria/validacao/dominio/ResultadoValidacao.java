package br.com.mentoria.validacao.dominio;

import java.util.List;

public record ResultadoValidacao(
        boolean aprovado,
        StatusValidacao status,
        List<String> regrasAtendidas,
        List<String> pendencias
) {
}
