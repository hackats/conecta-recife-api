package br.com.conectarecife.controller;

import br.com.conectarecife.service.BuscaService;
import br.com.conectarecife.model.Servico;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class BuscaController {
    private final BuscaService busca;
    public BuscaController(BuscaService busca) { this.busca = busca; }
    @GetMapping("/servicos/busca")
    public List<Servico> buscar(@RequestParam(defaultValue = "") String q) { return busca.buscar(q); }
}
