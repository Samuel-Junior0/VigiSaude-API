package br.edu.ifpb.vigisaude.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressParser {

    private static final Pattern STREET_TYPE_PATTERN = Pattern.compile(
        "^(R|RUA|AV|AVN|AVENIDA|PC|PCA|PRACA|TV|TRAV|TRAVESSA|ROD|RODOVIA|AL|ALAMEDA|QD|QUADRA|ST|SETOR)\\b[\\.\\s]*",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    public static ParsedAddress parse(String logradouroBruto, String numeroBruto) {
        String tipoLogradouro = null;
        String nomeLogradouro = null;

        if (logradouroBruto != null) {
            String trimmed = logradouroBruto.trim();
            Matcher matcher = STREET_TYPE_PATTERN.matcher(trimmed);
            if (matcher.find()) {
                String rawPrefix = matcher.group(1).toUpperCase();
                tipoLogradouro = normalizeStreetType(rawPrefix);
                nomeLogradouro = trimmed.substring(matcher.end()).trim();
            } else {
                tipoLogradouro = "RUA";
                nomeLogradouro = trimmed;
            }
        }

        String numero = null;
        boolean semNumero = false;

        if (numeroBruto != null) {
            String sanitizedNum = numeroBruto.trim();
            if (sanitizedNum.equalsIgnoreCase("S/N") || 
                sanitizedNum.equalsIgnoreCase("SN") || 
                sanitizedNum.equalsIgnoreCase("S.N.") ||
                sanitizedNum.equalsIgnoreCase("S / N") ||
                sanitizedNum.isEmpty()) {
                numero = null;
                semNumero = true;
            } else {
                numero = CsvSanitizer.toNullIfInvalid(sanitizedNum);
                semNumero = (numero == null);
            }
        } else {
            semNumero = true;
        }

        return new ParsedAddress(tipoLogradouro, nomeLogradouro, numero, semNumero);
    }

    private static String normalizeStreetType(String rawPrefix) {
        switch (rawPrefix) {
            case "R":
            case "RUA":
                return "RUA";
            case "AV":
            case "AVN":
            case "AVENIDA":
                return "AVENIDA";
            case "PC":
            case "PCA":
            case "PRACA":
                return "PRAÇA";
            case "TV":
            case "TRAV":
            case "TRAVESSA":
                return "TRAVESSA";
            case "ROD":
            case "RODOVIA":
                return "RODOVIA";
            case "AL":
            case "ALAMEDA":
                return "ALAMEDA";
            case "QD":
            case "QUADRA":
                return "QUADRA";
            case "ST":
            case "SETOR":
                return "SETOR";
            default:
                return rawPrefix;
        }
    }
}
