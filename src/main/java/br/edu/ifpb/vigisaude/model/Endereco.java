package br.edu.ifpb.vigisaude.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_endereco")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_logradouro")
    private String tipoLogradouro;

    @Column(name = "nome_logradouro")
    private String nomeLogradouro;

    private String numero;

    @Column(name = "sem_numero")
    private Boolean semNumero;

    private String bairro;

    @Column(name = "codigo_municipio")
    private String codigoMunicipio;

    private String municipio;

    private String uf;

    private String cep;
}
