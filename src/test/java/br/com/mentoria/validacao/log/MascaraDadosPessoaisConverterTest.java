package br.com.mentoria.validacao.log;

import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class MascaraDadosPessoaisConverterTest {

    @Test
    void mascaraCpfNaMensagemFormatada() {
        LoggingEvent evento = new LoggingEvent();
        evento.setMessage("documento do CPF {}");
        evento.setArgumentArray(new Object[]{"123.456.789-00"});

        assertEquals("documento do CPF ***.***.***-**", new MascaraDadosPessoaisConverter().convert(evento));
    }

    @Test
    void logDaAplicacaoSaiComCpfMascarado(CapturedOutput saida) {
        LoggerFactory.getLogger(MascaraDadosPessoaisConverterTest.class).info("teste de log CPF {}", "529.982.247-25");

        assertTrue(saida.getOut().contains("teste de log CPF ***.***.***-**"));
        assertFalse(saida.getOut().contains("529.982.247-25"));
    }
}
