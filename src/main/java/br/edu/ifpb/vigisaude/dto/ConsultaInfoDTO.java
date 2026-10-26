package br.edu.ifpb.vigisaude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultaInfoDTO {
    @JsonProperty("termo_digitado")
    private String termoDigitado;

    @JsonProperty("termo_padronizado")
    private String termoPadronizado;

    @JsonProperty("status_normalizacao")
    private String statusNormalizacao;
}
