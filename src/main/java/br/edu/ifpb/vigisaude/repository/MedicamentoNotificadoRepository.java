package br.edu.ifpb.vigisaude.repository;

import br.edu.ifpb.vigisaude.model.MedicamentoNotificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicamentoNotificadoRepository extends JpaRepository<MedicamentoNotificado, Long> {
}
