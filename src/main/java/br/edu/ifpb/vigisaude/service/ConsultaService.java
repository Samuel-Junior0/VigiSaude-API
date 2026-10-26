package br.edu.ifpb.vigisaude.service;

import br.edu.ifpb.vigisaude.dto.*;
import br.edu.ifpb.vigisaude.model.Estabelecimento;
import br.edu.ifpb.vigisaude.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultaService {

    private final NormalizacaoService normalizacaoService;
    private final ReacaoNotificadaRepository reacaoNotificadaRepository;
    private final PrincipioAtivoRepository principioAtivoRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public RespostaFarmacovigilanciaDTO processarConsulta(ConsultaRequestDTO request) {
        String sintomaBruto = request.getSintoma();
        String municipio = request.getMunicipio();
        String uf = request.getUf();

        String termoPadronizado = normalizacaoService.padronizarSintoma(sintomaBruto);
        boolean sinonimoEncontrado = termoPadronizado != null
                && !termoPadronizado.equalsIgnoreCase(sintomaBruto != null ? sintomaBruto.trim() : "");

        ConsultaInfoDTO consultaInfo = ConsultaInfoDTO.builder()
                .termoDigitado(sintomaBruto)
                .termoPadronizado(termoPadronizado)
                .statusNormalizacao(sinonimoEncontrado ? 
                        "Sinônimo identificado e convertido via tabela de equivalências MedDRA" : 
                        "Termo processado diretamente (sem equivalência pré-cadastrada)")
                .build();

        long totalNotificacoes = reacaoNotificadaRepository.countNotificacoesByTermoPtAndUf(termoPadronizado, uf);
        
        List<Object[]> topMedicamentos = principioAtivoRepository.findTopPrincipioAtivoByTermoPtAndUf(termoPadronizado, uf);
        MedicamentoMaisNotificadoDTO medMaisNotificado = null;

        if (!topMedicamentos.isEmpty() && topMedicamentos.get(0) != null) {
            Object[] top = topMedicamentos.get(0);
            String principioAtivo = (String) top[0];
            long totalPrincipio = top.length > 1 && top[1] != null ? ((Number) top[1]).longValue() : 0L;
            String percentual = totalNotificacoes > 0
                    ? String.format(Locale.ROOT, "%.1f%%", (totalPrincipio * 100.0) / totalNotificacoes)
                    : "0%";
            medMaisNotificado = MedicamentoMaisNotificadoDTO.builder()
                    .principioAtivo(principioAtivo)
                    .percentualDosCasos(percentual)
                    .build();
        }

        EstatisticasVigiMedDTO estatisticas = EstatisticasVigiMedDTO.builder()
                .totalNotificacoesEncontradas(totalNotificacoes)
                .medicamentoMaisNotificadoAssociado(medMaisNotificado)
                .registrosIncompletosIgnoradosNaAnalise(0)
                .build();

        List<Estabelecimento> estabelecimentos = estabelecimentoRepository.findByEnderecoMunicipioIgnoreCaseAndEnderecoUfIgnoreCase(municipio, uf);
        List<EstabelecimentoDTO> estabDTOs = estabelecimentos.stream().map(e -> {
            EnderecoDTO endDTO = null;
            if (e.getEndereco() != null) {
                endDTO = EnderecoDTO.builder()
                        .tipoLogradouro(e.getEndereco().getTipoLogradouro())
                        .logradouro(e.getEndereco().getNomeLogradouro())
                        .numero(e.getEndereco().getNumero())
                        .semNumero(e.getEndereco().getSemNumero())
                        .bairro(e.getEndereco().getBairro())
                        .cep(e.getEndereco().getCep())
                        .build();
            }
            return EstabelecimentoDTO.builder()
                    .cnes(e.getCnes())
                    .nomeFantasia(e.getNomeFantasia())
                    .tipoUnidade(e.getTipoUnidade())
                    .enderecoDesmembrado(endDTO)
                    .build();
        }).collect(Collectors.toList());

        AtendimentoCnesDTO atendimentoCnes = AtendimentoCnesDTO.builder()
                .municipioFiltrado(municipio)
                .uf(uf)
                .estabelecimentosEncontrados(estabDTOs)
                .build();

        return RespostaFarmacovigilanciaDTO.builder()
                .consulta(consultaInfo)
                .estatisticasHistoricasVigiMed(estatisticas)
                .atendimentoProximoCnes(atendimentoCnes)
                .build();
    }
}
