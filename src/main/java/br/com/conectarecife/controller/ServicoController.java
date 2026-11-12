package br.com.conectarecife.controller;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Servico;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Serviços")
@RestController
@RequestMapping("/servicos")
public class ServicoController {

    private final DadosStore store;

    public ServicoController(DadosStore store) {
        this.store = store;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Consultar jornada",
            description = "Requisitos e passos derivados do catálogo em linguagem simples.")
    @GetMapping("/{id}/jornada")
    public br.com.conectarecife.service.JornadaServico.Jornada jornada(@PathVariable Integer id) {
        return br.com.conectarecife.service.JornadaServico.criar(buscarPorId(id));
    }

    public record ItemProgresso(int posicao, String descricao, boolean concluido) {}
    public record Progresso(java.util.List<ItemProgresso> checklist, int concluidos, int total,
                            int percentual, String descricao, boolean autodeclarado) {}

    @io.swagger.v3.oas.annotations.Operation(summary = "Calcular progresso",
            description = "Envie as posições concluídas, por exemplo [1,2], ou [] para desmarcar todas. Posições começam em 1. Progresso autodeclarado, sem persistência no servidor. Não confirma conclusão junto ao órgão.")
    @PostMapping("/{id}/jornada/progresso")
    public Progresso progresso(@PathVariable Integer id, @RequestBody java.util.List<Integer> concluidos) {
        Servico servico = buscarPorId(id);
        var passos = servico.passos() == null || servico.passos().isBlank()
                ? java.util.List.<String>of() : br.com.conectarecife.service.JornadaServico.criar(servico).passos();
        if (concluidos == null || concluidos.size() > passos.size()
                || concluidos.stream().anyMatch(n -> n == null || n < 1 || n > passos.size())
                || new java.util.HashSet<>(concluidos).size() != concluidos.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe posições distintas existentes no checklist");
        }
        var marcados = new java.util.HashSet<>(concluidos);
        var itens = java.util.stream.IntStream.range(0, passos.size())
                .mapToObj(i -> new ItemProgresso(i + 1, passos.get(i), marcados.contains(i + 1))).toList();
        int percentual = passos.isEmpty() ? 0 : 100 * marcados.size() / passos.size();
        String descricao = passos.isEmpty() ? "Consulte o órgão responsável sobre as etapas deste serviço"
                : marcados.size() + " de " + passos.size() + " etapas marcadas por você";
        return new Progresso(itens, marcados.size(), passos.size(), percentual, descricao, true);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Consultar serviço",
            description = "Consulta um serviço existente do catálogo, por exemplo 1.")
    @GetMapping("/{id}")
    public Servico buscarPorId(@PathVariable Integer id) {
        Servico servico = store.findServico(id);
        if (servico == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Serviço não encontrado: " + id);
        }
        return servico;
    }
}
