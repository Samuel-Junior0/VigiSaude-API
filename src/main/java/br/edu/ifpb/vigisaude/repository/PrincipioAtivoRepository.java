package br.edu.ifpb.vigisaude.repository;

import br.edu.ifpb.vigisaude.model.PrincipioAtivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrincipioAtivoRepository extends JpaRepository<PrincipioAtivo, Long> {

    @Query("SELECT p.principioAtivo, COUNT(DISTINCT m.notificacao.identificacaoNotificacao) " +
           "FROM PrincipioAtivo p JOIN p.medicamentoNotificado m " +
           "WHERE UPPER(m.notificacao.uf) = UPPER(:uf) " +
           "AND m.notificacao.identificacaoNotificacao IN " +
           "(SELECT r.notificacao.identificacaoNotificacao FROM ReacaoNotificada r " +
           "WHERE UPPER(r.termoMeddraPt) = UPPER(:termoPt)) " +
           "GROUP BY p.principioAtivo " +
           "ORDER BY COUNT(DISTINCT m.notificacao.identificacaoNotificacao) DESC, p.principioAtivo ASC")
    List<Object[]> findTopPrincipioAtivoByTermoPtAndUf(@Param("termoPt") String termoPt, @Param("uf") String uf);
}
