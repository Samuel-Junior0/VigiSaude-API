package br.edu.ifpb.vigisaude.service;

import br.edu.ifpb.vigisaude.model.SinonimoTermo;
import br.edu.ifpb.vigisaude.repository.SinonimoTermoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NormalizacaoService {

    private final SinonimoTermoRepository sinonimoTermoRepository;

    public String padronizarSintoma(String sintomaBruto) {
        if (sintomaBruto == null || sintomaBruto.isBlank()) {
            return null;
        }
        String trimmed = sintomaBruto.trim();
        Optional<SinonimoTermo> sinonimo = sinonimoTermoRepository.findByTermoBrutoIgnoreCaseAndTipoTermo(trimmed, "SINTOMA");
        if (sinonimo.isPresent()) {
            return sinonimo.get().getTermoPadronizado();
        }
        return trimmed.toUpperCase();
    }
}
