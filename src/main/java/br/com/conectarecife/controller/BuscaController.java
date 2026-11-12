package br.com.conectarecife.controller;

import br.com.conectarecife.service.BuscaService;
import br.com.conectarecife.model.Servico;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Busca")
@RestController
public class BuscaController {
    private final BuscaService busca;
    public BuscaController(BuscaService busca) { this.busca = busca; }
    @io.swagger.v3.oas.annotations.Operation(summary = "Buscar serviços",
            description = "Busca com acentos normalizados, sinônimos e aproximação. Exemplo de q: quero marcar medico. Máximo de 300 caracteres e dez resultados existentes. Consulta vazia ou sem correspondência retorna [].")
    @GetMapping("/servicos/busca")
    public List<Servico> buscar(@RequestParam(defaultValue = "") String q) { return busca.buscar(q); }
}
