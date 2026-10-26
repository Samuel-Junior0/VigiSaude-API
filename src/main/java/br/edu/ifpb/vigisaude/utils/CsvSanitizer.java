package br.edu.ifpb.vigisaude.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvSanitizer {

    private static final Pattern HTML_ENTITY_NUM_PATTERN = Pattern.compile("&#(\\d+);");
    private static final Pattern HTML_ENTITY_HEX_PATTERN = Pattern.compile("&#x([0-9a-fA-F]+);");

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

    public static String decodeHtmlEntities(String texto) {
        if (texto == null) {
            return null;
        }

        String decoded = texto
                .replace("&ccedil;", "ç")
                .replace("&Ccedil;", "Ç")
                .replace("&atilde;", "ã")
                .replace("&Atilde;", "Ã")
                .replace("&aacute;", "á")
                .replace("&Aacute;", "Á")
                .replace("&eacute;", "é")
                .replace("&Eacute;", "É")
                .replace("&iacute;", "í")
                .replace("&Iacute;", "Í")
                .replace("&oacute;", "ó")
                .replace("&Oacute;", "Ó")
                .replace("&uacute;", "ú")
                .replace("&Uacute;", "Ú")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");

        Matcher matcherNum = HTML_ENTITY_NUM_PATTERN.matcher(decoded);
        StringBuffer sbNum = new StringBuffer();
        while (matcherNum.find()) {
            try {
                int codePoint = Integer.parseInt(matcherNum.group(1));
                matcherNum.appendReplacement(sbNum, Matcher.quoteReplacement(String.valueOf((char) codePoint)));
            } catch (NumberFormatException e) {
                // Keep original
            }
        }
        matcherNum.appendTail(sbNum);
        decoded = sbNum.toString();

        Matcher matcherHex = HTML_ENTITY_HEX_PATTERN.matcher(decoded);
        StringBuffer sbHex = new StringBuffer();
        while (matcherHex.find()) {
            try {
                int codePoint = Integer.parseInt(matcherHex.group(1), 16);
                matcherHex.appendReplacement(sbHex, Matcher.quoteReplacement(String.valueOf((char) codePoint)));
            } catch (NumberFormatException e) {
                // Keep original
            }
        }
        matcherHex.appendTail(sbHex);
        decoded = sbHex.toString();

        return decoded;
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
