package br.com.mentoria.validacao.servico;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Nao depende de Python nem de OpenCV instalados. */
class OpenCvServiceTest {

    @Test
    void soEstaConfiguradoComComando() {
        assertFalse(new OpenCvService("").configurado());
        assertFalse(new OpenCvService("   ").configurado());
        assertFalse(new OpenCvService(null).configurado());
        assertTrue(new OpenCvService("python scripts/preprocessar_documento.py").configurado());
    }

    @Test
    void semComandoDevolveAImagemOriginal() throws IOException {
        byte[] imagem = {1, 2, 3};
        assertArrayEquals(imagem, new OpenCvService("").preprocessar(imagem));
    }

    @Test
    void falhaDoComandoViraErroEApagaOsArquivosTemporarios() throws IOException {
        var openCv = new OpenCvService("comando-inexistente-idp scripts/preprocessar_documento.py");
        long antes = arquivosTemporariosDoOpenCv();

        assertThrows(IOException.class, () -> openCv.preprocessar(new byte[]{1, 2, 3}));

        assertEquals(antes, arquivosTemporariosDoOpenCv());
    }

    private static long arquivosTemporariosDoOpenCv() throws IOException {
        try (Stream<Path> arquivos = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return arquivos.filter(p -> p.getFileName().toString().startsWith("idp-opencv-")).count();
        }
    }
}
