package br.edu.ifpb.vigisaude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtendimentoCnesDTO {
    @JsonProperty("municipio_filtrado")
    private String municipioFiltrado;

    private String uf;

    @JsonProperty("estabelecimentos_encontrados")
    private List<EstabelecimentoDTO> estabelecimentosEncontrados;
}
