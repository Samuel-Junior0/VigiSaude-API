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
public class EstabelecimentoDTO {
    private String cnes;

    @JsonProperty("nome_fantasia")
    private String nomeFantasia;

    @JsonProperty("tipo_unidade")
    private String tipoUnidade;

    @JsonProperty("endereco_desmembrado")
    private EnderecoDTO enderecoDesmembrado;
}
