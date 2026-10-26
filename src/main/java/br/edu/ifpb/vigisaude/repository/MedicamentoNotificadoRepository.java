package br.edu.ifpb.vigisaude.repository;

import br.edu.ifpb.vigisaude.model.MedicamentoNotificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicamentoNotificadoRepository extends JpaRepository<MedicamentoNotificado, Long> {

    @Query("SELECT m.principioAtivo, COUNT(m) as total FROM MedicamentoNotificado m WHERE m.notificacao.identificacaoNotificacao IN " +
           "(SELECT r.notificacao.identificacaoNotificacao FROM ReacaoNotificada r WHERE UPPER(r.termoMeddraPt) = UPPER(:termoPt)) " +
           "GROUP BY m.principioAtivo ORDER BY total DESC")
    List<Object[]> findTopPrincipioAtivoByTermoPt(@Param("termoPt") String termoPt);
}
