package br.edu.ifpb.vigisaude.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void testCsvSanitizerToNullIfInvalid() {
        assertNull(CsvSanitizer.toNullIfInvalid("None"));
        assertNull(CsvSanitizer.toNullIfInvalid("Não informado"));
        assertNull(CsvSanitizer.toNullIfInvalid("Mal Formatado"));
        assertNull(CsvSanitizer.toNullIfInvalid(""));
        assertNull(CsvSanitizer.toNullIfInvalid("   "));
        assertNull(CsvSanitizer.toNullIfInvalid("-"));
        assertEquals("RECIFE", CsvSanitizer.toNullIfInvalid("RECIFE"));
    }

    @Test
    void testCsvSanitizerDecodeHtmlEntities() {
        assertEquals("SOLUÇÃO", CsvSanitizer.decodeHtmlEntities("SOLU&#231;&#227;O"));
        assertEquals("ç", CsvSanitizer.decodeHtmlEntities("&ccedil;"));
    }

    @Test
    void testCsvSanitizerCleanZipCode() {
        assertEquals("52010100", CsvSanitizer.cleanZipCode("52010-100"));
        assertEquals("00010100", CsvSanitizer.cleanZipCode("10100"));
    }

    @Test
    void testCsvSanitizerCleanCnes() {
        assertEquals("0023456", CsvSanitizer.cleanCnes("23456"));
        assertEquals("2345678", CsvSanitizer.cleanCnes("2345678"));
    }

    @Test
    void testAddressParser() {
        ParsedAddress p1 = AddressParser.parse("AV HISTORIADOR PEREIRA COSTA", "123");
        assertEquals("AVENIDA", p1.tipoLogradouro());
        assertEquals("HISTORIADOR PEREIRA COSTA", p1.nomeLogradouro());
        assertEquals("123", p1.numero());
        assertFalse(p1.semNumero());

        ParsedAddress p2 = AddressParser.parse("RUA 21 DE ABRIL", "S/N");
        assertEquals("RUA", p2.tipoLogradouro());
        assertEquals("21 DE ABRIL", p2.nomeLogradouro());
        assertNull(p2.numero());
        assertTrue(p2.semNumero());
    }
}
