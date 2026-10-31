package br.com.mentoria.validacao.servico;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Extrai texto de uma imagem chamando o executavel do Tesseract como processo externo. */
@Service
public class OcrService {

    private static final long TIMEOUT_SEGUNDOS = 30;

    private final String comando;
    private final String tessdataPath;
    private final String idioma;

    public OcrService(@Value("${app.ocr.command:tesseract}") String comando,
                      @Value("${app.ocr.tessdata-path:}") String tessdataPath,
                      @Value("${app.ocr.language:por}") String idioma) {
        this.comando = comando;
        this.tessdataPath = tessdataPath;
        this.idioma = idioma;
    }

    public boolean configurado() {
        return tessdataPath != null && !tessdataPath.isBlank();
    }

    /** Grava a imagem num arquivo temporario so durante o OCR; o arquivo e apagado mesmo se der erro. */
    public String extrairTexto(byte[] imagem) throws IOException {
        Path arquivo = Files.createTempFile("idp-ocr-", ".img");
        try {
            Files.write(arquivo, imagem);
            return extrairTexto(arquivo);
        } finally {
            Files.deleteIfExists(arquivo);
        }
    }

    /** Executa: tesseract IMAGEM stdout -l IDIOMA --tessdata-dir TESSDATA. */
    public String extrairTexto(Path imagem) throws IOException {
        // argumentos em lista: nada e montado por concatenacao, entao nao ha injecao de comando
        List<String> comandoCompleto = List.of(comando, imagem.toString(), "stdout",
                "-l", idioma, "--tessdata-dir", tessdataPath);
        Process processo = new ProcessBuilder(comandoCompleto)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        // le a saida em paralelo para o processo nao travar com o buffer cheio
        CompletableFuture<byte[]> saida = CompletableFuture.supplyAsync(() -> {
            try {
                return processo.getInputStream().readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        try {
            if (!processo.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)) {
                processo.destroyForcibly();
                throw new IOException("Tesseract excedeu o tempo limite de " + TIMEOUT_SEGUNDOS + " segundos");
            }
        } catch (InterruptedException e) {
            processo.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Execucao do Tesseract interrompida", e);
        }
        if (processo.exitValue() != 0) {
            throw new IOException("Tesseract terminou com codigo " + processo.exitValue());
        }
        return new String(saida.join(), StandardCharsets.UTF_8);
    }
}
