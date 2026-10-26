package br.edu.ifpb.vigisaude.repository;

import br.edu.ifpb.vigisaude.model.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, String> {
    List<Estabelecimento> findByEnderecoMunicipioIgnoreCaseAndEnderecoUfIgnoreCase(String municipio, String uf);
}
