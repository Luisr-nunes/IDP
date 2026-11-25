package br.com.mentoria.validacao.servico;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** PDF com texto e lido direto; PDF escaneado vira imagem (primeira pagina, 300 DPI) para o OCR. */
@Service
public class PdfService {

    public String extrairTexto(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento).strip();
        }
    }

    public byte[] primeiraPaginaComoImagem(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            var imagem = new PDFRenderer(documento).renderImageWithDPI(0, 300, ImageType.GRAY);
            var saida = new ByteArrayOutputStream();
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        }
    }
}
