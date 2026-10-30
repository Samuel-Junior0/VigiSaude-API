package br.edu.ifpb.vigisaude.service;

import br.edu.ifpb.vigisaude.model.*;
import br.edu.ifpb.vigisaude.repository.*;
import br.edu.ifpb.vigisaude.utils.AddressParser;
import br.edu.ifpb.vigisaude.utils.CsvLineReader;
import br.edu.ifpb.vigisaude.utils.CsvSanitizer;
import br.edu.ifpb.vigisaude.utils.ParsedAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ETLService {

    private final SinonimoTermoRepository sinonimoTermoRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final ReacaoNotificadaRepository reacaoNotificadaRepository;
    private final MedicamentoNotificadoRepository medicamentoNotificadoRepository;
    private final PrincipioAtivoRepository principioAtivoRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final QuarentenaRepository quarentenaRepository;

    private static final Charset VIGIMED_CHARSET = Charset.forName("windows-1252");

    private static final Map<String, String> IBGE_UF_MAP = Map.ofEntries(
            Map.entry("11", "RO"), Map.entry("12", "AC"), Map.entry("13", "AM"), Map.entry("14", "RR"),
            Map.entry("15", "PA"), Map.entry("16", "AP"), Map.entry("17", "TO"), Map.entry("21", "MA"),
            Map.entry("22", "PI"), Map.entry("23", "CE"), Map.entry("24", "RN"), Map.entry("25", "PB"),
            Map.entry("26", "PE"), Map.entry("27", "AL"), Map.entry("28", "SE"), Map.entry("29", "BA"),
            Map.entry("31", "MG"), Map.entry("32", "ES"), Map.entry("33", "RJ"), Map.entry("35", "SP"),
            Map.entry("41", "PR"), Map.entry("42", "SC"), Map.entry("43", "RS"), Map.entry("50", "MS"),
            Map.entry("51", "MT"), Map.entry("52", "GO"), Map.entry("53", "DF")
    );

    public Map<String, String> carregarMapaMunicipios(String filePath) {
        Map<String, String> mapa = new HashMap<>();
        if (filePath == null || filePath.isBlank()) {
            log.warn("Caminho do arquivo de municípios não informado. Prosseguindo sem mapa de municípios.");
            return mapa;
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.warn("Arquivo de municípios não encontrado em: {}. Prosseguindo sem mapa de municípios.", filePath);
            return mapa;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.startsWith("codigo_ibge")) continue;

                String[] cols = line.split(",", -1);
                if (cols.length < 2) continue;

                String codigoIbge = cols[0].trim();
                String nome = cols[1].replace("\"", "").trim();
                if (codigoIbge.isEmpty() || nome.isEmpty()) continue;

                mapa.put(codigoIbge, nome);
                if (codigoIbge.length() > 6) {
                    mapa.putIfAbsent(codigoIbge.substring(0, 6), nome);
                }
            }
            log.info("Mapa de municípios carregado com {} códigos.", mapa.size());
        } catch (Exception e) {
            log.error("Erro ao carregar o mapa de municípios", e);
        }
        return mapa;
    }

    @Transactional
    public void carregarSinonimos() {
        if (sinonimoTermoRepository.count() == 0) {
            sinonimoTermoRepository.save(SinonimoTermo.builder().termoBruto("dor de cabeça").termoPadronizado("CEFALEIA").tipoTermo("SINTOMA").build());
            sinonimoTermoRepository.save(SinonimoTermo.builder().termoBruto("dor na cabeça").termoPadronizado("CEFALEIA").tipoTermo("SINTOMA").build());
            sinonimoTermoRepository.save(SinonimoTermo.builder().termoBruto("coceira").termoPadronizado("PRURIDO").tipoTermo("SINTOMA").build());
            log.info("Sinônimos iniciais carregados com sucesso.");
        }
    }

    @Transactional
    public void carregarSinonimosDeReacoes(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            log.warn("Caminho do arquivo de reações não informado. Sinônimos LLT/PT não carregados.");
            return;
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.warn("Arquivo VigiMed Reacoes não encontrado em: {}. Sinônimos LLT/PT não carregados.", filePath);
            return;
        }

        Map<String, String> termosOriginais = new LinkedHashMap<>();
        Map<String, String> lltParaPt = new LinkedHashMap<>();

        try (CsvLineReader reader = new CsvLineReader(Files.newBufferedReader(path, VIGIMED_CHARSET))) {
            String line;
            boolean header = true;
            int idxLlt = 1;
            int idxPt = 2;

            while ((line = reader.readLine()) != null) {
                if (header) {
                    String[] colunas = line.split(";", -1);
                    for (int i = 0; i < colunas.length; i++) {
                        String nomeColuna = colunas[i].replace("\"", "").trim();
                        if (nomeColuna.equalsIgnoreCase("REACAO_EVTO_ADVERSO_MEDDRA_LLT")) idxLlt = i;
                        if (nomeColuna.equalsIgnoreCase("PT")) idxPt = i;
                    }
                    header = false;
                    continue;
                }

                String[] cols = line.split(";", -1);
                if (cols.length <= Math.max(idxLlt, idxPt)) continue;

                String llt = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols[idxLlt]));
                String pt = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols[idxPt]));
                if (llt == null || pt == null) continue;

                String chave = llt.trim().toLowerCase(Locale.ROOT);
                if (chave.isEmpty()) continue;

                termosOriginais.putIfAbsent(chave, llt.trim());
                lltParaPt.putIfAbsent(chave, pt.trim().toUpperCase(Locale.ROOT));
            }
        } catch (Exception e) {
            log.error("Erro ao ler os sinônimos LLT/PT do arquivo de reações", e);
            return;
        }

        if (lltParaPt.isEmpty()) {
            log.warn("Nenhum par LLT/PT encontrado no arquivo de reações.");
            return;
        }

        Set<String> existentes = new HashSet<>();
        for (SinonimoTermo sinonimo : sinonimoTermoRepository.findAll()) {
            if (sinonimo.getTermoBruto() != null
                    && sinonimo.getTipoTermo() != null
                    && sinonimo.getTipoTermo().equalsIgnoreCase("SINTOMA")) {
                existentes.add(sinonimo.getTermoBruto().trim().toLowerCase(Locale.ROOT));
            }
        }

        List<SinonimoTermo> novos = new ArrayList<>();
        for (Map.Entry<String, String> par : lltParaPt.entrySet()) {
            String chave = par.getKey();
            if (existentes.contains(chave)) continue;

            novos.add(SinonimoTermo.builder()
                    .termoBruto(termosOriginais.get(chave))
                    .termoPadronizado(par.getValue())
                    .tipoTermo("SINTOMA")
                    .build());
            existentes.add(chave);
        }

        if (!novos.isEmpty()) {
            sinonimoTermoRepository.saveAll(novos);
        }
        log.info("Sinônimos LLT/PT carregados: {} novos de {} pares distintos.", novos.size(), lltParaPt.size());
    }

    @Transactional
    public void carregarCNES(String filePath, String municipiosCsvPath) {
        if (estabelecimentoRepository.count() > 0) {
            log.info("Estabelecimentos já carregados no banco. Pulando carga do CNES.");
            return;
        }

        Map<String, String> municipiosMap = carregarMapaMunicipios(municipiosCsvPath);

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.warn("Arquivo CNES não encontrado em: {}. Pulando carga.", filePath);
            return;
        }

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setQuote('"')
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = new CSVParser(
                Files.newBufferedReader(path, StandardCharsets.ISO_8859_1), formato)) {

            boolean header = true;
            long total = 0;
            for (CSVRecord cols : parser) {
                if (header) {
                    header = false;
                    continue;
                }
                if (cols.size() < 20) continue;

                String cnesRaw = cols.get(0);
                String coUf = CsvSanitizer.toNullIfInvalid(cols.get(2));
                String coIbge = CsvSanitizer.toNullIfInvalid(cols.get(3));
                String razaoSocial = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols.get(5)));
                String nomeFantasia = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols.get(6)));
                String tipoUnidade = CsvSanitizer.toNullIfInvalid(cols.get(15));
                String cepRaw = cols.get(16);
                String logradouroBruto = cols.get(17);
                String numeroBruto = cols.get(18);
                String bairro = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols.get(19)));

                String cnesClean = CsvSanitizer.cleanCnes(cnesRaw);
                if (cnesClean == null) continue;

                String ufSigla = coUf != null ? IBGE_UF_MAP.getOrDefault(coUf, coUf) : null;
                String nomeMunicipio = coIbge != null ? municipiosMap.getOrDefault(coIbge, coIbge) : null;

                ParsedAddress parsedAddress = AddressParser.parse(logradouroBruto, numeroBruto);
                Endereco endereco = Endereco.builder()
                        .tipoLogradouro(parsedAddress.tipoLogradouro())
                        .nomeLogradouro(CsvSanitizer.decodeHtmlEntities(parsedAddress.nomeLogradouro()))
                        .numero(parsedAddress.numero())
                        .semNumero(parsedAddress.semNumero())
                        .bairro(bairro)
                        .codigoMunicipio(coIbge)
                        .municipio(nomeMunicipio != null ? nomeMunicipio.toUpperCase() : null)
                        .uf(ufSigla)
                        .cep(CsvSanitizer.cleanZipCode(cepRaw))
                        .build();

                Estabelecimento estab = Estabelecimento.builder()
                        .cnes(cnesClean)
                        .razaoSocial(razaoSocial)
                        .nomeFantasia(nomeFantasia)
                        .tipoUnidade(tipoUnidade)
                        .endereco(endereco)
                        .build();

                estabelecimentoRepository.save(estab);
                total++;
            }
            log.info("Carga do CNES finalizada com sucesso. Registros: {}", total);
        } catch (Exception e) {
            log.error("Erro ao carregar CNES", e);
        }
    }

    @Transactional
    public void carregarNotificacoes(String filePath) {
        if (notificacaoRepository.count() > 0) {
            log.info("Notificações já carregadas no banco. Pulando carga.");
            return;
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.warn("Arquivo VigiMed Notificacoes não encontrado em: {}. Pulando carga.", filePath);
            return;
        }

        try (CsvLineReader reader = new CsvLineReader(Files.newBufferedReader(path, VIGIMED_CHARSET))) {
            String line;
            boolean header = true;
            long linhasMalformadas = 0;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }

                String linhaBruta = line;
                line = line.replace('\r', '|');

                String[] cols = line.split(";", -1);
                if (cols.length < 4) {
                    linhasMalformadas++;
                    registrarQuarentena("NOTIFICACAO", filePath, null, "LINHA_MALFORMADA", linhaBruta);
                    continue;
                }

                String uf = CsvSanitizer.toNullIfInvalid(cols[0]);
                String idNotificacao = CsvSanitizer.toNullIfInvalid(cols[3]);
                if (idNotificacao == null) {
                    linhasMalformadas++;
                    registrarQuarentena("NOTIFICACAO", filePath, null, "ID_VAZIO", linhaBruta);
                    continue;
                }

                String sexo = cols.length > 13 ? CsvSanitizer.toNullIfInvalid(cols[13]) : null;

                Integer idadePaciente = null;
                if (cols.length > 10) {
                    String idadeStr = CsvSanitizer.toNullIfInvalid(cols[10]);
                    if (idadeStr != null) {
                        try {
                            idadePaciente = (int) Double.parseDouble(idadeStr);
                        } catch (Exception ignored) {}
                    }
                }

                Notificacao notificacao = Notificacao.builder()
                        .identificacaoNotificacao(idNotificacao)
                        .uf(uf)
                        .sexo(sexo)
                        .idadePaciente(idadePaciente)
                        .build();

                notificacaoRepository.save(notificacao);
            }
            log.info("Carga de VigiMed Notificacoes finalizada. Linhas malformadas: {}", linhasMalformadas);
        } catch (Exception e) {
            log.error("Erro ao carregar VigiMed Notificacoes", e);
        }
    }

    @Transactional
    public void carregarReacoes(String filePath) {
        if (reacaoNotificadaRepository.count() > 0) {
            log.info("Reações já carregadas no banco. Pulando carga.");
            return;
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.warn("Arquivo VigiMed Reacoes não encontrado em: {}. Pulando carga.", filePath);
            return;
        }

        try (CsvLineReader reader = new CsvLineReader(Files.newBufferedReader(path, VIGIMED_CHARSET))) {
            String line;
            boolean header = true;
            long orfaosIgnorados = 0;
            long linhasMalformadas = 0;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                String linhaBruta = line;
                String[] cols = line.split(";", -1);
                if (cols.length < 3) {
                    linhasMalformadas++;
                    registrarQuarentena("REACAO", filePath, null, "LINHA_MALFORMADA", linhaBruta);
                    continue;
                }

                String notificacaoId = CsvSanitizer.toNullIfInvalid(cols[0]);
                if (notificacaoId == null) {
                    linhasMalformadas++;
                    registrarQuarentena("REACAO", filePath, null, "ID_VAZIO", linhaBruta);
                    continue;
                }

                Optional<Notificacao> notifOpt = notificacaoRepository.findById(notificacaoId);
                if (notifOpt.isEmpty()) {
                    orfaosIgnorados++;
                    registrarQuarentena("REACAO", filePath, notificacaoId, "ORFAO", linhaBruta);
                    continue;
                }

                String termoRelatado = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols[1]));
                String termoMeddraPt = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols[2]));
                String desfecho = cols.length > 11 ? CsvSanitizer.toNullIfInvalid(cols[11]) : null;

                ReacaoNotificada reacao = ReacaoNotificada.builder()
                        .notificacao(notifOpt.get())
                        .termoRelatado(termoRelatado)
                        .termoMeddraPt(termoMeddraPt != null ? termoMeddraPt : termoRelatado)
                        .desfecho(desfecho)
                        .build();

                reacaoNotificadaRepository.save(reacao);
            }
            log.info("Carga de VigiMed Reacoes finalizada. Órfãos: {}. Linhas malformadas: {}",
                    orfaosIgnorados, linhasMalformadas);
        } catch (Exception e) {
            log.error("Erro ao carregar VigiMed Reacoes", e);
        }
    }

    @Transactional
    public void carregarMedicamentos(String filePath) {
        if (medicamentoNotificadoRepository.count() > 0) {
            log.info("Medicamentos já carregados no banco. Pulando carga.");
            if (principioAtivoRepository.count() == 0) {
                log.warn("A tabela tb_principio_ativo está vazia. Limpe tb_medicamento_notificado e execute a carga novamente para popular os princípios ativos.");
            }
            return;
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.warn("Arquivo VigiMed Medicamentos não encontrado em: {}. Pulando carga.", filePath);
            return;
        }

        try (CsvLineReader reader = new CsvLineReader(Files.newBufferedReader(path, VIGIMED_CHARSET))) {
            String line;
            boolean header = true;
            long orfaosIgnorados = 0;
            long linhasMalformadas = 0;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                String linhaBruta = line;
                String[] cols = line.split(";", -1);
                if (cols.length < 4) {
                    linhasMalformadas++;
                    registrarQuarentena("MEDICAMENTO", filePath, null, "LINHA_MALFORMADA", linhaBruta);
                    continue;
                }

                String notificacaoId = CsvSanitizer.toNullIfInvalid(cols[0]);
                if (notificacaoId == null) {
                    linhasMalformadas++;
                    registrarQuarentena("MEDICAMENTO", filePath, null, "ID_VAZIO", linhaBruta);
                    continue;
                }

                Optional<Notificacao> notifOpt = notificacaoRepository.findById(notificacaoId);
                if (notifOpt.isEmpty()) {
                    orfaosIgnorados++;
                    registrarQuarentena("MEDICAMENTO", filePath, notificacaoId, "ORFAO", linhaBruta);
                    continue;
                }

                String relacaoEvento = CsvSanitizer.toNullIfInvalid(cols[1]);
                String nomeMed = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols[2]));
                String principioAtivoBruto = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols[3]));
                if (principioAtivoBruto == null) {
                    principioAtivoBruto = nomeMed;
                }

                MedicamentoNotificado med = MedicamentoNotificado.builder()
                        .notificacao(notifOpt.get())
                        .nomeMedicamento(nomeMed)
                        .relacaoEvento(relacaoEvento)
                        .build();

                medicamentoNotificadoRepository.save(med);

                List<String> principios = CsvSanitizer.splitPipeValues(principioAtivoBruto);
                if (!principios.isEmpty()) {
                    List<PrincipioAtivo> filhos = new ArrayList<>(principios.size());
                    int ordem = 0;
                    for (String principio : principios) {
                        filhos.add(PrincipioAtivo.builder()
                                .medicamentoNotificado(med)
                                .principioAtivo(principio)
                                .ordem(ordem++)
                                .build());
                    }
                    principioAtivoRepository.saveAll(filhos);
                }
            }
            log.info("Carga de VigiMed Medicamentos finalizada. Órfãos: {}. Linhas malformadas: {}",
                    orfaosIgnorados, linhasMalformadas);
        } catch (Exception e) {
            log.error("Erro ao carregar VigiMed Medicamentos", e);
        }
    }

    @Transactional
    public void carregarMedicamentosAnvisa(String filePath) {
        if (medicamentoRepository.count() > 0) {
            log.info("Medicamentos da Anvisa já carregados no banco. Pulando carga.");
            return;
        }

        if (filePath == null || filePath.isBlank()) {
            log.warn("Caminho da base de medicamentos da Anvisa não informado. Pulando carga.");
            return;
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.warn("Arquivo de medicamentos da Anvisa não encontrado em: {}. Pulando carga.", filePath);
            return;
        }

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setQuote('"')
                .setIgnoreEmptyLines(true)
                .build();

        try (CSVParser parser = new CSVParser(
                Files.newBufferedReader(path, StandardCharsets.ISO_8859_1), formato)) {

            Iterator<CSVRecord> iterator = parser.iterator();
            if (!iterator.hasNext()) {
                log.warn("Arquivo da Anvisa sem registros: {}", filePath);
                return;
            }

            CSVRecord cabecalho = iterator.next();
            Map<String, Integer> colunas = new HashMap<>();
            for (int i = 0; i < cabecalho.size(); i++) {
                colunas.put(cabecalho.get(i).trim().toUpperCase(Locale.ROOT), i);
            }

            Integer idxNome = colunas.get("NOME_PRODUTO");
            Integer idxDetentor = colunas.get("EMPRESA_DETENTORA_REGISTRO");
            Integer idxPrincipio = colunas.get("PRINCIPIO_ATIVO");

            if (idxNome == null || idxDetentor == null || idxPrincipio == null) {
                log.error("Cabeçalho inesperado no arquivo da Anvisa. Colunas encontradas: {}", colunas.keySet());
                return;
            }

            long total = 0;
            while (iterator.hasNext()) {
                CSVRecord registro = iterator.next();

                String nomeProduto = CsvSanitizer.decodeHtmlEntities(valorDaColuna(registro, idxNome));
                String principioAtivo = CsvSanitizer.decodeHtmlEntities(valorDaColuna(registro, idxPrincipio));
                String detentor = CsvSanitizer.decodeHtmlEntities(valorDaColuna(registro, idxDetentor));

                String cnpj = null;
                String empresa = detentor;
                if (detentor != null) {
                    int separador = detentor.indexOf(" - ");
                    if (separador > 0) {
                        cnpj = detentor.substring(0, separador).trim();
                        empresa = detentor.substring(separador + 3).trim();
                    }
                }

                Medicamento medicamento = Medicamento.builder()
                        .nomeProduto(CsvSanitizer.toNullIfInvalid(nomeProduto))
                        .principioAtivo(CsvSanitizer.toNullIfInvalid(principioAtivo))
                        .cnpjDetentor(CsvSanitizer.toNullIfInvalid(cnpj))
                        .empresaDetentora(CsvSanitizer.toNullIfInvalid(empresa))
                        .build();

                medicamentoRepository.save(medicamento);
                total++;
            }

            log.info("Carga de medicamentos da Anvisa finalizada. Registros: {}", total);
        } catch (Exception e) {
            log.error("Erro ao carregar medicamentos da Anvisa", e);
        }
    }

    private static String valorDaColuna(CSVRecord registro, Integer indice) {
        if (indice == null || indice < 0 || indice >= registro.size()) {
            return null;
        }
        return registro.get(indice);
    }

    private void registrarQuarentena(String origem, String arquivo, String identificacao,
                                     String motivo, String linhaBruta) {
        quarentenaRepository.save(Quarentena.builder()
                .origem(origem)
                .arquivo(arquivo)
                .identificacaoNotificacao(identificacao)
                .motivo(motivo)
                .linhaBruta(linhaBruta)
                .build());
    }
}
