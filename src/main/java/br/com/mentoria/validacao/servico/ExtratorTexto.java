package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.dominio.DadosComprovante;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extrai os campos do texto do OCR por palavra-chave e regex, sem usar IA. */
@Component
public class ExtratorTexto {

    private static final Pattern NOME = rotulo("nome");
    private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");

    public DadosComprovante extrair(String texto) {
        String t = texto == null ? "" : texto;
        return new DadosComprovante(valor(NOME, t), cpf(t), null, null, null);
    }

    static Pattern rotulo(String regexRotulo) {
        return Pattern.compile("(?im)^\\s*" + regexRotulo + "\\s*:\\s*(.+?)\\s*$");
    }

    static String valor(Pattern p, String texto) {
        Matcher m = p.matcher(texto);
        return m.find() ? m.group(1) : null;
    }

    private static String cpf(String texto) {
        Matcher m = CPF.matcher(texto);
        return m.find() ? m.group() : null;
    }
}
