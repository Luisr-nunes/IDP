package br.com.mentoria.validacao.dominio;

import java.util.regex.Pattern;

public final class Cpf {

    private static final Pattern NO_TEXTO = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");

    private Cpf() {
    }

    /** Troca cada CPF do texto por ***.***.***-** antes de o texto sair da aplicacao (por exemplo, para a IA). */
    public static String mascarar(String texto) {
        return texto == null ? null : NO_TEXTO.matcher(texto).replaceAll("***.***.***-**");
    }

    public static boolean digitosValidos(String cpf) {
        if (cpf == null) {
            return false;
        }
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    private static int digito(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }
}
