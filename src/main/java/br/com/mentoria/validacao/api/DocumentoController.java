package br.com.mentoria.validacao.api;

import br.com.mentoria.validacao.persistencia.ValidacaoRegistro;
import br.com.mentoria.validacao.servico.ValidacaoDocumentoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final ValidacaoDocumentoService service;

    public DocumentoController(ValidacaoDocumentoService service) {
        this.service = service;
    }

    /**
     * Payload de demonstracao: comprovante de residencia e CNH do mesmo titular, CPF valido,
     * emissao de 10 dias atras e CNH valida por mais 2 anos, para o exemplo sempre ser aprovado.
     */
    @GetMapping("/exemplo")
    public ValidacaoRequest exemplo() {
        DateTimeFormatter ddMMyyyy = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String emissao = LocalDate.now().minusDays(10).format(ddMMyyyy);
        String validade = LocalDate.now().plusYears(2).format(ddMMyyyy);
        return new ValidacaoRequest(
                "comprovante-residencia.pdf",
                "application/pdf",
                "COMPROVANTE DE RESIDENCIA\nNome: Joao da Silva\nCPF: 529.982.247-25\nEndereco: Rua das Flores, 150\nCidade: Guanambi - BA\nData de emissao: " + emissao,
                null,
                new DocumentoIdentificacao(
                        "cnh.pdf",
                        "application/pdf",
                        "CARTEIRA NACIONAL DE HABILITACAO\nNome: Joao da Silva\nCPF: 529.982.247-25\nValidade: " + validade,
                        null));
    }

    @PostMapping("/validar")
    public ResponseEntity<ValidacaoResponse> validar(@Valid @RequestBody ValidacaoRequest request) throws IOException {
        ValidacaoResponse resposta = service.validar(request);
        return resposta.id() == null ? ResponseEntity.ok(resposta)
                : ResponseEntity.ok().location(URI.create("/api/documentos/" + resposta.id())).body(resposta);
    }

    /** Historico das ultimas 20 validacoes (sem texto do documento nem dados pessoais). */
    @GetMapping
    public List<ValidacaoRegistro> listar() {
        return service.listarRecentes();
    }

    /** Consulta o resultado salvo de uma validacao (sem texto do documento nem dados pessoais). */
    @GetMapping("/{id}")
    public ResponseEntity<ValidacaoRegistro> buscar(@PathVariable Long id) {
        return ResponseEntity.of(service.buscar(id));
    }
}
