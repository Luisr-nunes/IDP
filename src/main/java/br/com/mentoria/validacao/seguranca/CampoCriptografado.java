package br.com.mentoria.validacao.seguranca;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Grava a coluna cifrada com AES-GCM e devolve o texto aberto ao ler. */
@Converter
public class CampoCriptografado implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String valor) {
        return valor == null ? null : Cifra.atual().cifrar(valor);
    }

    @Override
    public String convertToEntityAttribute(String coluna) {
        return coluna == null ? null : Cifra.atual().decifrar(coluna);
    }
}
