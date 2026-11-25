package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.api.ValidacaoRequest;
import br.com.mentoria.validacao.api.ValidacaoResponse;
import br.com.mentoria.validacao.api.DocumentoIdentificacao;
import br.com.mentoria.validacao.api.RegistroBanco;
import br.com.mentoria.validacao.dominio.DadosGuardados;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import br.com.mentoria.validacao.api.RevisaoRequest;
import br.com.mentoria.validacao.dominio.Cpf;
import br.com.mentoria.validacao.dominio.StatusValidacao;
import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.DadosIdentificacao;
import br.com.mentoria.validacao.dominio.EtapaPipeline;
import br.com.mentoria.validacao.dominio.ResultadoValidacao;
import br.com.mentoria.validacao.persistencia.ValidacaoRegistro;
import br.com.mentoria.validacao.persistencia.ValidacaoRegistroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
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
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final InterpretadorIaService interpretador;
    private final ValidadorComprovante validador;
    private final OcrService ocr;
    private final OpenCvService openCv;
    private final PdfService pdf;
    private final ExtratorTexto extrator;
    private final ValidacaoRegistroRepository registros;

    public ValidacaoDocumentoService(InterpretadorIaService interpretador, ValidadorComprovante validador,
                                     OcrService ocr, OpenCvService openCv, PdfService pdf,
                                     ExtratorTexto extrator, ValidacaoRegistroRepository registros) {
        this.extrator = extrator;
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
        DocumentoIdentificacao docId = request.identificacao();
        Leitura leituraId = docId == null ? null : ler(docId.mimeType(), docId.conteudoDocumento(),
                temTexto(docId.imagemBase64()) ? docId.decodificarImagem() : null);

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
        // RG/CNH tem layout fixo de rotulos: nome, CPF e validade saem por regex, sem enviar o documento a IA
        DadosIdentificacao identificacao = leituraId == null ? null : extrator.extrairIdentificacao(leituraId.texto());

        ResultadoValidacao resultado = validador.validar(dados, leitura.texto().strip().length(), identificacao,
                leituraId == null ? null : leituraId.texto().strip().length(), LocalDate.now());
        var registro = new ValidacaoRegistro(request.nomeArquivo(), resultado, Instant.now());
        registro.guardarDados(JSON.writeValueAsString(new DadosGuardados(dados, identificacao)));
        registros.save(registro); // registro novo: o persist preenche o id na propria instancia

        String documentos = identificacao == null ? request.nomeArquivo()
                : request.nomeArquivo() + " + " + (docId.nomeArquivo() == null ? identificacao.tipo() : docId.nomeArquivo());
        List<EtapaPipeline> etapas = List.of(
                new EtapaPipeline(1, "Documento", "Entrada do arquivo para o pipeline", documentos),
                new EtapaPipeline(2, "OpenCV", "Pre-processamento da imagem", juntar(leitura.openCv(), leituraId, Leitura::openCv)),
                new EtapaPipeline(3, "Tesseract", "Extracao do texto", juntar(leitura.ocr(), leituraId, Leitura::ocr)),
                new EtapaPipeline(4, "IA", "Interpretacao do texto em dados estruturados",
                        leituraId == null ? resultadoIa : resultadoIa + " | " + identificacao.tipo() + ": extracao local por regex"),
                new EtapaPipeline(5, "Regras", "Aplicacao das regras de negocio", resultado.status().name()),
                new EtapaPipeline(6, "Banco", "Resultado salvo para consulta",
                        registro.getId() == null ? "Nao salvo" : "Registro " + registro.getId() + " salvo"));

        return new ValidacaoResponse(registro.getId(), request.nomeArquivo(), etapas, dados, identificacao, resultado);
    }

    private static String juntar(String comprovante, Leitura id, java.util.function.Function<Leitura, String> campo) {
        return id == null || comprovante.equals(campo.apply(id)) ? comprovante : "Comprovante: " + comprovante + " | Identificacao: " + campo.apply(id);
    }

    public Optional<ValidacaoRegistro> buscar(Long id) {
        return registros.findById(id);
    }

    /**
     * Registra a decisao do analista. Vazio se o id nao existe; IllegalStateException se o documento
     * nao esta em revisao manual. O CPF digitado no motivo e mascarado antes de salvar.
     */
    @Transactional
    public Optional<ValidacaoRegistro> revisar(Long id, RevisaoRequest revisao) {
        return registros.findById(id).map(registro -> {
            registro.revisar(StatusValidacao.valueOf(revisao.decisao()), Cpf.mascarar(revisao.motivo().strip()), Instant.now());
            return registros.save(registro);
        });
    }

    /** Todos os registros com os dados extraidos abertos, do mais recente ao mais antigo (tela Banco de Dados). */
    public List<RegistroBanco> listarBanco() {
        return registros.findAllByOrderByCriadoEmDescIdDesc().stream().map(ValidacaoDocumentoService::paraBanco).toList();
    }

    private static RegistroBanco paraBanco(ValidacaoRegistro r) {
        DadosGuardados d = lerDados(r.dadosExtraidos());
        return new RegistroBanco(r.getId(), r.getNomeArquivo(), r.getStatus(), r.isAprovado(), r.getPendencias(),
                r.getCriadoEm(), r.isRevisadoManualmente(), r.getMotivoRevisao(), r.getRevisadoEm(),
                d.comprovante(), d.identificacao());
    }

    private static DadosGuardados lerDados(String json) {
        if (json == null) {
            return new DadosGuardados(null, null); // registros gravados antes de os dados serem guardados
        }
        try {
            return JSON.readValue(json, DadosGuardados.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** As 20 validacoes mais recentes, para o historico da tela. */
    public List<ValidacaoRegistro> listarRecentes() {
        return registros.findTop20ByOrderByCriadoEmDescIdDesc();
    }

    /**
     * PDF com texto dispensa OCR; PDF escaneado vira uma imagem por pagina (RG costuma ter frente e verso)
     * e cada pagina passa por OpenCV e Tesseract; sem OCR, usa o texto enviado.
     */
    Leitura ler(String mimeType, String conteudoDocumento, byte[] arquivo) {
        String conteudo = conteudoDocumento == null ? "" : conteudoDocumento;
        String semOcr = "OCR nao configurado: usando conteudoDocumento (" + conteudo.length() + " caracteres)";
        if (arquivo == null) {
            return new Leitura(conteudo, "Nao executado", semOcr);
        }
        List<byte[]> imagens = List.of(arquivo);
        if ("application/pdf".equals(mimeType)) {
            try {
                String textoPdf = pdf.extrairTexto(arquivo);
                if (!textoPdf.isBlank()) {
                    return new Leitura(textoPdf, "Nao necessario: PDF com texto",
                            "Texto lido direto do PDF (" + textoPdf.length() + " caracteres), sem OCR");
                }
                imagens = pdf.paginasComoImagem(arquivo);
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
        StringBuilder texto = new StringBuilder();
        try {
            for (byte[] pagina : imagens) {
                byte[] imagem = pagina;
                if (openCv.configurado()) {
                    try {
                        imagem = openCv.preprocessar(pagina);
                        resultadoOpenCv = "Imagem tratada pelo OpenCV";
                    } catch (IOException | RuntimeException e) {
                        falha("OpenCV", e);
                        resultadoOpenCv = "Falha no OpenCV: usando a imagem original";
                    }
                }
                texto.append(ocr.extrairTexto(imagem)).append('\n');
            }
            String lido = texto.toString();
            String paginas = imagens.size() > 1 ? " (" + imagens.size() + " paginas)" : "";
            return new Leitura(lido, resultadoOpenCv, lido.strip().length() + " caracteres extraidos da imagem" + paginas);
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
