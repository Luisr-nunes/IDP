package br.com.mentoria.validacao.api;

import br.com.mentoria.validacao.servico.ValidacaoDocumentoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Registros completos (resultado + dados extraidos) para a tela Banco de Dados e para migracao em JSON. */
@RestController
@RequestMapping("/api/banco")
public class BancoDeDadosController {

    private final ValidacaoDocumentoService service;

    public BancoDeDadosController(ValidacaoDocumentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<RegistroBanco> listar() {
        return service.listarBanco();
    }

    public record Exportacao(String sistema, Instant exportadoEm, int total, List<RegistroBanco> registros) {
    }

    /** Arquivo JSON com todos os registros, para importar no banco de outro sistema. */
    @GetMapping("/exportar")
    public ResponseEntity<Exportacao> exportar() {
        List<RegistroBanco> registros = service.listarBanco();
        String arquivo = "idp-banco-de-dados-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new Exportacao("IDP - Validacao Inteligente de Documentos", Instant.now(), registros.size(), registros));
    }
}
