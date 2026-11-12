package br.com.conectarecife.controller;

import br.com.conectarecife.service.PreferenciasService;
import br.com.conectarecife.service.PreferenciasService.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class PreferenciasController {
    private final PreferenciasService preferencias;

    public PreferenciasController(PreferenciasService preferencias) {
        this.preferencias = preferencias;
    }

    @GetMapping("/preferencias")
    public Preferencias consultar(@RequestParam String cidadaoId) {
        return preferencias.consultar(cidadaoId);
    }

    @PutMapping("/preferencias")
    public Preferencias alterar(@RequestBody Alteracao alteracao) {
        return preferencias.alterar(alteracao);
    }
}
