package br.edu.ifpb.vigisaude.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_medicamento")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_produto")
    private String nomeProduto;

    @Column(name = "principio_ativo")
    private String principioAtivo;

    @Column(name = "cnpj_detentor")
    private String cnpjDetentor;

    @Column(name = "empresa_detentora")
    private String empresaDetentora;
}
