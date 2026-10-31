package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.api.ValidacaoRequest;
import br.com.mentoria.validacao.api.ValidacaoResponse;
import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.EtapaPipeline;
import br.com.mentoria.validacao.dominio.ResultadoValidacao;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

/** Orquestra o pipeline: documento, pre-processamento, OCR, interpretacao e regras. */
@Service
public class ValidacaoDocumentoService {

    private final ExtratorTexto extrator;
    private final ValidadorComprovante validador;
    private final OcrService ocr;

    public ValidacaoDocumentoService(ExtratorTexto extrator, ValidadorComprovante validador, OcrService ocr) {
        this.extrator = extrator;
        this.validador = validador;
        this.ocr = ocr;
    }

    public ValidacaoResponse validar(ValidacaoRequest request) throws IOException {
        boolean temImagem = request.imagemBase64() != null && !request.imagemBase64().isBlank();
        boolean usarOcr = temImagem && ocr.configurado();
        String texto = usarOcr
                ? ocr.extrairTexto(Base64.getMimeDecoder().decode(request.imagemBase64()))
                : (request.conteudoDocumento() == null ? "" : request.conteudoDocumento());
        DadosComprovante dados = extrator.extrair(texto);
        ResultadoValidacao resultado = validador.validar(dados, LocalDate.now());

        String resultadoOcr = usarOcr
                ? texto.length() + " caracteres extraidos da imagem"
                : "OCR nao configurado: usando conteudoDocumento (" + texto.length() + " caracteres)";
        List<EtapaPipeline> etapas = List.of(
                new EtapaPipeline(1, "Documento", "Entrada do arquivo para o pipeline", request.nomeArquivo()),
                new EtapaPipeline(2, "OpenCV", "Pre-processamento da imagem", "Nao executado"),
                new EtapaPipeline(3, "Tesseract", "Extracao do texto", resultadoOcr),
                new EtapaPipeline(4, "IA", "Interpretacao do texto em dados estruturados", "Extracao local por regex"),
                new EtapaPipeline(5, "Regras", "Aplicacao das regras de negocio", resultado.status().name()));

        return new ValidacaoResponse(request.nomeArquivo(), etapas, dados, resultado);
    }
}
