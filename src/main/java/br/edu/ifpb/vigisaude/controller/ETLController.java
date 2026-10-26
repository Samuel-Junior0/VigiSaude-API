package br.edu.ifpb.vigisaude.controller;

import br.edu.ifpb.vigisaude.service.ETLService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/etl")
@RequiredArgsConstructor
public class ETLController {

    private final ETLService etlService;

    @PostMapping("/executar")
    public ResponseEntity<String> executarEtl(
            @RequestParam("cnesPath") String cnesPath,
            @RequestParam("notificacoesPath") String notificacoesPath,
            @RequestParam("reacoesPath") String reacoesPath,
            @RequestParam("medicamentosPath") String medicamentosPath) {
        
        etlService.carregarSinonimos();
        etlService.carregarCNES(cnesPath);
        etlService.carregarNotificacoes(notificacoesPath);
        etlService.carregarReacoes(reacoesPath);
        etlService.carregarMedicamentos(medicamentosPath);

        return ResponseEntity.ok("Processo ETL executado com sucesso para todas as bases de dados!");
    }
}
