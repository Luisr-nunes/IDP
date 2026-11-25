package br.com.mentoria.validacao.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM para os dados pessoais gravados no banco. A chave vem de APP_DADOS_CHAVE (Base64, 32 bytes).
 * No perfil dev, sem a variavel, a chave e gerada uma vez em data/chave-dev.key (fora do Git).
 */
@Component
public class Cifra {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int TAMANHO_IV = 12;
    private static volatile Cifra atual;

    private final SecretKeySpec chave;

    public Cifra(@Value("${app.dados.chave:}") String chaveBase64,
                 @Value("${app.dados.arquivo-chave-dev:}") String arquivoChaveDev) {
        this.chave = new SecretKeySpec(carregar(chaveBase64, arquivoChaveDev), "AES");
        atual = this;
    }

    /** Usada pelo conversor JPA, que o Hibernate cria fora do Spring. */
    static Cifra atual() {
        if (atual == null) {
            throw new IllegalStateException("Chave de criptografia dos dados nao configurada");
        }
        return atual;
    }

    public String cifrar(String texto) {
        try {
            byte[] iv = new byte[TAMANHO_IV];
            ALEATORIO.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(128, iv));
            byte[] cifrado = c.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar", e);
        }
    }

    public String decifrar(String base64) {
        try {
            byte[] tudo = Base64.getDecoder().decode(base64);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(128, tudo, 0, TAMANHO_IV));
            return new String(c.doFinal(tudo, TAMANHO_IV, tudo.length - TAMANHO_IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao decifrar (chave diferente da usada para gravar?)", e);
        }
    }

    private static byte[] carregar(String chaveBase64, String arquivoChaveDev) {
        if (chaveBase64 != null && !chaveBase64.isBlank()) {
            return validar(Base64.getDecoder().decode(chaveBase64.strip()));
        }
        if (arquivoChaveDev == null || arquivoChaveDev.isBlank()) {
            throw new IllegalStateException("Defina APP_DADOS_CHAVE (Base64 de 32 bytes) para gravar os dados extraidos");
        }
        try {
            Path arquivo = Path.of(arquivoChaveDev);
            if (Files.notExists(arquivo)) {
                byte[] nova = new byte[32];
                ALEATORIO.nextBytes(nova);
                Files.createDirectories(arquivo.toAbsolutePath().getParent());
                Files.writeString(arquivo, Base64.getEncoder().encodeToString(nova));
            }
            return validar(Base64.getDecoder().decode(Files.readString(arquivo).strip()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] validar(byte[] chave) {
        if (chave.length != 32) {
            throw new IllegalStateException("A chave dos dados precisa ter 32 bytes (AES-256)");
        }
        return chave;
    }
}
