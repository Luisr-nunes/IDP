package br.com.mentoria.validacao.persistencia;

import br.com.mentoria.validacao.dominio.ResultadoValidacao;
import br.com.mentoria.validacao.dominio.StatusValidacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Resultado de uma validacao. Guarda so o necessario para auditoria: nao guarda o texto bruto do documento,
 * a imagem nem os dados pessoais extraidos (nome, CPF, endereco).
 */
@Entity
public class ValidacaoRegistro {

    private static final String SEPARADOR = "\n";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusValidacao status;

    private boolean aprovado;

    @Column(length = 2000)
    private String pendencias;

    @Column(nullable = false)
    private Instant criadoEm;

    protected ValidacaoRegistro() {
    }

    public ValidacaoRegistro(String nomeArquivo, ResultadoValidacao resultado, Instant criadoEm) {
        this.nomeArquivo = nomeArquivo;
        this.status = resultado.status();
        this.aprovado = resultado.aprovado();
        this.pendencias = String.join(SEPARADOR, resultado.pendencias());
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public StatusValidacao getStatus() {
        return status;
    }

    public boolean isAprovado() {
        return aprovado;
    }

    public List<String> getPendencias() {
        return pendencias == null || pendencias.isEmpty() ? List.of() : Arrays.asList(pendencias.split(SEPARADOR));
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
