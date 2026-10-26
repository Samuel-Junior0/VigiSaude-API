package br.edu.ifpb.vigisaude.repository;

import br.edu.ifpb.vigisaude.model.SinonimoTermo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SinonimoTermoRepository extends JpaRepository<SinonimoTermo, Long> {
    Optional<SinonimoTermo> findByTermoBrutoIgnoreCaseAndTipoTermo(String termoBruto, String tipoTermo);
}
