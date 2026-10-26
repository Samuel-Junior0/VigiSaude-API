package br.edu.ifpb.vigisaude.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_reacao_notificada")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReacaoNotificada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "notificacao_id")
    private Notificacao notificacao;

    @Column(name = "termo_relatado", columnDefinition = "TEXT")
    private String termoRelatado;

    @Column(name = "termo_meddra_pt", columnDefinition = "TEXT")
    private String termoMeddraPt;

    private String desfecho;
}
