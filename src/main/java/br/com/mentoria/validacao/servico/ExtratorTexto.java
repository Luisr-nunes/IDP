package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.DadosIdentificacao;
import br.com.mentoria.validacao.dominio.Texto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Extrai os campos do texto do OCR ou do PDF por palavra-chave e regex, sem usar IA.
 * Primeiro procura "Rotulo: valor"; se nao achar, usa o layout de documentos reais (boletos, contas de consumo):
 * nome na linha do CPF, endereco e cidade na linha do CEP e a data mais antiga do documento como emissao.
 */
@Component
public class ExtratorTexto {

    private static final Pattern NOME = rotulo("nome");
    private static final Pattern NOME_SINONIMO = rotulo(
            "(?:nome\\s+do\\s+(?:cliente|titular|consumidor|pagador)|pagador|sacado|cliente|titular|consumidor)");
    private static final Pattern ENDERECO = rotulo("(?:endere[c\\u00e7]o|logradouro)");
    private static final Pattern CIDADE = rotulo("(?:cidade|munic[i\\u00ed]pio)");
    private static final Pattern DATA_EMISSAO = rotulo("data de emiss[a\\u00e3]o");
    private static final Pattern DATA_EMISSAO_SINONIMO = Pattern.compile(
            "(?iu)(?:emiss[a\\u00e3]o|data\\s+do\\s+documento|data\\s+(?:de\\s+)?processamento)\\s*:?\\s*(\\d{2}/\\d{2}/\\d{4})");
    private static final Pattern VALIDADE = rotulo("validade");
    private static final DateTimeFormatter DD_MM_AAAA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    /** "JOAO DA SILVA - CPF/CNPJ: 529.982.247-25": o nome vem antes do CPF na mesma linha. */
    private static final Pattern NOME_ANTES_DO_CPF = Pattern.compile(
            "(?imu)^\\s*([\\p{L}][\\p{L} .'-]+?)\\s*[-\\u2013|,]?\\s*CPF(?:\\s*/\\s*CNPJ)?\\s*:?\\s*\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}");
    private static final Pattern CEP = Pattern.compile("(?i)\\bCEP\\s*:?\\s*(\\d{5}-?\\d{3})\\b");
    private static final Pattern LINHA_DO_CEP = Pattern.compile("(?imu)^(.*?)[\\s,-]*\\bCEP\\s*:?\\s*\\d{5}-?\\d{3}.*$");
    private static final Pattern CIDADE_UF = Pattern.compile("(?u)^[\\p{L}][\\p{L} .'-]*?\\s*[/-]\\s*[A-Za-z]{2}$");
    private static final Pattern LINHA_DE_RUA = Pattern.compile(
            "(?imu)^\\s*((?:rua|r\\.|av\\.?|avenida|travessa|tv\\.|estrada|rodovia|alameda|pra[c\\u00e7]a)\\s+.+?)\\s*$");
    private static final Pattern NOME_ENTRE_SETAS = Pattern.compile("(?u)[<\u00ab]{2,}\\s*([\\p{L}][\\p{L} .'-]{3,}?)\\s*>\\s*>");
    private static final Pattern DATA = Pattern.compile("\\b(\\d{2}/\\d{2}/\\d{4})\\b");

    public DadosComprovante extrair(String texto) {
        String t = texto == null ? "" : texto;
        String[] linhaDoCep = partesDaLinhaDoCep(t);
        String endereco = primeiro(valor(ENDERECO, t), linhaDoCep[0], valor(LINHA_DE_RUA, t));
        String cidade = primeiro(valor(CIDADE, t), linhaDoCep[1]);
        LocalDate emissao = Stream.of(valor(DATA_EMISSAO, t), valor(DATA_EMISSAO_SINONIMO, t))
                .map(ExtratorTexto::data).filter(Objects::nonNull).findFirst()
                .orElseGet(() -> dataMaisAntiga(t));
        return new DadosComprovante(nome(t), cpf(t), endereco, cidade, emissao, valor(CEP, t));
    }

    /** RG ou CNH: nome, CPF e, na CNH, a validade. */
    public DadosIdentificacao extrairIdentificacao(String texto) {
        String t = texto == null ? "" : texto;
        String normalizado = Texto.normalizar(t);
        String tipo = normalizado.contains("HABILITACAO") || normalizado.matches("(?s).*\\bCNH\\b.*") ? "CNH" : "RG";
        // RG: o nome do titular vem entre << >> (a filiacao vem depois, no mesmo formato)
        String nome = primeiro(nome(t), valor(NOME_ENTRE_SETAS, t));
        return new DadosIdentificacao(tipo, nome, cpfValido(t), data(valor(VALIDADE, t)));
    }

    static Pattern rotulo(String regexRotulo) {
        // so espaco e tab em volta do ":": com \\s o valor vazio "Pagador:" pegaria a linha de baixo
        return Pattern.compile("(?imu)^[ \\t]*" + regexRotulo + "[ \\t]*:[ \\t]*(.+?)[ \\t]*$");
    }

    static String valor(Pattern p, String texto) {
        Matcher m = p.matcher(texto);
        return m.find() ? m.group(1) : null;
    }

    private static String nome(String t) {
        return primeiro(valor(NOME, t), valor(NOME_SINONIMO, t), valor(NOME_ANTES_DO_CPF, t));
    }

    /** "DOMINGOS SAVIO, 77 - PIEDADE - JABOATAO DOS GUARARAPES/PE CEP: 54420-170" vira endereco e cidade/UF. */
    private static String[] partesDaLinhaDoCep(String texto) {
        String antesDoCep = valor(LINHA_DO_CEP, texto);
        if (antesDoCep == null || antesDoCep.isBlank()) {
            return new String[]{null, null};
        }
        List<String> partes = Arrays.stream(antesDoCep.split("\\s+[-\\u2013]\\s+"))
                .map(String::strip).filter(p -> !p.isEmpty()).toList();
        String ultima = partes.get(partes.size() - 1);
        if (partes.size() > 1 && CIDADE_UF.matcher(ultima).matches()) {
            return new String[]{String.join(" - ", partes.subList(0, partes.size() - 1)), ultima};
        }
        return new String[]{String.join(" - ", partes), null};
    }

    /** Sem rotulo de emissao: a data mais antiga do documento que nao esteja no futuro (vencimento vem depois). */
    private static LocalDate dataMaisAntiga(String texto) {
        LocalDate hoje = LocalDate.now();
        return DATA.matcher(texto).results()
                .map(r -> data(r.group(1)))
                .filter(d -> d != null && !d.isAfter(hoje))
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    private static String primeiro(String... valores) {
        for (String v : valores) {
            if (v != null && !v.isBlank()) {
                return v.strip();
            }
        }
        return null;
    }

    private static LocalDate data(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return LocalDate.parse(valor.strip(), DD_MM_AAAA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Em documento lido por OCR, numeros de serie e de registro tambem tem 11 digitos:
     * so aceita o primeiro candidato com digitos verificadores validos.
     */
    private static String cpfValido(String texto) {
        Matcher m = CPF.matcher(texto);
        while (m.find()) {
            if (br.com.mentoria.validacao.dominio.Cpf.digitosValidos(m.group())) {
                return m.group();
            }
        }
        return null;
    }

    private static String cpf(String texto) {
        Matcher m = CPF.matcher(texto);
        return m.find() ? m.group() : null;
    }
}
