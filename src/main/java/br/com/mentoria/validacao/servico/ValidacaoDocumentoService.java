package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.api.ValidacaoRequest;
import br.com.mentoria.validacao.api.ValidacaoResponse;
import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.EtapaPipeline;
import br.com.mentoria.validacao.dominio.ResultadoValidacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

/**
 * Orquestra o pipeline: documento, pre-processamento, OCR, interpretacao e regras.
 * Se OpenCV, Tesseract ou IA falharem (erro ou timeout), a etapa cai no fallback local em vez de derrubar a requisicao.
 */
@Service
public class ValidacaoDocumentoService {

    private static final Logger log = LoggerFactory.getLogger(ValidacaoDocumentoService.class);

    private final InterpretadorIaService interpretador;
    private final ValidadorComprovante validador;
    private final OcrService ocr;
    private final OpenCvService openCv;

    public ValidacaoDocumentoService(InterpretadorIaService interpretador, ValidadorComprovante validador,
                                     OcrService ocr, OpenCvService openCv) {
        this.interpretador = interpretador;
        this.validador = validador;
        this.ocr = ocr;
        this.openCv = openCv;
    }

    public ValidacaoResponse validar(ValidacaoRequest request) throws IOException {
        String conteudo = request.conteudoDocumento() == null ? "" : request.conteudoDocumento();
        boolean temImagem = request.imagemBase64() != null && !request.imagemBase64().isBlank();
        String texto = conteudo;
        String resultadoOpenCv = "Nao executado";
        String resultadoOcr = "OCR nao configurado: usando conteudoDocumento (" + conteudo.length() + " caracteres)";

        if (temImagem && ocr.configurado()) {
            byte[] imagem = Base64.getMimeDecoder().decode(request.imagemBase64());
            if (openCv.configurado()) {
                try {
                    imagem = openCv.preprocessar(imagem);
                    resultadoOpenCv = "Imagem tratada pelo OpenCV";
                } catch (IOException | RuntimeException e) {
                    falha("OpenCV", e);
                    resultadoOpenCv = "Falha no OpenCV: usando a imagem original";
                }
            }
            try {
                texto = ocr.extrairTexto(imagem);
                resultadoOcr = texto.length() + " caracteres extraidos da imagem";
            } catch (IOException | RuntimeException e) {
                falha("Tesseract", e);
                resultadoOcr = "Falha no OCR: usando conteudoDocumento (" + conteudo.length() + " caracteres)";
            }
        }

        DadosComprovante dados;
        String resultadoIa = "Chave da IA nao configurada: extracao local por regex";
        if (interpretador.configurado()) {
            try {
                dados = interpretador.interpretar(texto);
                resultadoIa = "Interpretado pela OpenAI";
            } catch (IOException | RuntimeException e) {
                falha("IA", e);
                dados = interpretador.extrairLocal(texto);
                resultadoIa = "Falha na IA: extracao local por regex";
            }
        } else {
            dados = interpretador.extrairLocal(texto);
        }
        ResultadoValidacao resultado = validador.validar(dados, LocalDate.now());

        List<EtapaPipeline> etapas = List.of(
                new EtapaPipeline(1, "Documento", "Entrada do arquivo para o pipeline", request.nomeArquivo()),
                new EtapaPipeline(2, "OpenCV", "Pre-processamento da imagem", resultadoOpenCv),
                new EtapaPipeline(3, "Tesseract", "Extracao do texto", resultadoOcr),
                new EtapaPipeline(4, "IA", "Interpretacao do texto em dados estruturados", resultadoIa),
                new EtapaPipeline(5, "Regras", "Aplicacao das regras de negocio", resultado.status().name()));

        return new ValidacaoResponse(request.nomeArquivo(), etapas, dados, resultado);
    }

    private static void falha(String etapa, Exception e) {
        // so o tipo da excecao: a mensagem pode conter trechos do documento
        log.warn("Falha na etapa {} ({}); usando fallback local", etapa, e.getClass().getSimpleName());
    }
}
