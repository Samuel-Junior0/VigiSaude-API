package br.edu.ifpb.vigisaude.service;

import br.edu.ifpb.vigisaude.model.*;
import br.edu.ifpb.vigisaude.repository.*;
import br.edu.ifpb.vigisaude.utils.AddressParser;
import br.edu.ifpb.vigisaude.utils.CsvSanitizer;
import br.edu.ifpb.vigisaude.utils.ParsedAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ETLService {

    private final SinonimoTermoRepository sinonimoTermoRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final ReacaoNotificadaRepository reacaoNotificadaRepository;
    private final MedicamentoNotificadoRepository medicamentoNotificadoRepository;

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
    public void carregarCNES(String filePath) {
        if (estabelecimentoRepository.count() > 0) {
            log.info("Estabelecimentos já carregados no banco. Pulando carga do CNES.");
            return;
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.warn("Arquivo CNES não encontrado em: {}. Pulando carga.", filePath);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                String[] cols = line.split(";", -1);
                if (cols.length < 5) continue;

                String cnesRaw = cols[0];
                String razaoSocial = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols[1]));
                String nomeFantasia = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols[2]));
                String tipoUnidade = CsvSanitizer.toNullIfInvalid(cols[3]);
                String logradouroBruto = cols.length > 4 ? cols[4] : null;
                String numeroBruto = cols.length > 5 ? cols[5] : null;
                String bairro = cols.length > 6 ? CsvSanitizer.toNullIfInvalid(cols[6]) : null;
                String municipio = cols.length > 7 ? CsvSanitizer.toNullIfInvalid(cols[7]) : null;
                String uf = cols.length > 8 ? CsvSanitizer.toNullIfInvalid(cols[8]) : null;
                String cepRaw = cols.length > 9 ? cols[9] : null;

                String cnesClean = CsvSanitizer.cleanCnes(cnesRaw);
                if (cnesClean == null) continue;

                ParsedAddress parsedAddress = AddressParser.parse(logradouroBruto, numeroBruto);
                Endereco endereco = Endereco.builder()
                        .tipoLogradouro(parsedAddress.tipoLogradouro())
                        .nomeLogradouro(CsvSanitizer.decodeHtmlEntities(parsedAddress.nomeLogradouro()))
                        .numero(parsedAddress.numero())
                        .semNumero(parsedAddress.semNumero())
                        .bairro(bairro)
                        .municipio(municipio)
                        .uf(uf)
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
            }
            log.info("Carga do CNES finalizada com sucesso.");
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

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                String[] cols = line.split(";", -1);
                if (cols.length < 1) continue;

                String idNotificacao = CsvSanitizer.toNullIfInvalid(cols[0]);
                if (idNotificacao == null) continue;

                String uf = cols.length > 5 ? CsvSanitizer.toNullIfInvalid(cols[5]) : null;
                String sexo = cols.length > 4 ? CsvSanitizer.toNullIfInvalid(cols[4]) : null;
                String faixaEtaria = cols.length > 3 ? CsvSanitizer.toNullIfInvalid(cols[3]) : null;
                
                Integer idadePaciente = null;
                if (cols.length > 2) {
                    String idadeStr = CsvSanitizer.toNullIfInvalid(cols[2]);
                    if (idadeStr != null) {
                        try {
                            idadePaciente = Integer.parseInt(idadeStr.replaceAll("[^0-9]", ""));
                        } catch (Exception ignored) {}
                    }
                }

                Notificacao notificacao = Notificacao.builder()
                        .identificacaoNotificacao(idNotificacao)
                        .uf(uf)
                        .sexo(sexo)
                        .faixaEtaria(faixaEtaria)
                        .idadePaciente(idadePaciente)
                        .build();

                notificacaoRepository.save(notificacao);
            }
            log.info("Carga de VigiMed Notificacoes finalizada com sucesso.");
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

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {
            String line;
            boolean header = true;
            long orfaosIgnorados = 0;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                String[] cols = line.split(";", -1);
                if (cols.length < 2) continue;

                String notificacaoId = CsvSanitizer.toNullIfInvalid(cols[0]);
                if (notificacaoId == null) continue;

                Optional<Notificacao> notifOpt = notificacaoRepository.findById(notificacaoId);
                if (notifOpt.isEmpty()) {
                    orfaosIgnorados++;
                    continue; // Quarentena de órfãos
                }

                String termoRelatado = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols[1]));
                String termoMeddraPt = cols.length > 2 ? CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols[2])) : termoRelatado;
                String desfecho = cols.length > 3 ? CsvSanitizer.toNullIfInvalid(cols[3]) : null;

                ReacaoNotificada reacao = ReacaoNotificada.builder()
                        .notificacao(notifOpt.get())
                        .termoRelatado(termoRelatado)
                        .termoMeddraPt(termoMeddraPt != null ? termoMeddraPt : termoRelatado)
                        .desfecho(desfecho)
                        .build();

                reacaoNotificadaRepository.save(reacao);
            }
            log.info("Carga de VigiMed Reacoes finalizada. Órfãos ignorados (quarentena): {}", orfaosIgnorados);
        } catch (Exception e) {
            log.error("Erro ao carregar VigiMed Reacoes", e);
        }
    }

    @Transactional
    public void carregarMedicamentos(String filePath) {
        if (medicamentoNotificadoRepository.count() > 0) {
            log.info("Medicamentos já carregados no banco. Pulando carga.");
            return;
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.warn("Arquivo VigiMed Medicamentos não encontrado em: {}. Pulando carga.", filePath);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {
            String line;
            boolean header = true;
            long orfaosIgnorados = 0;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                String[] cols = line.split(";", -1);
                if (cols.length < 2) continue;

                String notificacaoId = CsvSanitizer.toNullIfInvalid(cols[0]);
                if (notificacaoId == null) continue;

                Optional<Notificacao> notifOpt = notificacaoRepository.findById(notificacaoId);
                if (notifOpt.isEmpty()) {
                    orfaosIgnorados++;
                    continue; // Quarentena de órfãos
                }

                String nomeMed = CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols[1]));
                String principioAtivo = cols.length > 2 ? CsvSanitizer.decodeHtmlEntities(CsvSanitizer.toNullIfInvalid(cols[2])) : nomeMed;
                String relacaoEvento = cols.length > 3 ? CsvSanitizer.toNullIfInvalid(cols[3]) : null;

                MedicamentoNotificado med = MedicamentoNotificado.builder()
                        .notificacao(notifOpt.get())
                        .nomeMedicamento(nomeMed)
                        .principioAtivo(principioAtivo != null ? principioAtivo : nomeMed)
                        .relacaoEvento(relacaoEvento)
                        .build();

                medicamentoNotificadoRepository.save(med);
            }
            log.info("Carga de VigiMed Medicamentos finalizada. Órfãos ignorados (quarentena): {}", orfaosIgnorados);
        } catch (Exception e) {
            log.error("Erro ao carregar VigiMed Medicamentos", e);
        }
    }
}
