package br.com.mentoria.validacao.persistencia;

import br.com.mentoria.validacao.dominio.ResultadoValidacao;
import br.com.mentoria.validacao.dominio.RevisaoNaoPermitidaException;
import br.com.mentoria.validacao.dominio.StatusValidacao;
import br.com.mentoria.validacao.seguranca.CampoCriptografado;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

    /** Decisao do analista quando o documento caiu em revisao manual. */
    private boolean revisadoManualmente;

    @Column(length = 500)
    private String motivoRevisao;

    private Instant revisadoEm;

    /** Dados extraidos (nome, CPF, endereco...) em JSON, gravados cifrados com AES-GCM. Nunca saem no historico. */
    @JsonIgnore
    @Convert(converter = CampoCriptografado.class)
    @Column(length = 4000)
    private String dadosExtraidos;

    protected ValidacaoRegistro() {
    }

    public ValidacaoRegistro(String nomeArquivo, ResultadoValidacao resultado, Instant criadoEm) {
        this.nomeArquivo = nomeArquivo;
        this.status = resultado.status();
        this.aprovado = resultado.aprovado();
        this.pendencias = String.join(SEPARADOR, resultado.pendencias());
        this.criadoEm = criadoEm;
    }

    /** O analista aprova ou reprova um documento que estava em revisao manual; o motivo fica registrado. */
    public void revisar(StatusValidacao decisao, String motivo, Instant quando) {
        if (status != StatusValidacao.REVISAO_MANUAL) {
            throw new RevisaoNaoPermitidaException();
        }
        if (decisao == StatusValidacao.REVISAO_MANUAL) {
            throw new IllegalArgumentException("Decisao deve ser APROVADO ou REPROVADO");
        }
        this.status = decisao;
        this.aprovado = decisao == StatusValidacao.APROVADO;
        this.revisadoManualmente = true;
        this.motivoRevisao = motivo;
        this.revisadoEm = quando;
    }

    public void guardarDados(String json) {
        this.dadosExtraidos = json;
    }

    public String dadosExtraidos() {
        return dadosExtraidos;
    }

    public boolean isRevisadoManualmente() {
        return revisadoManualmente;
    }

    public String getMotivoRevisao() {
        return motivoRevisao;
    }

    public Instant getRevisadoEm() {
        return revisadoEm;
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
