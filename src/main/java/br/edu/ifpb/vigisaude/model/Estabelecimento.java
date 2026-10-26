package br.edu.ifpb.vigisaude.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_estabelecimento")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estabelecimento {

    @Id
    @Column(length = 7)
    private String cnes;

    @Column(name = "co_unidade")
    private String coUnidade;

    @Column(name = "razao_social", length = 255)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 255)
    private String nomeFantasia;

    @Column(name = "tipo_unidade")
    private String tipoUnidade;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "endereco_id")
    private Endereco endereco;
}
