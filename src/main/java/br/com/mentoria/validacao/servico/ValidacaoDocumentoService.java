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
        boolean temImagem = request.imagemBase64() != null && !request.imagemBase64().isBlank();
        boolean usarOcr = temImagem && ocr.configurado();
        boolean usarOpenCv = usarOcr && openCv.configurado();
        String texto = usarOcr
                ? ocr.extrairTexto(openCv.preprocessar(Base64.getMimeDecoder().decode(request.imagemBase64())))
                : (request.conteudoDocumento() == null ? "" : request.conteudoDocumento());
        DadosComprovante dados = interpretador.interpretar(texto);
        ResultadoValidacao resultado = validador.validar(dados, LocalDate.now());

        String resultadoOcr = usarOcr
                ? texto.length() + " caracteres extraidos da imagem"
                : "OCR nao configurado: usando conteudoDocumento (" + texto.length() + " caracteres)";
        List<EtapaPipeline> etapas = List.of(
                new EtapaPipeline(1, "Documento", "Entrada do arquivo para o pipeline", request.nomeArquivo()),
                new EtapaPipeline(2, "OpenCV", "Pre-processamento da imagem",
                        usarOpenCv ? "Imagem tratada pelo OpenCV" : "Nao executado"),
                new EtapaPipeline(3, "Tesseract", "Extracao do texto", resultadoOcr),
                new EtapaPipeline(4, "IA", "Interpretacao do texto em dados estruturados",
                        interpretador.configurado() ? "Interpretado pela OpenAI"
                                : "Chave da IA nao configurada: extracao local por regex"),
                new EtapaPipeline(5, "Regras", "Aplicacao das regras de negocio", resultado.status().name()));

        return new ValidacaoResponse(request.nomeArquivo(), etapas, dados, resultado);
    }
}
