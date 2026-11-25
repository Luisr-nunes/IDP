package br.com.mentoria.validacao.api;

import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.DadosIdentificacao;
import br.com.mentoria.validacao.dominio.EtapaPipeline;
import br.com.mentoria.validacao.dominio.ResultadoValidacao;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * O id do registro salvo nao entra no JSON: vai no header Location (GET /api/documentos/{id}).
 * identificacao fica null quando so o comprovante foi enviado.
 */
public record ValidacaoResponse(
        @JsonIgnore Long id,
        String nomeArquivo,
        List<EtapaPipeline> etapas,
        DadosComprovante dadosExtraidos,
        DadosIdentificacao identificacao,
        ResultadoValidacao resultado
) {
}
