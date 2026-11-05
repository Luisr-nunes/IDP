package br.com.mentoria.validacao.servico;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Pre-processa a imagem com OpenCV chamando um comando externo que recebe arquivoEntrada arquivoSaida. */
@Service
public class OpenCvService {

    private static final long TIMEOUT_SEGUNDOS = 30;

    private final String comando;

    public OpenCvService(@Value("${app.opencv.command:}") String comando) {
        this.comando = comando;
    }

    public boolean configurado() {
        return comando != null && !comando.isBlank();
    }

    /** Devolve a imagem tratada; sem comando configurado, devolve a imagem original. */
    public byte[] preprocessar(byte[] imagem) throws IOException {
        if (!configurado()) {
            return imagem;
        }
        Path entrada = Files.createTempFile("idp-opencv-entrada-", ".img");
        Path saida = Files.createTempFile("idp-opencv-saida-", ".png");
        try {
            Files.write(entrada, imagem);
            executar(entrada, saida);
            return Files.readAllBytes(saida);
        } finally {
            Files.deleteIfExists(entrada);
            Files.deleteIfExists(saida);
        }
    }

    private void executar(Path entrada, Path saida) throws IOException {
        // o comando vem da configuracao; os caminhos entram como argumentos separados, sem concatenacao
        List<String> comandoCompleto = new ArrayList<>(Arrays.asList(comando.trim().split("\\s+")));
        comandoCompleto.add(entrada.toString());
        comandoCompleto.add(saida.toString());
        Process processo = new ProcessBuilder(comandoCompleto)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        try {
            if (!processo.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)) {
                processo.destroyForcibly();
                throw new IOException("OpenCV excedeu o tempo limite de " + TIMEOUT_SEGUNDOS + " segundos");
            }
        } catch (InterruptedException e) {
            processo.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Execucao do OpenCV interrompida", e);
        }
        if (processo.exitValue() != 0) {
            throw new IOException("OpenCV terminou com codigo " + processo.exitValue());
        }
    }
}
