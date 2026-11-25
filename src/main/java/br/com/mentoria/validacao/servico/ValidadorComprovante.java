package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.dominio.Cpf;
import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.ResultadoValidacao;
import br.com.mentoria.validacao.dominio.StatusValidacao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ValidadorComprovante {

    private final int prazoEmissaoDias;

    public ValidadorComprovante(@Value("${app.validacao.prazo-emissao-dias:90}") int prazoEmissaoDias) {
        this.prazoEmissaoDias = prazoEmissaoDias;
    }

    public ResultadoValidacao validar(DadosComprovante dados, LocalDate hoje) {
        Regras r = new Regras();

        r.checar(preenchido(dados.nome()), "Nome identificado", false);
        r.checar(preenchido(dados.cpf()), "CPF encontrado", false);
        if (preenchido(dados.cpf())) {
            r.checar(Cpf.digitosValidos(dados.cpf()), "CPF valido", true);
        }
        r.checar(preenchido(dados.endereco()), "Endereco encontrado", false);
        r.checar(preenchido(dados.cidade()), "Cidade encontrada", false);

        LocalDate emissao = dados.dataEmissao();
        r.checar(emissao != null, "Data de emissao reconhecida", false);
        boolean dentroDoPrazo = emissao != null
                && !emissao.isAfter(hoje)
                && !emissao.isBefore(hoje.minusDays(prazoEmissaoDias));
        // data lida e fora do prazo reprova; data ausente so deixa a regra pendente
        r.checar(dentroDoPrazo, "Emissao dentro de " + prazoEmissaoDias + " dias", emissao != null);

        long encontrados = Stream.of(dados.nome(), dados.cpf(), dados.endereco(), dados.cidade())
                .filter(ValidadorComprovante::preenchido).count() + (emissao != null ? 1 : 0);
        return r.resultado(encontrados > 0);
    }

    static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    /**
     * Acumula regras atendidas e pendencias. Falha de regra "dura" (CPF invalido, prazo vencido) reprova;
     * campo ausente com outros campos lidos vai para revisao manual (OCR ruim ou valor da IA descartado).
     */
    static final class Regras {
        final List<String> atendidas = new ArrayList<>();
        final List<String> pendencias = new ArrayList<>();
        boolean reprova;

        void checar(boolean ok, String regra, boolean reprovaSeFalhar) {
            if (ok) {
                atendidas.add(regra);
            } else {
                pendencias.add("Falhou: " + regra);
                reprova |= reprovaSeFalhar;
            }
        }

        ResultadoValidacao resultado(boolean algoFoiLido) {
            StatusValidacao status = pendencias.isEmpty() ? StatusValidacao.APROVADO
                    : !reprova && algoFoiLido ? StatusValidacao.REVISAO_MANUAL
                    : StatusValidacao.REPROVADO;
            return new ResultadoValidacao(status == StatusValidacao.APROVADO, status, atendidas, pendencias);
        }
    }
}
