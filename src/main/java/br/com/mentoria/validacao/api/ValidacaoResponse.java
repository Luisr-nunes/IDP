package br.com.mentoria.validacao.api;

import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.EtapaPipeline;
import br.com.mentoria.validacao.dominio.ResultadoValidacao;

import java.util.List;

public record ValidacaoResponse(
        String nomeArquivo,
        List<EtapaPipeline> etapas,
        DadosComprovante dadosExtraidos,
        ResultadoValidacao resultado
) {
}
