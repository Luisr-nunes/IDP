package br.com.mentoria.validacao.api;

import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.DadosIdentificacao;
import br.com.mentoria.validacao.dominio.StatusValidacao;

import java.time.Instant;
import java.util.List;

/** Linha da tela Banco de Dados e do JSON de exportacao: resultado da validacao mais os dados extraidos. */
public record RegistroBanco(
        Long id,
        String nomeArquivo,
        StatusValidacao status,
        boolean aprovado,
        List<String> pendencias,
        Instant criadoEm,
        boolean revisadoManualmente,
        String motivoRevisao,
        Instant revisadoEm,
        DadosComprovante comprovante,
        DadosIdentificacao identificacao
) {
}
