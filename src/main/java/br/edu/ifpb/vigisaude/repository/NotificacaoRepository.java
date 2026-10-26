package br.edu.ifpb.vigisaude.repository;

import br.edu.ifpb.vigisaude.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, String> {
}
