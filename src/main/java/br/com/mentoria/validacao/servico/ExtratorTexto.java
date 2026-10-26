package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.dominio.DadosComprovante;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extrai os campos do texto do OCR por palavra-chave e regex, sem usar IA. */
@Component
public class ExtratorTexto {

    private static final Pattern NOME = rotulo("nome");
    private static final Pattern ENDERECO = rotulo("endere[c\\u00e7]o");
    private static final Pattern CIDADE = rotulo("cidade");
    private static final Pattern DATA_EMISSAO = rotulo("data de emiss[a\\u00e3]o");
    private static final DateTimeFormatter DD_MM_AAAA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");

    public DadosComprovante extrair(String texto) {
        String t = texto == null ? "" : texto;
        return new DadosComprovante(valor(NOME, t), cpf(t), valor(ENDERECO, t), valor(CIDADE, t), data(valor(DATA_EMISSAO, t)));
    }

    static Pattern rotulo(String regexRotulo) {
        return Pattern.compile("(?imu)^\\s*" + regexRotulo + "\\s*:\\s*(.+?)\\s*$");
    }

    static String valor(Pattern p, String texto) {
        Matcher m = p.matcher(texto);
        return m.find() ? m.group(1) : null;
    }

    private static LocalDate data(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return LocalDate.parse(valor, DD_MM_AAAA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String cpf(String texto) {
        Matcher m = CPF.matcher(texto);
        return m.find() ? m.group() : null;
    }
}
