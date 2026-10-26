package br.edu.ifpb.vigisaude.controller;

import br.edu.ifpb.vigisaude.dto.ConsultaRequestDTO;
import br.edu.ifpb.vigisaude.dto.RespostaFarmacovigilanciaDTO;
import br.edu.ifpb.vigisaude.service.ConsultaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/farmacovigilancia")
@RequiredArgsConstructor
public class ConsultaFarmacovigilanciaController {

    private final ConsultaService consultaService;

    @PostMapping("/consultar")
    public ResponseEntity<RespostaFarmacovigilanciaDTO> consultar(@RequestBody ConsultaRequestDTO request) {
        RespostaFarmacovigilanciaDTO resposta = consultaService.processarConsulta(request);
        return ResponseEntity.ok(resposta);
    }
}
