package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.api.ValidacaoRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Nao depende do Tesseract instalado: cobre a configuracao, o fallback e a limpeza do arquivo temporario. */
class OcrServiceTest {

    @Test
    void soEstaConfiguradoComTessdataPath() {
        assertFalse(new OcrService("tesseract", "", "por").configurado());
        assertFalse(new OcrService("tesseract", null, "por").configurado());
        assertTrue(new OcrService("tesseract", "C:/tesseract/tessdata", "por").configurado());
    }

    @Test
    void semOcrConfiguradoUsaConteudoDocumentoMesmoComImagem() throws IOException {
        var service = new ValidacaoDocumentoService(new ExtratorTexto(), new ValidadorComprovante(90),
                new OcrService("tesseract", "", "por"));
        String imagem = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});

        var resposta = service.validar(new ValidacaoRequest("doc.png", "image/png", "Nome: Maria Souza", imagem));

        assertEquals("Maria Souza", resposta.dadosExtraidos().nome());
        assertTrue(resposta.etapas().get(2).resultado().startsWith("OCR nao configurado"));
    }

    @Test
    void falhaDoTesseractViraErroEApagaOArquivoTemporario() throws IOException {
        var ocr = new OcrService("comando-inexistente-idp", "C:/tessdata", "por");
        long antes = arquivosTemporariosDoOcr();

        assertThrows(IOException.class, () -> ocr.extrairTexto(new byte[]{1, 2, 3}));

        assertEquals(antes, arquivosTemporariosDoOcr());
    }

    private static long arquivosTemporariosDoOcr() throws IOException {
        try (Stream<Path> arquivos = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return arquivos.filter(p -> p.getFileName().toString().startsWith("idp-ocr-")).count();
        }
    }
}
