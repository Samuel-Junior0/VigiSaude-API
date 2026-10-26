package br.edu.ifpb.vigisaude.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "tb_notificacao")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacao {

    @Id
    @Column(name = "identificacao_notificacao")
    private String identificacaoNotificacao;

    @Column(name = "data_notificacao")
    private LocalDate dataNotificacao;

    @Column(name = "idade_paciente")
    private Integer idadePaciente;

    @Column(name = "faixa_etaria")
    private String faixaEtaria;

    private String sexo;

    private String uf;

    @OneToMany(mappedBy = "notificacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReacaoNotificada> reacoes;

    @OneToMany(mappedBy = "notificacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicamentoNotificado> medicamentos;
}
