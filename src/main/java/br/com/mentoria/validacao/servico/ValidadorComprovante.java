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
        checar(preenchido(dados.cpf()), "CPF encontrado", atendidas, pendencias);
        checar(preenchido(dados.endereco()), "Endereco encontrado", atendidas, pendencias);
        checar(preenchido(dados.cidade()), "Cidade encontrada", atendidas, pendencias);

        LocalDate emissao = dados.dataEmissao();
        checar(emissao != null, "Data de emissao reconhecida", atendidas, pendencias);
        boolean dentroDoPrazo = emissao != null
                && !emissao.isAfter(hoje)
                && !emissao.isBefore(hoje.minusDays(prazoEmissaoDias));
        checar(dentroDoPrazo, "Emissao dentro de " + prazoEmissaoDias + " dias", atendidas, pendencias);

        StatusValidacao status = pendencias.isEmpty() ? StatusValidacao.APROVADO : StatusValidacao.REPROVADO;
        return new ResultadoValidacao(status == StatusValidacao.APROVADO, status, atendidas, pendencias);
    }

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static void checar(boolean ok, String regra, List<String> atendidas, List<String> pendencias) {
        (ok ? atendidas : pendencias).add(ok ? regra : "Falhou: " + regra);
    }
}
