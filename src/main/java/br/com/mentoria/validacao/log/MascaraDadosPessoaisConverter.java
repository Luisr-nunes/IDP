package br.com.mentoria.validacao.log;

import br.com.mentoria.validacao.dominio.Cpf;
import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/** Rede de seguranca do log: qualquer CPF que chegue a uma mensagem sai mascarado (ver logback-spring.xml). */
public class MascaraDadosPessoaisConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return Cpf.mascarar(super.convert(event));
    }
}
