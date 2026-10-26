package br.edu.ifpb.vigisaude.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_sinonimo_termo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SinonimoTermo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "termo_bruto")
    private String termoBruto;

    @Column(name = "termo_padronizado")
    private String termoPadronizado;

    @Column(name = "tipo_termo")
    private String tipoTermo;
}
