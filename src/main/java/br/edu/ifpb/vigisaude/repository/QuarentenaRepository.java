package br.edu.ifpb.vigisaude.repository;

import br.edu.ifpb.vigisaude.model.Quarentena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuarentenaRepository extends JpaRepository<Quarentena, Long> {
}
