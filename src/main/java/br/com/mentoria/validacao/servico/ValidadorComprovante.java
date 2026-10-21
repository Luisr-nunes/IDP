package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.dominio.DadosComprovante;
import br.com.mentoria.validacao.dominio.ResultadoValidacao;
import br.com.mentoria.validacao.dominio.StatusValidacao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ValidadorComprovante {

    private final int prazoEmissaoDias;

    public ValidadorComprovante(@Value("${app.validacao.prazo-emissao-dias:90}") int prazoEmissaoDias) {
        this.prazoEmissaoDias = prazoEmissaoDias;
    }

    public ResultadoValidacao validar(DadosComprovante dados, LocalDate hoje) {
        List<String> atendidas = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        checar(preenchido(dados.nome()), "Nome identificado", atendidas, pendencias);
        checar(preenchido(dados.endereco()), "Endereco identificado", atendidas, pendencias);

        LocalDate emissao = dados.dataEmissao();
        if (emissao == null) {
            pendencias.add("Data de emissao nao reconhecida");
        } else {
            checar(!emissao.isAfter(hoje), "Data de emissao nao esta no futuro", atendidas, pendencias);
            checar(!emissao.isBefore(hoje.minusDays(prazoEmissaoDias)),
                    "Emissao dentro de " + prazoEmissaoDias + " dias", atendidas, pendencias);
        }

        StatusValidacao status = pendencias.isEmpty() ? StatusValidacao.APROVADO : StatusValidacao.REPROVADO;
        return new ResultadoValidacao(status, atendidas, pendencias);
    }

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static void checar(boolean ok, String regra, List<String> atendidas, List<String> pendencias) {
        (ok ? atendidas : pendencias).add(ok ? regra : "Falhou: " + regra);
    }
}
