package br.edu.ifpb.vigisaude.repository;

import br.edu.ifpb.vigisaude.model.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Long> {
}
