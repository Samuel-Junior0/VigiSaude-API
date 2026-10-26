package br.edu.ifpb.vigisaude.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_principio_ativo", indexes = {
        @Index(name = "idx_principio_ativo_nome", columnList = "principio_ativo"),
        @Index(name = "idx_principio_ativo_medicamento", columnList = "medicamento_notificado_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrincipioAtivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medicamento_notificado_id", nullable = false)
    private MedicamentoNotificado medicamentoNotificado;

    @Column(name = "principio_ativo", length = 255, nullable = false)
    private String principioAtivo;

    @Column(name = "ordem")
    private Integer ordem;
}
