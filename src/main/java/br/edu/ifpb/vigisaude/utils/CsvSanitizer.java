package br.edu.ifpb.vigisaude.utils;

import org.apache.commons.text.StringEscapeUtils;

import java.util.Arrays;
import java.util.List;

public class CsvSanitizer {

    public static String toNullIfInvalid(String valor) {
        if (valor == null) {
            return null;
        }
        String trimmed = valor.trim();
        if (trimmed.isEmpty() ||
            trimmed.equalsIgnoreCase("None") ||
            trimmed.equalsIgnoreCase("Não informado") ||
            trimmed.equalsIgnoreCase("Nao informado") ||
            trimmed.equalsIgnoreCase("Mal Formatado") ||
            trimmed.equalsIgnoreCase("NOT SPECIFIED") ||
            trimmed.equalsIgnoreCase("NULL") ||
            trimmed.equals("-")) {
            return null;
        }
        return trimmed;
    }

    /**
     * Decodifica entidades HTML4 nomeadas e numéricas (decimais e hexadecimais),
     * incluindo code points fora do BMP (ex.: &#x1F600;).
     */
    public static String decodeHtmlEntities(String texto) {
        if (texto == null) {
            return null;
        }
        return StringEscapeUtils.unescapeHtml4(texto);
    }

    public static List<String> splitPipeValues(String valor) {
        if (valor == null) {
            return List.of();
        }
        return Arrays.stream(valor.split("\\|"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .toList();
    }

    public static String cleanZipCode(String cep) {
        if (cep == null) {
            return null;
        }
        String digits = cep.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.length() > 8) {
            digits = digits.substring(0, 8);
        }
        return String.format("%08d", Long.parseLong(digits));
    }

    public static String cleanCnes(String cnes) {
        if (cnes == null) {
            return null;
        }
        String digits = cnes.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.length() > 7) {
            digits = digits.substring(digits.length() - 7);
        }
        return String.format("%07d", Long.parseLong(digits));
    }
}
