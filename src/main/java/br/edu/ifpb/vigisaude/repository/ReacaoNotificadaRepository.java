package br.edu.ifpb.vigisaude.repository;

import br.edu.ifpb.vigisaude.model.ReacaoNotificada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReacaoNotificadaRepository extends JpaRepository<ReacaoNotificada, Long> {

    @Query("SELECT COUNT(DISTINCT r.notificacao) FROM ReacaoNotificada r WHERE UPPER(r.termoMeddraPt) = UPPER(:termoPt) AND UPPER(r.notificacao.uf) = UPPER(:uf)")
    long countNotificacoesByTermoPtAndUf(@Param("termoPt") String termoPt, @Param("uf") String uf);
}
