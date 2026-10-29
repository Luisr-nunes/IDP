package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.api.ValidacaoRequest;
import br.com.mentoria.validacao.api.ValidacaoResponse;
import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.EtapaPipeline;
import br.com.mentoria.validacao.dominio.ResultadoValidacao;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/** Orquestra o pipeline: documento, pre-processamento, OCR, interpretacao e regras. */
@Service
public class ValidacaoDocumentoService {

    private final ExtratorTexto extrator;
    private final ValidadorComprovante validador;

    public ValidacaoDocumentoService(ExtratorTexto extrator, ValidadorComprovante validador) {
        this.extrator = extrator;
        this.validador = validador;
    }

    public ValidacaoResponse validar(ValidacaoRequest request) {
        String texto = request.conteudoDocumento() == null ? "" : request.conteudoDocumento();
        DadosComprovante dados = extrator.extrair(texto);
        ResultadoValidacao resultado = validador.validar(dados, LocalDate.now());

        List<EtapaPipeline> etapas = List.of(
                new EtapaPipeline(1, "Documento", "Entrada do arquivo para o pipeline", request.nomeArquivo()),
                new EtapaPipeline(2, "OpenCV", "Pre-processamento da imagem", "Nao executado"),
                new EtapaPipeline(3, "Tesseract", "Extracao do texto", texto.length() + " caracteres"),
                new EtapaPipeline(4, "IA", "Interpretacao do texto em dados estruturados", "Extracao local por regex"),
                new EtapaPipeline(5, "Regras", "Aplicacao das regras de negocio", resultado.status().name()));

        return new ValidacaoResponse(request.nomeArquivo(), etapas, dados, resultado);
    }
}
