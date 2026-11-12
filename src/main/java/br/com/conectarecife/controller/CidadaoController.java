package br.com.conectarecife.controller;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Cidadao;
import br.com.conectarecife.model.Clique;
import br.com.conectarecife.service.Recomendador;
import br.com.conectarecife.service.Recomendador.Recomendacao;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Cidadãos")
@RestController
@RequestMapping("/cidadaos")
public class CidadaoController {

    private final DadosStore store;

    private final Recomendador recomendador;

    public CidadaoController(DadosStore store, Recomendador recomendador) {
        this.store = store;
        this.recomendador = recomendador;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Consultar cidadão",
            description = "Consulta pelo identificador simulado, por exemplo C007938.")
    @GetMapping("/{id}")
    public Cidadao buscarPorId(@PathVariable String id) {
        Cidadao cidadao = store.findCidadao(id);
        if (cidadao == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Cidadão não encontrado: " + id);
        }
        return cidadao;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Recomendar serviços",
            description = "Até dez serviços distintos ainda não acessados, com motivo. Considera perfil, histórico, transições e consentimento.")
    @GetMapping("/{id}/recomendacoes")
    public List<Recomendacao> listarRecomendacoes(@PathVariable String id) {
        return recomendador.recomendar(buscarPorId(id));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Consultar histórico",
            description = "Cliques do cidadão simulado.")
    @GetMapping("/{id}/cliques")
    public List<Clique> listarCliques(@PathVariable String id) {
        if (store.findCidadao(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Cidadão não encontrado: " + id);
        }
        return store.findCliquesDoCidadao(id);
    }
}
