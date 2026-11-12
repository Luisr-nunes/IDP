package br.com.mentoria.validacao.dominio;

import java.text.Normalizer;
import java.util.Locale;

public final class Texto {

    private Texto() {
    }

    /** "  João  da Silva" vira "JOAO DA SILVA": sem acento, em maiusculas e com espacos simples, para comparar. */
    public static String normalizar(String s) {
        if (s == null) {
            return null;
        }
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
