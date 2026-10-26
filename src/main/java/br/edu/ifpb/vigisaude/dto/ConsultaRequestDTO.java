package br.edu.ifpb.vigisaude.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultaRequestDTO {
    private String sintoma;
    private String municipio;
    private String uf;
}
