package br.edu.ifpb.vigisaude.utils;

public record ParsedAddress(
    String tipoLogradouro,
    String nomeLogradouro,
    String numero,
    Boolean semNumero
) {}
