package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.api.ValidacaoRequest;
import br.com.mentoria.validacao.api.ValidacaoResponse;
import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.EtapaPipeline;
import br.com.mentoria.validacao.dominio.ResultadoValidacao;
import br.com.mentoria.validacao.persistencia.ValidacaoRegistro;
import br.com.mentoria.validacao.persistencia.ValidacaoRegistroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Orquestra o pipeline: documento, leitura do PDF, pre-processamento, OCR, interpretacao e regras.
 * Se OpenCV, Tesseract ou IA falharem (erro ou timeout), a etapa cai no fallback local em vez de derrubar a requisicao.
 */
@Service
public class ValidacaoDocumentoService {

    private static final Logger log = LoggerFactory.getLogger(ValidacaoDocumentoService.class);

    private final InterpretadorIaService interpretador;
    private final ValidadorComprovante validador;
    private final OcrService ocr;
    private final OpenCvService openCv;
    private final PdfService pdf;
    private final ValidacaoRegistroRepository registros;

    public ValidacaoDocumentoService(InterpretadorIaService interpretador, ValidadorComprovante validador,
                                     OcrService ocr, OpenCvService openCv, PdfService pdf,
                                     ValidacaoRegistroRepository registros) {
        this.interpretador = interpretador;
        this.validador = validador;
        this.ocr = ocr;
        this.openCv = openCv;
        this.pdf = pdf;
        this.registros = registros;
    }

    /** Texto lido de um documento e o que aconteceu nas etapas OpenCV e Tesseract. */
    record Leitura(String texto, String openCv, String ocr) {
    }

    public ValidacaoResponse validar(ValidacaoRequest request) throws IOException {
        Leitura leitura = ler(request.mimeType(), request.conteudoDocumento(),
                temTexto(request.imagemBase64()) ? request.decodificarImagem() : null);

        DadosComprovante dados;
        String resultadoIa = "Chave da IA nao configurada: extracao local por regex";
        if (interpretador.configurado()) {
            try {
                dados = interpretador.interpretar(leitura.texto());
                resultadoIa = "Interpretado pela OpenAI";
            } catch (IOException | RuntimeException e) {
                falha("IA", e);
                dados = interpretador.extrairLocal(leitura.texto());
                resultadoIa = "Falha na IA: extracao local por regex";
            }
        } else {
            dados = interpretador.extrairLocal(leitura.texto());
        }
        ResultadoValidacao resultado = validador.validar(dados, LocalDate.now());
        var registro = new ValidacaoRegistro(request.nomeArquivo(), resultado, Instant.now());
        registros.save(registro); // registro novo: o persist preenche o id na propria instancia

        List<EtapaPipeline> etapas = List.of(
                new EtapaPipeline(1, "Documento", "Entrada do arquivo para o pipeline", request.nomeArquivo()),
                new EtapaPipeline(2, "OpenCV", "Pre-processamento da imagem", leitura.openCv()),
                new EtapaPipeline(3, "Tesseract", "Extracao do texto", leitura.ocr()),
                new EtapaPipeline(4, "IA", "Interpretacao do texto em dados estruturados", resultadoIa),
                new EtapaPipeline(5, "Regras", "Aplicacao das regras de negocio", resultado.status().name()));

        return new ValidacaoResponse(registro.getId(), request.nomeArquivo(), etapas, dados, resultado);
    }

    public Optional<ValidacaoRegistro> buscar(Long id) {
        return registros.findById(id);
    }

    /** PDF com texto dispensa OCR; PDF escaneado e imagem passam por OpenCV e Tesseract; sem OCR, usa o texto enviado. */
    Leitura ler(String mimeType, String conteudoDocumento, byte[] arquivo) {
        String conteudo = conteudoDocumento == null ? "" : conteudoDocumento;
        String semOcr = "OCR nao configurado: usando conteudoDocumento (" + conteudo.length() + " caracteres)";
        if (arquivo == null) {
            return new Leitura(conteudo, "Nao executado", semOcr);
        }
        byte[] imagem = arquivo;
        if ("application/pdf".equals(mimeType)) {
            try {
                String textoPdf = pdf.extrairTexto(arquivo);
                if (!textoPdf.isBlank()) {
                    return new Leitura(textoPdf, "Nao necessario: PDF com texto",
                            "Texto lido direto do PDF (" + textoPdf.length() + " caracteres), sem OCR");
                }
                imagem = pdf.primeiraPaginaComoImagem(arquivo);
            } catch (IOException | RuntimeException e) {
                falha("PDF", e);
                return new Leitura(conteudo, "Nao executado",
                        "PDF invalido: usando conteudoDocumento (" + conteudo.length() + " caracteres)");
            }
        }
        if (!ocr.configurado()) {
            return new Leitura(conteudo, "Nao executado", semOcr);
        }
        String resultadoOpenCv = "Nao executado";
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
            String texto = ocr.extrairTexto(imagem);
            return new Leitura(texto, resultadoOpenCv, texto.length() + " caracteres extraidos da imagem");
        } catch (IOException | RuntimeException e) {
            falha("Tesseract", e);
            return new Leitura(conteudo, resultadoOpenCv,
                    "Falha no OCR: usando conteudoDocumento (" + conteudo.length() + " caracteres)");
        }
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }

    private static void falha(String etapa, Exception e) {
        // so o tipo da excecao: a mensagem pode conter trechos do documento
        log.warn("Falha na etapa {} ({}); usando fallback local", etapa, e.getClass().getSimpleName());
    }
}
