package br.com.conectarecife.controller;

import br.com.conectarecife.service.PreferenciasService;
import br.com.conectarecife.service.PreferenciasService.*;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Preferências")
@RestController
public class PreferenciasController {
    private final PreferenciasService preferencias;

    public PreferenciasController(PreferenciasService preferencias) {
        this.preferencias = preferencias;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Consultar preferências",
            description = "Consentimento de saúde começa desligado. Use cidadaoId, por exemplo C007938.")
    @GetMapping("/preferencias")
    public Preferencias consultar(@RequestParam String cidadaoId) {
        return preferencias.consultar(cidadaoId);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Alterar consentimento",
            description = "cidadaoId e consentimentoSaude são obrigatórios. aceitaNotificacao é opcional e preserva o valor atual se omitido. A revogação vale na próxima recomendação. Dados em memória.")
    @PutMapping("/preferencias")
    public Preferencias alterar(@RequestBody Alteracao alteracao) {
        return preferencias.alterar(alteracao);
    }
}
