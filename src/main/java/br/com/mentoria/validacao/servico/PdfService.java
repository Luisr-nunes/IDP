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
import java.util.ArrayList;
import java.util.List;

/** PDF com texto e lido direto; PDF escaneado vira imagens (uma por pagina, 300 DPI) para o OCR. */
@Service
public class PdfService {

    public String extrairTexto(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento).strip();
        }
    }

    /** Limite de paginas enviadas ao OCR (frente e verso de documento, sem processar PDFs enormes). */
    static final int MAXIMO_PAGINAS_OCR = 4;

    public byte[] primeiraPaginaComoImagem(byte[] pdf) throws IOException {
        return paginasComoImagem(pdf).get(0);
    }

    /** Cada pagina (ate MAXIMO_PAGINAS_OCR) vira uma imagem PNG em tons de cinza, 300 DPI. */
    public List<byte[]> paginasComoImagem(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            var renderizador = new PDFRenderer(documento);
            List<byte[]> paginas = new ArrayList<>();
            for (int i = 0; i < Math.min(documento.getNumberOfPages(), MAXIMO_PAGINAS_OCR); i++) {
                var saida = new ByteArrayOutputStream();
                ImageIO.write(renderizador.renderImageWithDPI(i, 300, ImageType.GRAY), "png", saida);
                paginas.add(saida.toByteArray());
            }
            return paginas;
        }
    }
}
