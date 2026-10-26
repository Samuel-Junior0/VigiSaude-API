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
public class EstatisticasVigiMedDTO {
    @JsonProperty("total_notificacoes_encontradas")
    private long totalNotificacoesEncontradas;

    @JsonProperty("medicamento_mais_notificado_associado")
    private MedicamentoMaisNotificadoDTO medicamentoMaisNotificadoAssociado;

    @JsonProperty("registros_incompletos_ignorados_na_analise")
    private long registrosIncompletosIgnoradosNaAnalise;
}
