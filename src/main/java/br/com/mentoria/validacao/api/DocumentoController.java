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

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final ValidacaoDocumentoService service;

    public DocumentoController(ValidacaoDocumentoService service) {
        this.service = service;
    }

    @GetMapping("/exemplo")
    public ValidacaoRequest exemplo() {
        return new ValidacaoRequest(
                "comprovante-residencia.pdf",
                "application/pdf",
                "COMPROVANTE DE RESIDENCIA\nNome: Joao da Silva\nCPF: 123.456.789-00\nEndereco: Rua das Flores, 150\nCidade: Guanambi - BA\nData de emissao: 10/08/2026",
                null);
    }

    @PostMapping("/validar")
    public ResponseEntity<ValidacaoResponse> validar(@Valid @RequestBody ValidacaoRequest request) throws IOException {
        ValidacaoResponse resposta = service.validar(request);
        return resposta.id() == null ? ResponseEntity.ok(resposta)
                : ResponseEntity.ok().location(URI.create("/api/documentos/" + resposta.id())).body(resposta);
    }

    /** Consulta o resultado salvo de uma validacao (sem texto do documento nem dados pessoais). */
    @GetMapping("/{id}")
    public ResponseEntity<ValidacaoRegistro> buscar(@PathVariable Long id) {
        return ResponseEntity.of(service.buscar(id));
    }
}
