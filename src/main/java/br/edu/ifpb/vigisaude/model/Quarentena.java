package br.edu.ifpb.vigisaude.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_quarentena", indexes = {
        @Index(name = "idx_quarentena_origem", columnList = "origem"),
        @Index(name = "idx_quarentena_motivo", columnList = "motivo"),
        @Index(name = "idx_quarentena_notificacao", columnList = "identificacao_notificacao")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quarentena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origem", length = 50, nullable = false)
    private String origem;

    @Column(name = "arquivo", length = 500)
    private String arquivo;

    @Column(name = "identificacao_notificacao", length = 100)
    private String identificacaoNotificacao;

    @Column(name = "motivo", length = 50, nullable = false)
    private String motivo;

    @Column(name = "linha_bruta", columnDefinition = "TEXT")
    private String linhaBruta;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;

    @PrePersist
    void preencherDataRegistro() {
        if (dataRegistro == null) {
            dataRegistro = LocalDateTime.now();
        }
    }
}
