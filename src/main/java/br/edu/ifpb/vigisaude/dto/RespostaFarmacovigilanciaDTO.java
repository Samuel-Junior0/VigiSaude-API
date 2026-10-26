package br.edu.ifpb.vigisaude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaFarmacovigilanciaDTO {
    private ConsultaInfoDTO consulta;

    @JsonProperty("estatisticas_historicas_vigimed")
    private EstatisticasVigiMedDTO estatisticasHistoricasVigiMed;

    @JsonProperty("atendimento_proximo_cnes")
    private AtendimentoCnesDTO atendimentoProximoCnes;
}
