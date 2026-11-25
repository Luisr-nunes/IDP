package br.com.mentoria.validacao.servico;

import br.com.mentoria.validacao.api.ValidacaoRequest;
import br.com.mentoria.validacao.persistencia.ValidacaoRegistroRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class LeituraPdfTest {

    private final ValidacaoDocumentoService service = new ValidacaoDocumentoService(
            new InterpretadorIaService("", new ExtratorTexto()), new ValidadorComprovante(90),
            new OcrService("tesseract", "", "por"), new OpenCvService(""), new PdfService(), new ExtratorTexto(),
            mock(ValidacaoRegistroRepository.class));

    static byte[] pdf(String... linhas) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);
            try (PDPageContentStream c = new PDPageContentStream(doc, pagina)) {
                c.beginText();
                c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                c.setLeading(16);
                c.newLineAtOffset(50, 700);
                for (String linha : linhas) {
                    c.showText(linha);
                    c.newLine();
                }
                c.endText();
            }
            var saida = new ByteArrayOutputStream();
            doc.save(saida);
            return saida.toByteArray();
        }
    }

    static byte[] pdfComPaginas(int quantidade) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < quantidade; i++) {
                doc.addPage(new PDPage());
            }
            var saida = new ByteArrayOutputStream();
            doc.save(saida);
            return saida.toByteArray();
        }
    }

    @Test
    void pdfComTextoELidoSemOcr() throws IOException {
        String base64 = Base64.getEncoder().encodeToString(pdf("Nome: Maria Souza", "Cidade: Recife"));

        var resposta = service.validar(new ValidacaoRequest("conta.pdf", "application/pdf", null, base64));

        assertEquals("Maria Souza", resposta.dadosExtraidos().nome());
        assertEquals("Recife", resposta.dadosExtraidos().cidade());
        assertTrue(resposta.etapas().get(2).resultado().startsWith("Texto lido direto do PDF"));
    }

    @Test
    void pdfInvalidoCaiNoConteudoDocumento() throws IOException {
        String base64 = Base64.getEncoder().encodeToString("nao e pdf".getBytes());

        var resposta = service.validar(new ValidacaoRequest("x.pdf", "application/pdf", "Nome: Ana Lima", base64));

        assertEquals("Ana Lima", resposta.dadosExtraidos().nome());
        assertTrue(resposta.etapas().get(2).resultado().startsWith("PDF invalido"));
    }

    @Test
    void pdfEscaneadoViraImagemParaOOcr() throws IOException {
        byte[] imagem = new PdfService().primeiraPaginaComoImagem(pdf());
        assertEquals((byte) 0x89, imagem[0]); // assinatura PNG
    }
}
