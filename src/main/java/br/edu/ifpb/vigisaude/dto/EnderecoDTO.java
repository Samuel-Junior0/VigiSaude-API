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
public class EnderecoDTO {
    @JsonProperty("tipo_logradouro")
    private String tipoLogradouro;

    @JsonProperty("logradouro")
    private String logradouro;

    private String numero;

    @JsonProperty("sem_numero")
    private Boolean semNumero;

    private String bairro;
    private String cep;
}
