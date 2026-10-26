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
            @RequestParam("municipiosPath") String municipiosPath,
            @RequestParam("notificacoesPath") String notificacoesPath,
            @RequestParam("reacoesPath") String reacoesPath,
            @RequestParam("medicamentosPath") String medicamentosPath,
            @RequestParam(value = "anvisaPath", required = false) String anvisaPath) {

        etlService.carregarSinonimos();
        etlService.carregarCNES(cnesPath, municipiosPath);
        etlService.carregarNotificacoes(notificacoesPath);
        etlService.carregarReacoes(reacoesPath);
        etlService.carregarSinonimosDeReacoes(reacoesPath);
        etlService.carregarMedicamentos(medicamentosPath);
        etlService.carregarMedicamentosAnvisa(anvisaPath);

        return ResponseEntity.ok("Processo ETL executado com sucesso para todas as bases de dados!");
    }
}
