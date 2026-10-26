package br.edu.ifpb.vigisaude.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * Lê linhas de arquivos CSV usando apenas o caractere '\n' como terminador.
 *
 * Diferentemente de {@link java.io.BufferedReader#readLine()}, esta classe não
 * trata '\r' como quebra de linha. Assim, eventuais '\r' internos (presentes,
 * por exemplo, no campo NOTIFICADOR da base VigiMed) são preservados, e apenas
 * o '\r' que antecede um '\n' (fim de linha CRLF) é removido.
 */
public final class CsvLineReader implements Closeable {

    private final Reader reader;
    private final char[] buffer = new char[8192];
    private int pos;
    private int length;

    public CsvLineReader(Reader reader) {
        this.reader = reader;
    }

    /**
     * @return a próxima linha, sem o '\n' e sem o '\r' de um CRLF; ou null no fim do arquivo.
     */
    public String readLine() throws IOException {
        StringBuilder acumulado = null;

        while (true) {
            if (pos >= length) {
                length = reader.read(buffer);
                pos = 0;
                if (length == -1) {
                    break;
                }
            }

            int inicio = pos;
            while (pos < length && buffer[pos] != '\n') {
                pos++;
            }

            if (pos < length) {
                if (acumulado == null) {
                    int fim = pos;
                    if (fim > inicio && buffer[fim - 1] == '\r') {
                        fim--;
                    }
                    pos++; // consome o '\n'
                    return new String(buffer, inicio, fim - inicio);
                }

                acumulado.append(buffer, inicio, pos - inicio);
                pos++; // consome o '\n'
                removerCrFinal(acumulado);
                return acumulado.toString();
            }

            if (acumulado == null) {
                acumulado = new StringBuilder(256);
            }
            acumulado.append(buffer, inicio, length - inicio);
        }

        if (acumulado == null) {
            return null;
        }
        removerCrFinal(acumulado);
        return acumulado.toString();
    }

    private static void removerCrFinal(StringBuilder texto) {
        int tamanho = texto.length();
        if (tamanho > 0 && texto.charAt(tamanho - 1) == '\r') {
            texto.setLength(tamanho - 1);
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
