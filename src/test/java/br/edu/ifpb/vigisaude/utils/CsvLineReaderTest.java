package br.edu.ifpb.vigisaude.utils;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CsvLineReaderTest {

    @Test
    void leLinhasComCrlf() throws Exception {
        try (CsvLineReader reader = new CsvLineReader(new StringReader("a;b\r\nc;d\r\n"))) {
            assertEquals("a;b", reader.readLine());
            assertEquals("c;d", reader.readLine());
            assertNull(reader.readLine());
        }
    }

    @Test
    void preservaCrInternoENaoQuebraLinha() throws Exception {
        try (CsvLineReader reader = new CsvLineReader(new StringReader("a;b\rc;d\ne;f\n"))) {
            assertEquals("a;b\rc;d", reader.readLine());
            assertEquals("e;f", reader.readLine());
            assertNull(reader.readLine());
        }
    }

    @Test
    void aceitaUltimaLinhaSemQuebra() throws Exception {
        try (CsvLineReader reader = new CsvLineReader(new StringReader("a;b\r\nc;d"))) {
            assertEquals("a;b", reader.readLine());
            assertEquals("c;d", reader.readLine());
            assertNull(reader.readLine());
        }
    }

    @Test
    void retornaNullParaArquivoVazio() throws Exception {
        try (CsvLineReader reader = new CsvLineReader(new StringReader(""))) {
            assertNull(reader.readLine());
        }
    }
}
