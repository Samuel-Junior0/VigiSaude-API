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
public class MedicamentoMaisNotificadoDTO {
    @JsonProperty("principio_ativo")
    private String principioAtivo;

    @JsonProperty("percentual_dos_casos")
    private String percentualDosCasos;
}
