package br.edu.ifpb.vigisaude.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_medicamento_notificado")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicamentoNotificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "notificacao_id")
    private Notificacao notificacao;

    @Column(name = "nome_medicamento")
    private String nomeMedicamento;

    @Column(name = "principio_ativo")
    private String principioAtivo;

    @Column(name = "relacao_evento")
    private String relacaoEvento;
}
